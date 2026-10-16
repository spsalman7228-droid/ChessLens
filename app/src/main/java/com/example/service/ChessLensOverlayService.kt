package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.ChessLensApplication
import com.example.MainActivity
import com.example.R
import com.example.chess.DetailLevel
import com.example.model.AnalysisLevel
import com.example.model.ChessPosition
import com.example.model.MoveAnalysis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

class ChessLensOverlayService : Service() {

    private var windowManager: WindowManager? = null

    // 1. Floating Bubble Icon
    private var floatingBubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    // 2. Fullscreen Transparent Arrow Overlay
    private var arrowOverlayView: ArrowCanvasOverlayView? = null
    private var arrowOverlayParams: WindowManager.LayoutParams? = null

    // 3. Floating Result Card
    private var resultCardView: View? = null
    private var resultCardParams: WindowManager.LayoutParams? = null

    // 4. Floating Settings / Quick Action Panel
    private var settingsPanelView: View? = null
    private var settingsPanelParams: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var liveAnalysisJob: Job? = null

    // User settings & state
    private var isLiveMode = false
    private var isThinkFirst = false
    private var isRevealed = false
    private var isFlipped = false
    private var alwaysAnalyzeMoves = true
    private var selectedDetailLevel = DetailLevel.INTERMEDIATE
    private var currentBubbleSizeDp = 54
    private var currentBubbleAlpha = 1.0f

    // Simultaneous Multi-API Levels
    private var l1Enabled = true
    private var l2Enabled = true
    private var l3Enabled = true
    private var lichessApiEnabled = true
    private var stockfishOnlineEnabled = true
    private var avoidEnabled = true

    // Last analysis memory
    private var lastAnalyzedPosition: ChessPosition? = null
    private var lastBounds: Rect = Rect()
    private var lastAnalyses: List<MoveAnalysis> = emptyList()
    private var lastAvoidMoves: List<MoveAnalysis> = emptyList()

    fun onBoardContentChanged() {
        if (alwaysAnalyzeMoves && !isPaused.value) {
            triggerScreenAnalysis()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isOverlayRunning.value = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(isPaused = false))

        initFloatingBubble()
        
        serviceScope.launch {
            val app = application as? com.example.ChessLensApplication ?: return@launch
            app.preferencesManager.alwaysAnalyzeMoves.collect { enabled ->
                alwaysAnalyzeMoves = enabled
                updateNotification(isPaused = _isPaused.value)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ANALYZE_NOW -> triggerScreenAnalysis()
            ACTION_CLEAR_ARROWS -> hideScreenArrows()
            ACTION_PAUSE -> {
                _isPaused.value = true
                liveAnalysisJob?.cancel()
                updateNotification(isPaused = true)
                updateBubbleAppearance()
            }
            ACTION_RESUME -> {
                _isPaused.value = false
                updateNotification(isPaused = false)
                updateBubbleAppearance()
            }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Feature: Small floating icon stays even if the main app is closed / swiped from recents!
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        liveAnalysisJob?.cancel()
        hideScreenArrows()
        removeFloatingResultCard()
        removeSettingsPanel()
        removeFloatingBubble()
        _isOverlayRunning.value = false
    }

    // ==========================================
    // 1. FLOATING BUBBLE
    // ==========================================

    @SuppressLint("ClickableViewAccessibility")
    private fun initFloatingBubble() {
        if (!Settings.canDrawOverlays(this)) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = resources.displayMetrics.density
        val sizePx = (currentBubbleSizeDp * density).toInt()

        bubbleParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 300
        }

        val frame = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0F172A"))
                setStroke((2.5f * density).toInt(), Color.parseColor("#38BDF8"))
            }
            background = bg
            elevation = 16 * density
        }

        val knightText = TextView(this).apply {
            text = "♞"
            textSize = (currentBubbleSizeDp * 0.46f).coerceAtLeast(18f)
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
        }
        frame.addView(
            knightText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )

