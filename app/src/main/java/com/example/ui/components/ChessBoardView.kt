package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArrowStyle
import com.example.model.ChessMove
import com.example.model.ChessPiece
import com.example.model.ChessPosition
import com.example.model.ChessSquare
import com.example.model.MoveAnalysis
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.ui.theme.BoardSquareDark
import com.example.ui.theme.BoardSquareHighlighted
import com.example.ui.theme.BoardSquareLight
import com.example.ui.theme.BoardSquareTarget
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun ChessBoardView(
    position: ChessPosition,
    modifier: Modifier = Modifier,
    isFlipped: Boolean = false,
    selectedSquare: ChessSquare? = null,
    legalMovesFromSelected: List<ChessMove> = emptyList(),
    lastMove: ChessMove? = null,
    analyses: List<MoveAnalysis> = emptyList(),
    avoidMoves: List<MoveAnalysis> = emptyList(),
    arrowStyle: ArrowStyle = ArrowStyle.SOLID,
    onSquareClicked: ((ChessSquare) -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(BoardSquareDark)
    ) {
        val boardSize = maxWidth
        val squareSizePx = (constraints.maxWidth / 8f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFlipped, position) {
                    detectTapGestures { offset ->
                        if (onSquareClicked != null && squareSizePx > 0) {
                            val col = (offset.x / squareSizePx).toInt().coerceIn(0, 7)
                            val row = (offset.y / squareSizePx).toInt().coerceIn(0, 7)
                            val actualRow = if (isFlipped) 7 - row else row
                            val actualCol = if (isFlipped) 7 - col else col
                            onSquareClicked(ChessSquare(actualRow, actualCol))
                        }
                    }
                }
        ) {
            val sqW = size.width / 8f
            val sqH = size.height / 8f

            // 1. Draw squares
            for (r in 0..7) {
                for (c in 0..7) {
                    val dispR = if (isFlipped) 7 - r else r
                    val dispC = if (isFlipped) 7 - c else c
                    val isLight = (r + c) % 2 == 0
                    val sqColor = if (isLight) BoardSquareLight else BoardSquareDark

                    drawRect(
                        color = sqColor,
                        topLeft = Offset(dispC * sqW, dispR * sqH),
                        size = androidx.compose.ui.geometry.Size(sqW, sqH)
                    )
                }
            }

            // 2. Highlight last move
            if (lastMove != null) {
                for (sq in listOf(lastMove.from, lastMove.to)) {
                    val dr = if (isFlipped) 7 - sq.row else sq.row
                    val dc = if (isFlipped) 7 - sq.col else sq.col
                    drawRect(
                        color = BoardSquareHighlighted,
                        topLeft = Offset(dc * sqW, dr * sqH),
                        size = androidx.compose.ui.geometry.Size(sqW, sqH)
                    )
                }
            }

            // 3. Highlight selected square
            if (selectedSquare != null) {
                val dr = if (isFlipped) 7 - selectedSquare.row else selectedSquare.row
                val dc = if (isFlipped) 7 - selectedSquare.col else selectedSquare.col
                drawRect(
                    color = Color(0x6638BDF8),
                    topLeft = Offset(dc * sqW, dr * sqH),
                    size = androidx.compose.ui.geometry.Size(sqW, sqH)
                )
            }

            // 4. Draw legal move dots
            for (move in legalMovesFromSelected) {
                val dr = if (isFlipped) 7 - move.to.row else move.to.row
                val dc = if (isFlipped) 7 - move.to.col else move.to.col
                val center = Offset((dc + 0.5f) * sqW, (dr + 0.5f) * sqH)
                val isCapture = position.getPiece(move.to) != null
                if (isCapture) {
                    drawCircle(
                        color = BoardSquareTarget,
                        radius = sqW * 0.42f,
                        center = center,
                        style = Stroke(width = sqW * 0.1f)
                    )
                } else {
                    drawCircle(
                        color = BoardSquareTarget,
                        radius = sqW * 0.18f,
                        center = center
                    )
                }
            }

            // 5. Draw piece glyphs
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = sqH * 0.78f
            }

            for (r in 0..7) {
                for (c in 0..7) {
                    val piece = position.getPiece(r, c) ?: continue
                    val dispR = if (isFlipped) 7 - r else r
                    val dispC = if (isFlipped) 7 - c else c
                    val centerX = (dispC + 0.5f) * sqW
                    val centerY = (dispR + 0.78f) * sqH

                    textPaint.color = if (piece.color == PieceColor.WHITE) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                    textPaint.setShadowLayer(
                        if (piece.color == PieceColor.WHITE) 6f else 2f,
                        0f,
                        2f,
                        if (piece.color == PieceColor.WHITE) android.graphics.Color.argb(180, 0, 0, 0) else android.graphics.Color.argb(120, 255, 255, 255)
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        piece.symbol,
                        centerX,
                        centerY,
                        textPaint
                    )
                }
            }

            // 6. Draw Multi-level Arrows
            // Group by move to show agreement count (e.g. "x3")
            val moveGroups = analyses.groupBy { it.move.uci }
            moveGroups.forEach { (_, group) ->
                val primaryAnalysis = group.first()
                val move = primaryAnalysis.move
                val color = parseHexColor(primaryAnalysis.levelColorHex)
                drawChessArrow(
                    move = move,
                    color = color,
                    sqW = sqW,
                    sqH = sqH,
                    isFlipped = isFlipped,
                    style = arrowStyle,
                    agreementCount = group.size
                )
            }

            // 7. Draw Red Avoid Arrows
            for (avoid in avoidMoves) {
                drawChessArrow(
                    move = avoid.move,
                    color = Color(0xFFF87171),
                    sqW = sqW,
                    sqH = sqH,
                    isFlipped = isFlipped,
                    style = arrowStyle,
                    isAvoid = true
                )
            }
        }
    }
}

