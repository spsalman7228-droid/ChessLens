package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.ChessPiece
import com.example.model.ChessPosition
import com.example.model.ChessSquare
import com.example.model.PieceColor
import com.example.model.PieceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class AccessibilityBoardResult(
    val position: ChessPosition,
    val bounds: Rect,
    val isFlipped: Boolean
)

class ChessLensAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceRunning.value = true
        instance = this
        speak("ChessLens accessibility service is active and ready to recognize on-screen chessboards.")
        vibrateFeedback(FeedbackType.CLICK)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track current foreground app package for context
        event?.packageName?.toString()?.let { pkg ->
            _currentForegroundPackage.value = pkg
        }
    }

    override fun onInterrupt() {
        _isServiceRunning.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        if (instance == this) {
            instance = null
        }
    }

    /**
     * Inspects active window hierarchy for accessibility semantic labels of chess pieces.
     * Web platforms (chess.com, lichess) and chess apps frequently set descriptions like "White Queen on d1".
     * If found, provides instant zero-latency 100% accurate board state & screen coordinates without screen capture!
     */
    fun tryReadPositionFromAccessibilityNodesWithBounds(): AccessibilityBoardResult? {
        val root = rootInActiveWindow ?: return null
        val foundPieces = mutableMapOf<ChessSquare, ChessPiece>()
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE

        val nodeRect = Rect()

        fun scanNode(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val desc = node.contentDescription?.toString()?.lowercase(Locale.ROOT) ?: ""
            val text = node.text?.toString()?.lowercase(Locale.ROOT) ?: ""
            val combined = "$desc $text"

            if (combined.isNotBlank()) {
                parseSquareAndPiece(combined)?.let { (sq, piece) ->
                    foundPieces[sq] = piece
                    node.getBoundsInScreen(nodeRect)
                    if (nodeRect.width() > 0 && nodeRect.height() > 0) {
                        minX = minOf(minX, nodeRect.left)
                        minY = minOf(minY, nodeRect.top)
                        maxX = maxOf(maxX, nodeRect.right)
                        maxY = maxOf(maxY, nodeRect.bottom)
                    }
                }
            }

            for (i in 0 until node.childCount) {
                scanNode(node.getChild(i))
            }
        }

        scanNode(root)

        // If we found at least 8 chess pieces via accessibility nodes, construct position!
        if (foundPieces.size >= 8) {
            val board = Array<ChessPiece?>(64) { null }
            foundPieces.forEach { (sq, piece) ->
                board[sq.index] = piece
            }

            val whiteOnBottom = foundPieces.count { it.value.color == PieceColor.WHITE && it.key.row >= 4 }
            val whiteOnTop = foundPieces.count { it.value.color == PieceColor.WHITE && it.key.row < 4 }
            val isFlipped = whiteOnTop > whiteOnBottom

            val pos = ChessPosition(board = board)
            if (pos.validate().isValid) {
                val bounds = if (minX < maxX && minY < maxY) {
                    Rect(minX, minY, maxX, maxY)
                } else {
                    Rect()
                }
                return AccessibilityBoardResult(pos, bounds, isFlipped)
            }
        }
        return null
    }

    private fun parseSquareAndPiece(text: String): Pair<ChessSquare, ChessPiece>? {
        // Example: "white knight e4", "black pawn d5", "square f3 with white bishop"
        val isWhite = text.contains("white")
        val isBlack = text.contains("black")
        if (!isWhite && !isBlack) return null

        val color = if (isWhite) PieceColor.WHITE else PieceColor.BLACK

        val type = when {
            text.contains("queen") -> PieceType.QUEEN
            text.contains("king") -> PieceType.KING
            text.contains("rook") -> PieceType.ROOK
            text.contains("bishop") -> PieceType.BISHOP
            text.contains("knight") -> PieceType.KNIGHT
            text.contains("pawn") -> PieceType.PAWN
            else -> return null
        }

        // Find algebraic square: e.g. e4, d5, c3
        val squareRegex = "\\b([a-h][1-8])\\b".toRegex()
        val match = squareRegex.find(text) ?: return null
        val sq = ChessSquare.fromAlgebraic(match.value) ?: return null

        return Pair(sq, ChessPiece(type, color))
    }

    fun captureScreen(onCaptured: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshotResult: ScreenshotResult) {
                        try {
                            val hardwareBuffer = screenshotResult.hardwareBuffer
                            val colorSpace = screenshotResult.colorSpace
                            val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                                ?.copy(Bitmap.Config.ARGB_8888, false)
                            hardwareBuffer.close()
                            onCaptured(bitmap)
                        } catch (e: Exception) {
                            onCaptured(null)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        onCaptured(null)
                    }
                }
            )
        } else {
            onCaptured(null)
        }
    }

    /**
     * Announces text for TalkBack and screen reader users
     */
    fun speak(text: String) {
        try {
            val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
            if (am?.isEnabled == true) {
                val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
                event.text.add(text)
                event.className = javaClass.name
                event.packageName = packageName
                event.isEnabled = true
                am.sendAccessibilityEvent(event)
            }
        } catch (e: Exception) {}
    }

    /**
     * Tactile haptic feedback for accessibility
     */
    fun vibrateFeedback(type: FeedbackType) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val v = vm?.defaultVibrator
                when (type) {
                    FeedbackType.CLICK -> v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    FeedbackType.SUCCESS -> v?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                    FeedbackType.WARNING -> v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 200), -1))
                }
            } else {
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (type) {
                        FeedbackType.CLICK -> v?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                        FeedbackType.SUCCESS -> v?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
                        FeedbackType.WARNING -> v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 200), -1))
                    }
                } else {
                    v?.vibrate(50)
                }
            }
        } catch (e: Exception) {}
    }

    enum class FeedbackType {
        CLICK,
        SUCCESS,
        WARNING
    }

    companion object {
        var instance: ChessLensAccessibilityService? = null
            private set

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _currentForegroundPackage = MutableStateFlow<String?>(null)
        val currentForegroundPackage: StateFlow<String?> = _currentForegroundPackage.asStateFlow()
    }
}
