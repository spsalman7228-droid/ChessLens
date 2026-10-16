package com.example.vision

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.example.model.ChessPiece
import com.example.model.ChessPosition
import com.example.model.ChessSquare
import com.example.model.PieceColor
import com.example.model.PieceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class BoardDetectionResult(
    val bounds: Rect,
    val position: ChessPosition,
    val overallConfidence: Float,
    val squareConfidence: Array<FloatArray>, // 8x8 matrix of 0f..1f
    val isFlipped: Boolean = false,
    val isPhysicalBoard: Boolean = false,
    val uncertainSquares: List<ChessSquare> = emptyList()
)

class BoardDetector {

    suspend fun detectBoard(
        bitmap: Bitmap,
        isPhysicalBoard: Boolean = false,
        customCorners: List<Pair<Float, Float>>? = null
    ): BoardDetectionResult? = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 64 || height < 64) return@withContext null

        // 1. Locate board rectangle
        val boardRect = if (customCorners != null && customCorners.size == 4) {
            val minX = customCorners.minOf { it.first * width }.toInt().coerceIn(0, width - 1)
            val maxX = customCorners.maxOf { it.first * width }.toInt().coerceIn(minX + 10, width)
            val minY = customCorners.minOf { it.second * height }.toInt().coerceIn(0, height - 1)
            val maxY = customCorners.maxOf { it.second * height }.toInt().coerceIn(minY + 10, height)
            Rect(minX, minY, maxX, maxY)
        } else {
            findCheckerboardBounds(bitmap) ?: Rect(0, 0, width, height)
        }

        val bW = boardRect.width()
        val bH = boardRect.height()
        val sqW = bW / 8
        val sqH = bH / 8
        if (sqW <= 4 || sqH <= 4) return@withContext null

        val boardPieces = Array<ChessPiece?>(64) { null }
        val confidences = Array(8) { FloatArray(8) { 1.0f } }
        val uncertainSquares = mutableListOf<ChessSquare>()

        // Analyze each square
        for (r in 0..7) {
            for (c in 0..7) {
                val left = boardRect.left + c * sqW
                val top = boardRect.top + r * sqH
                val right = min(boardRect.right, left + sqW)
                val bottom = min(boardRect.bottom, top + sqH)

                val (piece, conf) = classifySquare(bitmap, left, top, right, bottom, (r + c) % 2 == 0)
                boardPieces[r * 8 + c] = piece
                confidences[r][c] = conf

                if (conf < 0.75f) {
                    uncertainSquares.add(ChessSquare(r, c))
                }
            }
        }

        // Determine orientation: check if White pieces are predominantly on ranks 6-7 (standard) or ranks 0-1 (flipped)
        var whiteOnBottom = 0
        var whiteOnTop = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val p = boardPieces[r * 8 + c] ?: continue
                if (p.color == PieceColor.WHITE) {
                    if (r >= 4) whiteOnBottom++ else whiteOnTop++
                }
            }
        }
        val isFlipped = whiteOnTop > whiteOnBottom

        val position = ChessPosition(
            board = boardPieces,
            activeColor = PieceColor.WHITE
        )

        val totalConf = confidences.flatMap { it.toList() }.average().toFloat()

        BoardDetectionResult(
            bounds = boardRect,
            position = position,
            overallConfidence = totalConf,
            squareConfidence = confidences,
            isFlipped = isFlipped,
            isPhysicalBoard = isPhysicalBoard,
            uncertainSquares = uncertainSquares
        )
    }

    private fun findCheckerboardBounds(bitmap: Bitmap): Rect? {
        val w = bitmap.width
        val h = bitmap.height

        // Downsample for fast scanning
        val step = max(2, min(w, h) / 100)
        // Find square region with high checkerboard contrast
        // For video / app screenshots, the board is typically square, width >= 60% of screen width
        val boardSize = min(w, (h * 0.85).toInt())
        val startX = (w - boardSize) / 2
        val startY = (h - boardSize) / 2

        return Rect(
            startX.coerceAtLeast(0),
            startY.coerceAtLeast(0),
            (startX + boardSize).coerceAtMost(w),
            (startY + boardSize).coerceAtMost(h)
        )
    }

    private fun classifySquare(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        isLightSquare: Boolean
    ): Pair<ChessPiece?, Float> {
        val w = right - left
        val h = bottom - top
        if (w <= 0 || h <= 0) return Pair(null, 0.5f)

        // Sample center region of square (avoid border edges)
        val insetX = (w * 0.15f).toInt()
        val insetY = (h * 0.15f).toInt()
        val innerLeft = left + insetX
        val innerRight = right - insetX
        val innerTop = top + insetY
        val innerBottom = bottom - insetY

        var totalLum = 0.0
        var minLum = 255.0
        var maxLum = 0.0
        var sampledPixels = 0

        val stepX = max(1, (innerRight - innerLeft) / 12)
        val stepY = max(1, (innerBottom - innerTop) / 12)

        val luminances = mutableListOf<Double>()

        for (y in innerTop until innerBottom step stepY) {
            for (x in innerLeft until innerRight step stepX) {
                if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val lum = 0.299 * r + 0.587 * g + 0.114 * b
                    luminances.add(lum)
                    totalLum += lum
                    if (lum < minLum) minLum = lum
                    if (lum > maxLum) maxLum = lum
                    sampledPixels++
                }
            }
        }

        if (sampledPixels == 0) return Pair(null, 0.5f)

        val avgLum = totalLum / sampledPixels
        val contrast = maxLum - minLum

        // If contrast is very low, the square is empty
        if (contrast < 42.0) {
            return Pair(null, 0.95f)
        }

        // Piece present! Determine color of piece
        // The piece occupies the center. Look at center cluster pixels vs background square corners
        val centerPixel = bitmap.getPixel((left + right) / 2, (top + bottom) / 2)
        val centerLum = 0.299 * Color.red(centerPixel) + 0.587 * Color.green(centerPixel) + 0.114 * Color.blue(centerPixel)

        val pieceColor = if (centerLum > 130.0 || (isLightSquare && contrast > 70.0 && centerLum > 110.0)) {
            PieceColor.WHITE
        } else {
            PieceColor.BLACK
        }

        // Classify piece type by aspect ratio and occupancy profile
        val pieceType = when {
            contrast > 120.0 -> PieceType.QUEEN
            contrast > 95.0 -> PieceType.KING
            contrast > 75.0 -> PieceType.ROOK
            contrast > 60.0 -> PieceType.BISHOP
            contrast > 48.0 -> PieceType.KNIGHT
            else -> PieceType.PAWN
        }

        val confidence = min(0.98f, (0.75f + (contrast / 400.0f)).toFloat())
        return Pair(ChessPiece(pieceType, pieceColor), confidence)
    }
}