private fun DrawScope.drawChessArrow(
    move: ChessMove,
    color: Color,
    sqW: Float,
    sqH: Float,
    isFlipped: Boolean,
    style: ArrowStyle,
    isAvoid: Boolean = false,
    agreementCount: Int = 1
) {
    val fromR = if (isFlipped) 7 - move.from.row else move.from.row
    val fromC = if (isFlipped) 7 - move.from.col else move.from.col
    val toR = if (isFlipped) 7 - move.to.row else move.to.row
    val toC = if (isFlipped) 7 - move.to.col else move.to.col

    val start = Offset((fromC + 0.5f) * sqW, (fromR + 0.5f) * sqH)
    val end = Offset((toC + 0.5f) * sqW, (toR + 0.5f) * sqH)

    val strokeWidth = if (agreementCount > 1) sqW * 0.22f else sqW * 0.16f
    val pathEffect = when (style) {
        ArrowStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(sqW * 0.15f, sqW * 0.08f), 0f)
        else -> null
    }

    val dR = toR - fromR
    val dC = toC - fromC
    val isKnightMove = (abs(dR) == 1 && abs(dC) == 2) || (abs(dR) == 2 && abs(dC) == 1)

    if (isKnightMove) {
        // Draw L-shaped path
        val corner = if (abs(dR) == 2) {
            Offset(start.x, end.y)
        } else {
            Offset(end.x, start.y)
        }
        val path = Path().apply {
            moveTo(start.x, start.y)
            lineTo(corner.x, corner.y)
            lineTo(end.x, end.y)
        }
        drawPath(
            path = path,
            color = color.copy(alpha = if (isAvoid) 0.85f else 0.9f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, pathEffect = pathEffect)
        )
    } else {
        // Straight / diagonal arrow
        drawLine(
            color = color.copy(alpha = if (isAvoid) 0.85f else 0.9f),
            start = start,
            end = end,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
            pathEffect = pathEffect
        )
    }

    // Arrowhead at destination
    val angle = if (isKnightMove) {
        val corner = if (abs(dR) == 2) Offset(start.x, end.y) else Offset(end.x, start.y)
        atan2(end.y - corner.y, end.x - corner.x)
    } else {
        atan2(end.y - start.y, end.x - start.x)
    }

    val headSize = sqW * 0.36f
    val arrowAngle = Math.PI / 5.5

    val p1 = Offset(
        (end.x - headSize * cos(angle - arrowAngle)).toFloat(),
        (end.y - headSize * sin(angle - arrowAngle)).toFloat()
    )
    val p2 = Offset(
        (end.x - headSize * cos(angle + arrowAngle)).toFloat(),
        (end.y - headSize * sin(angle + arrowAngle)).toFloat()
    )

    val headPath = Path().apply {
        moveTo(end.x, end.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }

    drawPath(
        path = headPath,
        color = color
    )

    // Agreement badge ("x2" or "x3")
    if (agreementCount > 1) {
        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.WHITE
            textSize = sqW * 0.28f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val badgeX = (start.x + end.x) / 2
        val badgeY = (start.y + end.y) / 2
        drawCircle(
            color = Color(0xCC0F172A),
            radius = sqW * 0.22f,
            center = Offset(badgeX, badgeY)
        )
        drawContext.canvas.nativeCanvas.drawText("x$agreementCount", badgeX, badgeY + sqW * 0.09f, textPaint)
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF38BDF8)
    }
}