        // Status indicator dot (Top-Right)
        val statusDot = View(this).apply {
            val dotBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (_isPaused.value) Color.parseColor("#F87171") else Color.parseColor("#4ADE80"))
            }
            background = dotBg
        }
        val dotSize = (11 * density).toInt()
        val dotParams = FrameLayout.LayoutParams(dotSize, dotSize).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = (3 * density).toInt()
            rightMargin = (3 * density).toInt()
        }
        frame.addView(statusDot, dotParams)

        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f

        frame.setOnTouchListener { _, event ->
            val p = bubbleParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = initialX + (event.rawX - touchStartX).toInt()
                    p.y = initialY + (event.rawY - touchStartY).toInt()
                    try { windowManager?.updateViewLayout(frame, p) } catch (e: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dX = abs(event.rawX - touchStartX)
                    val dY = abs(event.rawY - touchStartY)
                    if (dX < 12 && dY < 12) {
                        // Tapped: Toggle Floating Controls & Settings Panel
                        toggleSettingsPanel()
                    } else {
                        // Drag released: snap to nearest screen edge
                        val screenWidth = resources.displayMetrics.widthPixels
                        p.x = if (event.rawX < screenWidth / 2) 20 else (screenWidth - p.width - 20)
                        try { windowManager?.updateViewLayout(frame, p) } catch (e: Exception) {}
                    }
                    true
                }
                else -> false
            }
        }

        floatingBubbleView = frame
        try {
            windowManager?.addView(frame, bubbleParams)
        } catch (e: Exception) {
            Log.e("ChessLens", "Error adding bubble view", e)
        }
    }

    private fun updateBubbleAppearance() {
        floatingBubbleView?.alpha = currentBubbleAlpha
    }

    private fun removeFloatingBubble() {
        floatingBubbleView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) {}
            floatingBubbleView = null
        }
    }

    // ==========================================
    // 2. ARROW CANVAS OVERLAY (DRAWS DIRECTLY OVER BOARD)
    // ==========================================

    private fun showScreenArrows(
        rect: Rect,
        analyses: List<MoveAnalysis>,
        avoidMoves: List<MoveAnalysis>,
        flipped: Boolean
    ) {
        if (!Settings.canDrawOverlays(this)) return

        if (arrowOverlayView == null) {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            arrowOverlayParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            arrowOverlayView = ArrowCanvasOverlayView(this)
            try {
                windowManager?.addView(arrowOverlayView, arrowOverlayParams)
            } catch (e: Exception) {
                Log.e("ChessLens", "Error adding arrow overlay", e)
            }
        }

        arrowOverlayView?.setAnalysisData(
            rect = rect,
            analysesList = analyses,
            avoidList = avoidMoves,
            flipped = flipped
        )
    }

    private fun hideScreenArrows() {
        arrowOverlayView?.clear()
        arrowOverlayView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) {}
            arrowOverlayView = null
        }
    }

    // ==========================================
    // 3. SCREEN ANALYSIS EXECUTION (MULTI-API SIMULTANEOUS)
    // ==========================================

    private fun triggerScreenAnalysis() {
        val accessibility = ChessLensAccessibilityService.instance
        if (accessibility == null) {
            Toast.makeText(this, "Accessibility permission required to analyze screen", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } catch (e: Exception) {}
            return
        }

        Toast.makeText(this, "Scanning screen for chessboard...", Toast.LENGTH_SHORT).show()

        // 1. Accessibility node hierarchy scan (<5ms, zero battery)
        val nodeResult = accessibility.tryReadPositionFromAccessibilityNodesWithBounds()
        if (nodeResult != null) {
            val effectiveBounds = if (nodeResult.bounds.width() > 60 && nodeResult.bounds.height() > 60) {
                nodeResult.bounds
            } else {
                calculateDefaultScreenBounds()
            }
            runMultiApiAnalysis(nodeResult.position, effectiveBounds, nodeResult.isFlipped)
            accessibility.vibrateFeedback(ChessLensAccessibilityService.FeedbackType.SUCCESS)
            return
        }

        // 2. High-speed screen frame capture
        accessibility.captureScreen { bitmap ->
            if (bitmap != null) {
                serviceScope.launch(Dispatchers.Default) {
                    val detection = ChessLensApplication.instance.boardDetector.detectBoard(bitmap)
                    if (detection != null) {
                        launch(Dispatchers.Main) {
                            runMultiApiAnalysis(detection.position, detection.bounds, detection.isFlipped)
                            accessibility.vibrateFeedback(ChessLensAccessibilityService.FeedbackType.SUCCESS)
                        }
                    } else {
                        launch(Dispatchers.Main) {
                            Toast.makeText(
                                this@ChessLensOverlayService,
                                "No chessboard recognized. Use 'Demo Position' in bubble to test arrows.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Unable to capture screen. Check accessibility permissions.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Executes multiple chess engines simultaneously in parallel (Local levels + Lichess + Stockfish.online)
     * and displays the resulting colored arrows directly on the screen!
     */
    private fun runMultiApiAnalysis(
        position: ChessPosition,
        boardBounds: Rect,
        boardFlipped: Boolean
    ) {
        val app = ChessLensApplication.instance
        val accessibility = ChessLensAccessibilityService.instance

        val effectiveFlipped = if (isFlipped) !boardFlipped else boardFlipped
        val bounds = if (boardBounds.width() > 60 && boardBounds.height() > 60) boardBounds else calculateDefaultScreenBounds()

        // Configure active levels for SIMULTANEOUS execution
        val levels = mutableListOf<AnalysisLevel>()
        if (l1Enabled) levels.add(AnalysisLevel("l1", "Level 1", "#38BDF8", depth = 5))
        if (l2Enabled) levels.add(AnalysisLevel("l2", "Level 2", "#2DD4BF", depth = 10))
        if (l3Enabled) levels.add(AnalysisLevel("l3", "Level 3", "#4ADE80", depth = 14))

        serviceScope.launch {
            try {
                // Multi-API simultaneous run
                val results = app.evaluationEngine.analyzeMultipleLevelsSimultaneously(
                    position = position,
                    levels = levels,
                    detailLevel = selectedDetailLevel,
                    webSource = app.webAnalysisSource,
                    includeWebApis = lichessApiEnabled || stockfishOnlineEnabled,
                    onProgress = { interimResults ->
                        // Progressive rendering: as fast depths return, show arrows immediately!
                        if (!isThinkFirst || isRevealed) {
                            showScreenArrows(bounds, interimResults, emptyList(), effectiveFlipped)
                        }
                    }
                )

                val avoids = if (avoidEnabled) {
                    app.evaluationEngine.findMovesToAvoid(
                        position = position,
                        level = levels.firstOrNull() ?: AnalysisLevel("l1", "L1", "#38BDF8", 5)
                    )
                } else emptyList()

                lastAnalyzedPosition = position
                lastBounds = bounds
                lastAnalyses = results
                lastAvoidMoves = avoids

                // Show arrows on screen (unless Think-First is pending reveal)
                if (!isThinkFirst || isRevealed) {
                    showScreenArrows(bounds, results, avoids, effectiveFlipped)
                }

                // Show floating result card
                showFloatingResultCard(position, results, avoids, bounds, effectiveFlipped)

                // Accessibility voice announcement
                val topMove = results.firstOrNull()
                if (topMove != null) {
                    val evalStr = if (topMove.evalCentipawns > 0) "+${topMove.evalCentipawns / 100f}" else "${topMove.evalCentipawns / 100f}"
                    accessibility?.speak("ChessLens: Recommended move ${topMove.move.san}, eval $evalStr. ${topMove.explanation}")
                }
            } catch (e: Exception) {
                Log.e("ChessLens", "Error during multi-API analysis", e)
            }
        }
    }

    private fun calculateDefaultScreenBounds(): Rect {
        val metrics = resources.displayMetrics
        val w = metrics.widthPixels
        val h = metrics.heightPixels
        val size = min(w, (h * 0.72f).toInt())
        val left = (w - size) / 2
        val top = (h - size) / 2
        return Rect(left, top, left + size, top + size)
    }

    // ==========================================
    // 4. FLOATING RESULT CARD (MOVABLE / COLLAPSIBLE)
    // ==========================================

    @SuppressLint("SetTextI18n")
    private fun showFloatingResultCard(
        position: ChessPosition,
        analyses: List<MoveAnalysis>,
        avoidMoves: List<MoveAnalysis>,
        bounds: Rect,
        flipped: Boolean
    ) {
        if (!Settings.canDrawOverlays(this)) return
        removeFloatingResultCard()

        val density = resources.displayMetrics.density
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        resultCardParams = WindowManager.LayoutParams(
            (320 * density).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (48 * density).toInt()
        }

        val top = analyses.firstOrNull()
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = 16 * density
                setColor(Color.parseColor("#0F172A"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#38BDF8"))
            }
            setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt())
            elevation = 18 * density
        }

        // Header with Eval badge & Close
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val evalBadge = TextView(this).apply {
            val evalText = if (top != null) "${if (top.evalCentipawns > 0) "+" else ""}%.1f".format(top.evalCentipawns / 100f) else "0.0"
            text = "Eval $evalText"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
            background = GradientDrawable().apply {
                cornerRadius = 6 * density
                setColor(Color.parseColor("#1E293B"))
            }
            setPadding((8 * density).toInt(), (3 * density).toInt(), (8 * density).toInt(), (3 * density).toInt())
        }
        val title = TextView(this).apply {
            text = " Best: ${top?.move?.san ?: "None"}"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val dismissBtn = TextView(this).apply {
            text = "✕"
            textSize = 17f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding((8 * density).toInt(), 0, (4 * density).toInt(), 0)
            setOnClickListener {
                hideScreenArrows()
                removeFloatingResultCard()
            }
        }
        row1.addView(evalBadge)
        row1.addView(title)
        row1.addView(dismissBtn)
        card.addView(row1)

        // Think-First Reveal Button if applicable
        if (isThinkFirst && !isRevealed) {
            val revealBtn = Button(this).apply {
                text = "👁 Reveal Moves & Colored Arrows"
                setTextColor(Color.parseColor("#0F172A"))
                setTypeface(null, Typeface.BOLD)
                background = GradientDrawable().apply {
                    cornerRadius = 8 * density
                    setColor(Color.parseColor("#4ADE80"))
                }
                setOnClickListener {
                    isRevealed = true
                    showScreenArrows(bounds, analyses, avoidMoves, flipped)
                    visibility = View.GONE
                }
            }
            card.addView(revealBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (38 * density).toInt()).apply {
                topMargin = (8 * density).toInt()
                bottomMargin = (4 * density).toInt()
            })
        }

        // Pedagogical Move Explanation text
        if (top != null) {
            val expl = TextView(this).apply {
                text = top.explanation
                textSize = 12.5f
                setTextColor(Color.parseColor("#E2E8F0"))
                setPadding(0, (6 * density).toInt(), 0, (6 * density).toInt())
            }
            card.addView(expl)
        }

        // Avoid move warning
        val avoid = avoidMoves.firstOrNull()
        if (avoid != null) {
            val avoidBox = TextView(this).apply {
                text = "⚠ Avoid ${avoid.move.san}: ${avoid.avoidReason ?: avoid.explanation}"
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#F87171"))
                background = GradientDrawable().apply {
                    cornerRadius = 6 * density
                    setColor(Color.parseColor("#261418"))
                }
                setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            }
            card.addView(avoidBox)
        }

        // Action row: Hide Arrows, Copy FEN, Open Studio
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (8 * density).toInt(), 0, 0)
        }
        val hideBtn = Button(this).apply {
            text = "Hide Arrows"
            textSize = 11f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 6 * density
                setColor(Color.parseColor("#1E293B"))
            }
            setOnClickListener {
                hideScreenArrows()
                removeFloatingResultCard()
            }
        }
        val copyBtn = Button(this).apply {
            text = "Copy FEN"
            textSize = 11f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 6 * density
                setColor(Color.parseColor("#1E293B"))
            }
            setOnClickListener {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("FEN", position.toFen()))
                Toast.makeText(this@ChessLensOverlayService, "FEN Copied!", Toast.LENGTH_SHORT).show()
            }
        }
        val openAppBtn = Button(this).apply {
            text = "Full Studio"
            textSize = 11f
            setTextColor(Color.parseColor("#0F172A"))
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = 6 * density
                setColor(Color.parseColor("#38BDF8"))
            }
            setOnClickListener {
                val intent = Intent(this@ChessLensOverlayService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                removeFloatingResultCard()
            }
        }
        actionRow.addView(hideBtn, LinearLayout.LayoutParams(0, (32 * density).toInt(), 1f).apply { marginEnd = (4 * density).toInt() })
        actionRow.addView(copyBtn, LinearLayout.LayoutParams(0, (32 * density).toInt(), 1f).apply { marginEnd = (4 * density).toInt() })
        actionRow.addView(openAppBtn, LinearLayout.LayoutParams(0, (32 * density).toInt(), 1f))
        card.addView(actionRow)

        resultCardView = card
        try {
            windowManager?.addView(card, resultCardParams)
        } catch (e: Exception) {
            Log.e("ChessLens", "Error adding result card", e)
        }
    }

    private fun removeFloatingResultCard() {
        resultCardView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) {}
            resultCardView = null
        }
    }

    // ==========================================
    // 5. FLOATING SETTINGS & QUICK CONTROLS MODAL
    // ==========================================

    private fun toggleSettingsPanel() {
        if (settingsPanelView != null) {
            removeSettingsPanel()
        } else {
            showSettingsPanel()
        }
    }

    private fun removeSettingsPanel() {
        settingsPanelView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) {}
            settingsPanelView = null
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showSettingsPanel() {
        if (!Settings.canDrawOverlays(this)) return

        val density = resources.displayMetrics.density
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        settingsPanelParams = WindowManager.LayoutParams(
            (340 * density).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                cornerRadius = 18 * density
                setColor(Color.parseColor("#0F172A"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#38BDF8"))
            }
            background = bg
            setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            elevation = 20 * density
        }

        // Header: Title & Close
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "ChessLens Floating Controls"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            setOnClickListener { removeSettingsPanel() }
        }
        header.addView(title)
        header.addView(closeBtn)
        card.addView(header)

        // 1. Primary Action: Analyze Screen Now & Show Moves
        val analyzeBtn = Button(this).apply {
            text = "⚡ Analyze Screen & Show Moves"
            setTextColor(Color.parseColor("#0F172A"))
            setTypeface(null, Typeface.BOLD)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 10 * density
                setColor(Color.parseColor("#38BDF8"))
            }
            background = btnBg
            setOnClickListener {
                isRevealed = false
                triggerScreenAnalysis()
                removeSettingsPanel()
            }
        }
        card.addView(analyzeBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (44 * density).toInt()
        ).apply {
            topMargin = (12 * density).toInt()
            bottomMargin = (6 * density).toInt()
        })

        // 2. Demo / Instant Test Board Button (Guarantees user sees arrows working immediately)
        val demoBtn = Button(this).apply {
            text = "🎯 Test Demo Board (Show Arrows Now)"
            textSize = 12f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 8 * density
                setColor(Color.parseColor("#1E293B"))
                setStroke((1 * density).toInt(), Color.parseColor("#2DD4BF"))
            }
            setOnClickListener {
                removeSettingsPanel()
                val demoPosition = ChessPosition.fromFen("r1b1kb1r/pppp1ppp/5n2/4q3/4P3/2N5/PPP2PPP/R1BQKB1R w KQkq - 0 7")
                    ?: ChessPosition.initial()
                val bounds = calculateDefaultScreenBounds()
                Toast.makeText(this@ChessLensOverlayService, "Displaying Demo Analysis Arrows on Screen!", Toast.LENGTH_SHORT).show()
                runMultiApiAnalysis(demoPosition, bounds, isFlipped)
            }
        }
        card.addView(demoBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (36 * density).toInt()
        ).apply { bottomMargin = (8 * density).toInt() })

        // Clear arrows button if active
        if (arrowOverlayView?.hasActiveArrows() == true) {
            val clearBtn = Button(this).apply {
                text = "✕ Clear Arrows from Screen"
                textSize = 12f
                setTextColor(Color.parseColor("#F87171"))
                background = GradientDrawable().apply {
                    cornerRadius = 8 * density
                    setColor(Color.parseColor("#261418"))
                }
                setOnClickListener {
                    hideScreenArrows()
                    removeFloatingResultCard()
                    removeSettingsPanel()
                }
            }
            card.addView(clearBtn, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (36 * density).toInt()
            ).apply { bottomMargin = (8 * density).toInt() })
        }

        // Scrollable settings section
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (260 * density).toInt()
            )
        }
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Switches
        scrollContent.addView(createToggleRow("Continuous Live Mode (Auto-Scan)", isLiveMode) { checked ->
            isLiveMode = checked
            if (checked) startLiveAnalysisLoop() else liveAnalysisJob?.cancel()
        })
        scrollContent.addView(createToggleRow("Think-First Mode (Hide until Reveal)", isThinkFirst) { checked ->
            isThinkFirst = checked
        })
        scrollContent.addView(createToggleRow("Flip Board (Black POV)", isFlipped) { checked ->
            isFlipped = checked
            if (lastAnalyses.isNotEmpty()) {
                showScreenArrows(lastBounds, lastAnalyses, lastAvoidMoves, isFlipped)
            }
        })

        // Simultaneous Multi-API Levels Section
        val apiHeader = TextView(this).apply {
            text = "Simultaneous Multi-API / Levels:"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, (8 * density).toInt(), 0, (4 * density).toInt())
        }
        scrollContent.addView(apiHeader)

        scrollContent.addView(createToggleRow("Level 1: Depth 5 (Blue Arrow)", l1Enabled) { l1Enabled = it })
        scrollContent.addView(createToggleRow("Level 2: Depth 10 (Teal Arrow)", l2Enabled) { l2Enabled = it })
        scrollContent.addView(createToggleRow("Level 3: Depth 14 (Green Arrow)", l3Enabled) { l3Enabled = it })
        scrollContent.addView(createToggleRow("Lichess Cloud Database API", lichessApiEnabled) { lichessApiEnabled = it })
        scrollContent.addView(createToggleRow("Stockfish Online API", stockfishOnlineEnabled) { stockfishOnlineEnabled = it })
        scrollContent.addView(createToggleRow("Moves to Avoid (Red Dashed Arrow)", avoidEnabled) { avoidEnabled = it })

        // Bubble Size selector
        val sizeHeader = TextView(this).apply {
            text = "Floating Bubble Size:"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, (8 * density).toInt(), 0, (4 * density).toInt())
        }
        scrollContent.addView(sizeHeader)

        val sizeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        listOf(Pair("Small", 44), Pair("Medium", 54), Pair("Large", 64)).forEach { (name, s) ->
            val b = Button(this).apply {
                text = name
                textSize = 11f
                val isSel = currentBubbleSizeDp == s
                setTextColor(if (isSel) Color.parseColor("#0F172A") else Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = 8 * density
                    setColor(if (isSel) Color.parseColor("#38BDF8") else Color.parseColor("#1E293B"))
                }
                setOnClickListener {
                    currentBubbleSizeDp = s
                    updateBubbleSize(s)
                    removeSettingsPanel()
                    showSettingsPanel()
                }
            }
            val lp = LinearLayout.LayoutParams(0, (36 * density).toInt(), 1f).apply {
                marginEnd = (4 * density).toInt()
            }
            sizeRow.addView(b, lp)
        }
        scrollContent.addView(sizeRow)

        // Bubble Opacity selector
        val alphaHeader = TextView(this).apply {
            text = "Floating Bubble Opacity:"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, (8 * density).toInt(), 0, (4 * density).toInt())
        }
        scrollContent.addView(alphaHeader)

        val alphaRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        listOf(Pair("100%", 1.0f), Pair("75%", 0.75f), Pair("50%", 0.5f)).forEach { (name, a) ->
            val b = Button(this).apply {
                text = name
                textSize = 11f
                val isSel = currentBubbleAlpha == a
                setTextColor(if (isSel) Color.parseColor("#0F172A") else Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = 8 * density
                    setColor(if (isSel) Color.parseColor("#38BDF8") else Color.parseColor("#1E293B"))
                }
                setOnClickListener {
                    currentBubbleAlpha = a
                    updateBubbleAppearance()
                    removeSettingsPanel()
                    showSettingsPanel()
                }
            }
            val lp = LinearLayout.LayoutParams(0, (36 * density).toInt(), 1f).apply {
                marginEnd = (4 * density).toInt()
            }
            alphaRow.addView(b, lp)
        }
        scrollContent.addView(alphaRow)

        scrollView.addView(scrollContent)
        card.addView(scrollView)

        // Quick Launch: Chess.com Browser
        val openBrowserBtn = Button(this).apply {
            text = "🌐 Open Chess.com Browser"
            textSize = 12f
            setTextColor(Color.parseColor("#0F172A"))
            setTypeface(null, Typeface.BOLD)
            background = GradientDrawable().apply {
                cornerRadius = 8 * density
                setColor(Color.parseColor("#2DD4BF"))
            }
            setOnClickListener {
                val intent = Intent(this@ChessLensOverlayService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                removeSettingsPanel()
            }
        }
        card.addView(openBrowserBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (36 * density).toInt()
        ).apply {
            topMargin = (10 * density).toInt()
        })

        // Footer buttons: Open App & Close
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (8 * density).toInt(), 0, 0)
        }
        val openAppBtn = Button(this).apply {
            text = "Full Studio"
            textSize = 12f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 8 * density
                setColor(Color.parseColor("#1E293B"))
            }
            setOnClickListener {
                val intent = Intent(this@ChessLensOverlayService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                removeSettingsPanel()
            }
        }
        val closeFloatingBtn = Button(this).apply {
            text = "Close Floating Icon"
            textSize = 12f
            setTextColor(Color.parseColor("#F87171"))
            background = GradientDrawable().apply {
                cornerRadius = 8 * density
                setColor(Color.parseColor("#261418"))
            }
            setOnClickListener {
                removeSettingsPanel()
                stopSelf()
            }
        }
        footer.addView(openAppBtn, LinearLayout.LayoutParams(0, (36 * density).toInt(), 1f).apply { marginEnd = (6 * density).toInt() })
        footer.addView(closeFloatingBtn, LinearLayout.LayoutParams(0, (36 * density).toInt(), 1f))
        card.addView(footer)

        settingsPanelView = card
        try {
            windowManager?.addView(card, settingsPanelParams)
        } catch (e: Exception) {
            Log.e("ChessLens", "Error adding settings panel", e)
        }
    }

    private fun updateBubbleSize(sizeDp: Int) {
        val density = resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()
        bubbleParams?.width = sizePx
        bubbleParams?.height = sizePx
        floatingBubbleView?.let { v ->
            try { windowManager?.updateViewLayout(v, bubbleParams) } catch (e: Exception) {}
        }
    }

    private fun createToggleRow(label: String, initialChecked: Boolean, onChecked: (Boolean) -> Unit): LinearLayout {
        val density = resources.displayMetrics.density
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())

            val tv = TextView(this@ChessLensOverlayService).apply {
                text = label
                textSize = 12.5f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val sw = Switch(this@ChessLensOverlayService).apply {
                isChecked = initialChecked
                setOnCheckedChangeListener { _, c -> onChecked(c) }
            }
            addView(tv)
            addView(sw)
        }
    }

    private fun startLiveAnalysisLoop() {
        liveAnalysisJob?.cancel()
        liveAnalysisJob = serviceScope.launch {
            while (isActive && isLiveMode) {
                delay(2500)
                if (!_isPaused.value) {
                    triggerScreenAnalysis()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(isPaused: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(isPaused))
    }

    private fun buildNotification(isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val analyzeIntent = Intent(this, ChessLensOverlayService::class.java).apply {
            action = ACTION_ANALYZE_NOW
        }
        val analyzePendingIntent = PendingIntent.getService(
            this, 1, analyzeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleActionIntent = Intent(this, ChessLensOverlayService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            this, 2, toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.chesslens_icon)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(if (isPaused) "Floating bubble paused" else "Floating bubble active on screen")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.chesslens_icon,
                getString(R.string.action_analyze_now),
                analyzePendingIntent
            )
            .addAction(
                R.drawable.chesslens_icon,
                if (isPaused) getString(R.string.action_resume) else getString(R.string.action_pause),
                togglePendingIntent
            )
            .build()
    }

    companion object {
        var instance: ChessLensOverlayService? = null
        const val CHANNEL_ID = "chesslens_overlay_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_ANALYZE_NOW = "com.example.chesslens.ACTION_ANALYZE_NOW"
        const val ACTION_CLEAR_ARROWS = "com.example.chesslens.ACTION_CLEAR_ARROWS"
        const val ACTION_PAUSE = "com.example.chesslens.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.chesslens.ACTION_RESUME"
        const val ACTION_STOP = "com.example.chesslens.ACTION_STOP"

        private val _isOverlayRunning = MutableStateFlow(false)
        val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()
    }
}
