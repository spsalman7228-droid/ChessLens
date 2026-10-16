package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ChessLensApplication
import com.example.model.AnalysisLevel
import com.example.model.ChessPosition
import com.example.ui.theme.ChessLensCyan
import com.example.ui.theme.ChessLensGreen
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessBrowserScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: () -> Unit = {},
    onNavigateToVideo: () -> Unit = {},
    onNavigateToPuzzles: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = ChessLensApplication.instance
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageLoadingProgress by remember { mutableFloatStateOf(0f) }
    var isPageLoading by remember { mutableStateOf(true) }

    // Multi-API level toggles
    var useL1 by remember { mutableStateOf(true) }
    var useL2 by remember { mutableStateOf(true) }
    var useL3 by remember { mutableStateOf(true) }
    var useLichess by remember { mutableStateOf(true) }
    var useStockfishOnline by remember { mutableStateOf(true) }

    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onNavigateBack()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Slate900,
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate950)
                            .padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ChessLensCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("♞", fontSize = 26.sp, color = ChessLensCyan)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chess.com Browser",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Multi-API Injected Extension",
                            fontSize = 12.sp,
                            color = ChessLensGreen
                        )
                    }

                    HorizontalDivider(color = Slate800)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Navigation Items
                    NavigationDrawerItem(
                        label = { Text("Chess.com Browser", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Language, contentDescription = null, tint = ChessLensCyan) },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = ChessLensCyan.copy(alpha = 0.15f),
                            selectedTextColor = ChessLensCyan
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text("Analysis Studio") },
                        icon = { Icon(Icons.Default.Layers, contentDescription = null, tint = Color.White) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToAnalysis()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color.White
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text("Video Analysis") },
                        icon = { Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToVideo()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color.White
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text("Tactical Puzzles") },
                        icon = { Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.White) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToPuzzles()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color.White
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text("Analysis History") },
                        icon = { Icon(Icons.Default.History, contentDescription = null, tint = Color.White) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToHistory()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color.White
                        )
                    )

                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Slate800)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Multi-API Engine Settings in Drawer
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate950),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Simultaneous Multi-API Engine",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChessLensCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Level 1: Depth 5 (Blue)", fontSize = 12.sp, color = Color.White)
                                Switch(
                                    checked = useL1,
                                    onCheckedChange = { useL1 = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Slate900, checkedTrackColor = ChessLensCyan)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Level 2: Depth 10 (Teal)", fontSize = 12.sp, color = Color.White)
                                Switch(
                                    checked = useL2,
                                    onCheckedChange = { useL2 = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Slate900, checkedTrackColor = Color(0xFF2DD4BF))
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Level 3: Depth 14 (Green)", fontSize = 12.sp, color = Color.White)
                                Switch(
                                    checked = useL3,
                                    onCheckedChange = { useL3 = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Slate900, checkedTrackColor = Color(0xFF4ADE80))
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lichess Cloud Database", fontSize = 12.sp, color = Color.White)
                                Switch(
                                    checked = useLichess,
                                    onCheckedChange = { useLichess = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Slate900, checkedTrackColor = ChessLensCyan)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Stockfish Online API", fontSize = 12.sp, color = Color.White)
                                Switch(
                                    checked = useStockfishOnline,
                                    onCheckedChange = { useStockfishOnline = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Slate900, checkedTrackColor = ChessLensCyan)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate900,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "chess.com (Multi-API Active)",
                                        fontSize = 11.5.sp,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Drawer", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                webViewRef?.evaluateJavascript("if (window.clTrigger) window.clTrigger();", null)
                                Toast.makeText(context, "Running Multi-API Analysis...", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = "Analyze Now", tint = ChessLensCyan)
                        }
                        IconButton(
                            onClick = {
                                if (webViewRef?.canGoBack() == true) webViewRef?.goBack() else onNavigateBack()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
                )
            },
            containerColor = Slate950
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            configureWebView(this, app)
                            webViewRef = this
                            loadUrl("https://www.chess.com/play/computer")
                        }
                    },
                    update = { wv ->
                        webViewRef = wv
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isPageLoading && pageLoadingProgress < 1.0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    ) {
                        LinearProgressIndicator(
                            progress = { pageLoadingProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = ChessLensCyan,
                            trackColor = Slate900
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(webView: WebView, app: ChessLensApplication) {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = true
        builtInZoomControls = true
        displayZoomControls = false
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    // Add JavaScript Interface for Native Simultaneous Multi-API Engine
    val bridge = ChessExtensionBridge(app, webView)
    webView.addJavascriptInterface(bridge, "ChessLensAndroid")

    webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
        }
    }

    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            // Restrict browser exclusively to chess.com
            return if (url.contains("chess.com")) {
                false // Allow loading
            } else {
                Toast.makeText(webView.context, "ChessLens Browser is dedicated exclusively to chess.com", Toast.LENGTH_SHORT).show()
                true // Block external sites
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            // Inject Chess Helper extension with simultaneous multiple APIs & animated arrows
            injectChessLensExtension(view)
        }
    }
}

/**
 * JavaScript Bridge that connects Chess.com web page directly to Android's
 * simultaneous multi-API chess engine (Local depths + Lichess Cloud + Stockfish.online)
 */
class ChessExtensionBridge(
    private val app: ChessLensApplication,
    private val webView: WebView
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bridgeScope = CoroutineScope(Dispatchers.IO)

    @JavascriptInterface
    fun requestSimultaneousAnalysis(fen: String, depthD5: Int, depthD10: Int, depthD15: Int, useLichess: Boolean, useStockfishOnline: Boolean) {
        bridgeScope.launch {
            val position = ChessPosition.fromFen(fen) ?: return@launch

            val levels = mutableListOf<AnalysisLevel>()
            if (depthD5 > 0) levels.add(AnalysisLevel("d5", "Depth 5", "#38BDF8", depthD5))
            if (depthD10 > 0) levels.add(AnalysisLevel("d10", "Depth 10", "#2DD4BF", depthD10))
            if (depthD15 > 0) levels.add(AnalysisLevel("d15", "Depth 14", "#4ADE80", depthD15))

            // Run multiple APIs simultaneously in parallel!
            val results = app.evaluationEngine.analyzeMultipleLevelsSimultaneously(
                position = position,
                levels = levels,
                webSource = app.webAnalysisSource,
                includeWebApis = useLichess || useStockfishOnline,
                onProgress = { currentResults ->
                    dispatchResultsToWeb(currentResults)
                }
            )

            // Final delivery
            dispatchResultsToWeb(results)
        }
    }

    private fun dispatchResultsToWeb(results: List<com.example.model.MoveAnalysis>) {
        mainHandler.post {
            try {
                val jsonArr = JSONArray()
                results.forEach { r ->
                    val obj = JSONObject().apply {
                        put("level", r.levelName)
                        put("uci", r.move.uci)
                        put("san", r.move.san)
                        put("color", r.levelColorHex)
                        put("eval", r.evalCentipawns)
                        put("explanation", r.explanation)
                    }
                    jsonArr.put(obj)
                }
                webView.evaluateJavascript("if (window.onChessLensResults) { window.onChessLensResults($jsonArr); }", null)
            } catch (e: Exception) {}
        }
    }
}

private fun injectChessLensExtension(webView: WebView?) {
    if (webView == null) return

    val script = """
        (function() {
            if (window.__chessLensInjected) return;
            window.__chessLensInjected = true;
            console.log('[ChessLens] Injecting Multi-API Chess Extension into chess.com...');

            // --- STYLES ---
            const style = document.createElement('style');
            style.innerHTML = `
                #chesslens-bubble {
                    position: fixed;
                    bottom: 24px;
                    right: 20px;
                    width: 52px;
                    height: 52px;
                    border-radius: 50%;
                    background: rgba(15, 23, 42, 0.95);
                    border: 2.5px solid #38bdf8;
                    box-shadow: 0 8px 32px rgba(0,0,0,0.6);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: #38bdf8;
                    font-size: 26px;
                    cursor: pointer;
                    z-index: 999999;
                    user-select: none;
                    transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
                }
                #chesslens-bubble:active { transform: scale(0.92); }
                #chesslens-panel {
                    position: fixed;
                    bottom: 86px;
                    right: 20px;
                    width: 285px;
                    background: rgba(15, 23, 42, 0.96);
                    border: 1.5px solid rgba(56, 189, 248, 0.4);
                    border-radius: 16px;
                    padding: 16px;
                    box-shadow: 0 20px 60px rgba(0,0,0,0.85);
                    color: white;
                    font-family: -apple-system, sans-serif;
                    z-index: 999999;
                    display: none;
                    flex-direction: column;
                    gap: 10px;
                }
                #chesslens-panel.open { display: flex; }
                .cl-row { display: flex; justify-content: space-between; align-items: center; }
                .cl-btn {
                    background: #38bdf8;
                    color: #0f172a;
                    border: none;
                    border-radius: 8px;
                    padding: 9px 12px;
                    font-weight: bold;
                    font-size: 13px;
                    cursor: pointer;
                    width: 100%;
                }
                .cl-tag {
                    font-size: 11px;
                    padding: 3px 6px;
                    border-radius: 4px;
                    font-weight: bold;
                }
                .cl-arrow-svg {
                    position: absolute;
                    top: 0; left: 0; width: 100%; height: 100%;
                    pointer-events: none;
                    z-index: 9999;
                }
                .cl-arrow-line {
                    stroke-width: 2.2;
                    stroke-linecap: round;
                }
            `;
            document.head.appendChild(style);

            // --- FLOATING BUBBLE & PANEL ---
            const bubble = document.createElement('div');
            bubble.id = 'chesslens-bubble';
            bubble.innerHTML = '♞';
            document.body.appendChild(bubble);

            const panel = document.createElement('div');
            panel.id = 'chesslens-panel';
            panel.innerHTML = `
                <div class="cl-row">
                    <span style="font-weight:bold; font-size:15px; color:#38bdf8;">ChessLens Multi-API</span>
                    <span id="cl-close" style="cursor:pointer; color:#94a3b8; font-size:16px;">✕</span>
                </div>
                <div id="cl-eval-text" style="font-size:13px; color:#e2e8f0;">Ready to analyze...</div>
                <div id="cl-moves-container" style="display:flex; flex-direction:column; gap:4px; max-height:120px; overflow-y:auto;"></div>
                <div style="font-size:11px; color:#94a3b8;">Simultaneous APIs Active:</div>
                <div style="display:flex; gap:6px; flex-wrap:wrap;">
                    <span class="cl-tag" style="background:#0284c7; color:white;">L1 d5</span>
                    <span class="cl-tag" style="background:#0d9488; color:white;">L2 d10</span>
                    <span class="cl-tag" style="background:#16a34a; color:white;">L3 d14</span>
                    <span class="cl-tag" style="background:#5aa4ff; color:#0f172a;">Lichess</span>
                    <span class="cl-tag" style="background:#a78bfa; color:#0f172a;">Stockfish</span>
                </div>
                <button class="cl-btn" id="cl-analyze-btn">⚡ Analyze Position Now</button>
            `;
            document.body.appendChild(panel);

            bubble.onclick = () => {
                panel.classList.toggle('open');
            };
            document.getElementById('cl-close').onclick = () => {
                panel.classList.remove('open');
            };

            // --- FEN SCRAPING & ARROWS ---
            const pieceMap = { 'wp': 'P', 'wn': 'N', 'wb': 'B', 'wr': 'R', 'wq': 'Q', 'wk': 'K', 'bp': 'p', 'bn': 'n', 'bb': 'b', 'br': 'r', 'bq': 'q', 'bk': 'k' };
            const colToNum = { 'a': 1, 'b': 2, 'c': 3, 'd': 4, 'e': 5, 'f': 6, 'g': 7, 'h': 8 };

            function getBoard() {
                return document.querySelector('wc-chess-board') || document.querySelector('.board') || document.querySelector('chess-board');
            }

            function getActiveFEN() {
                const pieces = document.querySelectorAll('.piece');
                if (pieces.length === 0) return null;
                let board = Array(8).fill(null).map(() => Array(8).fill(null));
                pieces.forEach(piece => {
                    const classes = Array.from(piece.classList);
                    const typeClass = classes.find(c => pieceMap[c]);
                    const squareClass = classes.find(c => c.startsWith('square-'));
                    if (typeClass && squareClass) {
                        const coords = squareClass.split('-')[1];
                        board[8 - parseInt(coords[1])][parseInt(coords[0]) - 1] = pieceMap[typeClass];
                    }
                });
                let fenRows = [];
                for (let r = 0; r < 8; r++) {
                    let empty = 0;
                    let rowStr = "";
                    for (let c = 0; c < 8; c++) {
                        if (board[r][c] === null) empty++;
                        else {
                            if (empty > 0) { rowStr += empty; empty = 0; }
                            rowStr += board[r][c];
                        }
                    }
                    if (empty > 0) rowStr += empty;
                    fenRows.push(rowStr);
                }
                const b = getBoard();
                const isFlipped = b ? b.classList.contains('flipped') : false;
                const side = isFlipped ? 'b' : 'w';
                return fenRows.join('/') + ' ' + side + ' - - 0 1';
            }

            function clearArrows() {
                document.querySelectorAll('.cl-arrow-svg').forEach(el => el.remove());
            }

            function drawArrow(moveUci, colorHex) {
                const board = getBoard();
                if (!board || !moveUci || moveUci.length < 4) return;
                const isFlipped = board.classList.contains('flipped');

                const getCenter = (sq) => {
                    const col = colToNum[sq[0]];
                    const row = parseInt(sq[1]);
                    if (!col || isNaN(row)) return null;
                    const size = 12.5, half = 6.25;
                    let x, y;
                    if (!isFlipped) { x = (col - 1) * size + half; y = (8 - row) * size + half; }
                    else { x = (8 - col) * size + half; y = (row - 1) * size + half; }
                    return { x, y };
                };

                const s = getCenter(moveUci.substring(0, 2));
                const e = getCenter(moveUci.substring(2, 4));
                if (!s || !e) return;

                const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
                svg.setAttribute("viewBox", "0 0 100 100");
                svg.classList.add('cl-arrow-svg');

                const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
                line.setAttribute("x1", s.x); line.setAttribute("y1", s.y);
                line.setAttribute("x2", e.x); line.setAttribute("y2", e.y);
                line.classList.add('cl-arrow-line');
                line.setAttribute('stroke', colorHex || '#38bdf8');

                const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
                circle.setAttribute("cx", e.x); circle.setAttribute("cy", e.y);
                circle.setAttribute("r", "2.6");
                circle.setAttribute("fill", colorHex || '#38bdf8');

                svg.appendChild(line);
                svg.appendChild(circle);
                board.appendChild(svg);
            }

            // --- RECEIVE SIMULTANEOUS MULTI-API RESULTS ---
            window.onChessLensResults = function(results) {
                clearArrows();
                const movesDiv = document.getElementById('cl-moves-container');
                const evalDiv = document.getElementById('cl-eval-text');
                if (!movesDiv) return;
                movesDiv.innerHTML = '';

                if (results.length > 0) {
                    const top = results[0];
                    evalDiv.innerText = top.san + ' (' + (top.eval > 0 ? '+' : '') + (top.eval / 100).toFixed(1) + ') - ' + top.explanation;
                }

                results.forEach(r => {
                    drawArrow(r.uci, r.color);
                    const item = document.createElement('div');
                    item.style.cssText = 'display:flex; justify-content:space-between; font-size:12px; padding:2px 0;';
                    item.innerHTML = '<span style="color:' + r.color + '; font-weight:bold;">● ' + r.level + ': ' + r.san + '</span><span style="color:#94a3b8;">' + (r.eval / 100).toFixed(1) + '</span>';
                    movesDiv.appendChild(item);
                });
            };

            function triggerAnalysis() {
                const fen = getActiveFEN();
                if (!fen) return;
                if (window.ChessLensAndroid) {
                    window.ChessLensAndroid.requestSimultaneousAnalysis(fen, 5, 10, 14, true, true);
                }
            }

            window.clTrigger = triggerAnalysis;
            document.getElementById('cl-analyze-btn').onclick = triggerAnalysis;

            // Auto-observe moves on the chess board
            const observer = new MutationObserver(() => {
                triggerAnalysis();
            });
            const board = getBoard();
            if (board) {
                observer.observe(board, { childList: true, subtree: true, attributes: true });
            }
        })();
    """.trimIndent()

    webView.evaluateJavascript(script, null)
}
