package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import com.example.model.ChessMove
import com.example.model.MoveAnalysis
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class ArrowCanvasOverlayView(context: Context) : View(context) {

    private var boardRect: Rect = Rect()
    private var analyses: List<MoveAnalysis> = emptyList()
    private var avoidMoves: List<MoveAnalysis> = emptyList()
    private var isFlipped: Boolean = false
    private var showArrows: Boolean = false

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#99000000") // 60% black shadow
    }

    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val headOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#99000000")
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F00F172A")
        style = Paint.Style.FILL
    }

    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#4D38BDF8")
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B338BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
    }

    fun setAnalysisData(
        rect: Rect,
        analysesList: List<MoveAnalysis>,
        avoidList: List<MoveAnalysis>,
        flipped: Boolean
    ) {
        this.boardRect = if (rect.width() > 60 && rect.height() > 60) {
            rect
        } else {
            // Safe fallback bounds
            Rect()
        }
        this.analyses = analysesList
        this.avoidMoves = avoidList
        this.isFlipped = flipped
        this.showArrows = true
        postInvalidate()
    }

    fun clear() {
        this.analyses = emptyList()
        this.avoidMoves = emptyList()
        this.showArrows = false
        postInvalidate()
    }

    fun hasActiveArrows(): Boolean = showArrows && (analyses.isNotEmpty() || avoidMoves.isNotEmpty())

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!showArrows || (analyses.isEmpty() && avoidMoves.isEmpty())) return

        val density = resources.displayMetrics.density

        // Determine effective board bounds (if not detected, use screen center)
        val effectiveRect = if (boardRect.width() > 60 && boardRect.height() > 60) {
            boardRect
        } else {
            val w = width.takeIf { it > 100 } ?: resources.displayMetrics.widthPixels
            val h = height.takeIf { it > 100 } ?: resources.displayMetrics.heightPixels
            val size = min(w, (h * 0.75f).toInt())
            val l = (w - size) / 2
            val t = (h - size) / 2
            Rect(l, t, l + size, t + size)
        }

        val sqW = effectiveRect.width() / 8f
        val sqH = effectiveRect.height() / 8f

        // Draw soft glowing border around detected board
        canvas.drawRoundRect(
            RectF(effectiveRect),
            12 * density,
            12 * density,
            borderPaint
        )

        // Draw high-contrast modern HUD corners
        val cornerLen = 22 * density
        val rL = effectiveRect.left.toFloat()
        val rT = effectiveRect.top.toFloat()
        val rR = effectiveRect.right.toFloat()
        val rB = effectiveRect.bottom.toFloat()

        // Top-Left corner
        canvas.drawLine(rL, rT, rL + cornerLen, rT, cornerPaint)
        canvas.drawLine(rL, rT, rL, rT + cornerLen, cornerPaint)
        // Top-Right corner
        canvas.drawLine(rR, rT, rR - cornerLen, rT, cornerPaint)
        canvas.drawLine(rR, rT, rR, rT + cornerLen, cornerPaint)
        // Bottom-Left corner
        canvas.drawLine(rL, rB, rL + cornerLen, rB, cornerPaint)
        canvas.drawLine(rL, rB, rL, rB - cornerLen, cornerPaint)
        // Bottom-Right corner
        canvas.drawLine(rR, rB, rR - cornerLen, rB, cornerPaint)
        canvas.drawLine(rR, rB, rR, rB - cornerLen, cornerPaint)

        // Group analyses by move to calculate agreement count ("x2", "x3")
        val grouped = analyses.groupBy { it.move.uci }

        // Draw candidate moves
        grouped.forEach { (_, group) ->
            val primary = group.first()
            val move = primary.move
            val arrowColor = try {
                Color.parseColor(primary.levelColorHex)
            } catch (e: Exception) {
                Color.parseColor("#38BDF8")
            }

            drawMoveArrow(
                canvas = canvas,
                effectiveRect = effectiveRect,
                move = move,
                color = arrowColor,
                sqW = sqW,
                sqH = sqH,
                density = density,
                label = primary.move.san,
                agreementCount = group.size,
                isAvoid = false
            )
        }

        // Draw moves to avoid (Red Dashed)
        avoidMoves.forEach { avoid ->
            drawMoveArrow(
                canvas = canvas,
                effectiveRect = effectiveRect,
                move = avoid.move,
                color = Color.parseColor("#F87171"),
                sqW = sqW,
                sqH = sqH,
                density = density,
                label = "Avoid: ${avoid.move.san}",
                agreementCount = 1,
                isAvoid = true
            )
        }
    }

    private fun drawMoveArrow(
        canvas: Canvas,
        effectiveRect: Rect,
        move: ChessMove,
        color: Int,
        sqW: Float,
        sqH: Float,
        density: Float,
        label: String,
        agreementCount: Int,
        isAvoid: Boolean
    ) {
        val fromR = if (isFlipped) 7 - move.from.row else move.from.row
        val fromC = if (isFlipped) 7 - move.from.col else move.from.col
        val toR = if (isFlipped) 7 - move.to.row else move.to.row
        val toC = if (isFlipped) 7 - move.to.col else move.to.col

        val startX = effectiveRect.left + (fromC + 0.5f) * sqW
        val startY = effectiveRect.top + (fromR + 0.5f) * sqH
        val endX = effectiveRect.left + (toC + 0.5f) * sqW
        val endY = effectiveRect.top + (toR + 0.5f) * sqH

        // 1. Highlight from-square and to-square
        val sqAlpha = if (isAvoid) 0x44 else 0x55
        highlightPaint.color = (sqAlpha shl 24) or (color and 0x00FFFFFF)
        val toLeft = effectiveRect.left + toC * sqW
        val toTop = effectiveRect.top + toR * sqH
        canvas.drawRoundRect(
            RectF(toLeft + 3, toTop + 3, toLeft + sqW - 3, toTop + sqH - 3),
            8 * density,
            8 * density,
            highlightPaint
        )

        // 2. Setup arrow stroke widths
        val strokeW = if (agreementCount > 1) sqW * 0.19f else sqW * 0.14f
        linePaint.strokeWidth = strokeW
        linePaint.color = color
        linePaint.alpha = if (isAvoid) 220 else 255

        outlinePaint.strokeWidth = strokeW + 4 * density

        if (isAvoid) {
            linePaint.pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
            outlinePaint.pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
        } else {
            linePaint.pathEffect = null
            outlinePaint.pathEffect = null
        }

        val dR = toR - fromR
        val dC = toC - fromC
        val isKnight = (abs(dR) == 1 && abs(dC) == 2) || (abs(dR) == 2 && abs(dC) == 1)

        val path = Path()
        var arrowAngle = 0.0

        if (isKnight) {
            val cornerX = if (abs(dR) == 2) startX else endX
            val cornerY = if (abs(dR) == 2) endY else startY

            path.moveTo(startX, startY)
            path.lineTo(cornerX, cornerY)
            path.lineTo(endX, endY)

            // Draw outline shadow then color path
            canvas.drawPath(path, outlinePaint)
            canvas.drawPath(path, linePaint)

            arrowAngle = atan2((endY - cornerY).toDouble(), (endX - cornerX).toDouble())
        } else {
            // Draw outline shadow then color line
            canvas.drawLine(startX, startY, endX, endY, outlinePaint)
            canvas.drawLine(startX, startY, endX, endY, linePaint)
            arrowAngle = atan2((endY - startY).toDouble(), (endX - startX).toDouble())
        }

        // 3. Arrowhead
        headPaint.color = color
        headPaint.alpha = 255
        val headLength = sqW * 0.40f
        val wingAngle = Math.PI / 5.2

        val p1X = (endX - headLength * cos(arrowAngle - wingAngle)).toFloat()
        val p1Y = (endY - headLength * sin(arrowAngle - wingAngle)).toFloat()
        val p2X = (endX - headLength * cos(arrowAngle + wingAngle)).toFloat()
        val p2Y = (endY - headLength * sin(arrowAngle + wingAngle)).toFloat()

        val headPath = Path().apply {
            moveTo(endX, endY)
            lineTo(p1X, p1Y)
            lineTo(p2X, p2Y)
            close()
        }
        canvas.drawPath(headPath, headOutlinePaint)
        canvas.drawPath(headPath, headPaint)

        // 4. Label Badge at arrowhead (e.g. "Nf3" or "x3" or "Avoid: e5")
        textPaint.textSize = (13 * density).coerceAtLeast(11f)
        val textWidth = textPaint.measureText(label)
        val badgeW = textWidth + 16 * density
        val badgeH = 22 * density
        val badgeX = endX
        val badgeY = endY - (16 * density)

        val badgeRect = RectF(badgeX - badgeW / 2, badgeY - badgeH / 2, badgeX + badgeW / 2, badgeY + badgeH / 2)
        canvas.drawRoundRect(badgeRect, 7 * density, 7 * density, badgeBgPaint)
        canvas.drawRoundRect(badgeRect, 7 * density, 7 * density, badgeBorderPaint)
        canvas.drawText(label, badgeX, badgeY + 4.5f * density, textPaint)

        // Agreement badge if multiple levels agree
        if (agreementCount > 1) {
            val agreeText = "x$agreementCount Agreement"
            val agreeW = textPaint.measureText(agreeText) + 12 * density
            val agreeRect = RectF(startX - agreeW / 2, startY - 11 * density, startX + agreeW / 2, startY + 11 * density)
            canvas.drawRoundRect(agreeRect, 6 * density, 6 * density, badgeBgPaint)
            canvas.drawRoundRect(agreeRect, 6 * density, 6 * density, badgeBorderPaint)
            canvas.drawText(agreeText, startX, startY + 4f * density, textPaint)
        }
    }
}
