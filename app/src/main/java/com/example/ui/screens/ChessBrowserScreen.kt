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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessBrowserScreen(
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val app = ChessLensApplication.instance
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageLoadingProgress by remember { mutableFloatStateOf(0f) }
    var isPageLoading by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate900,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "chess.com (ChessLens Extension Active)",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                actions = {
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
                    androidx.compose.material3.LinearProgressIndicator(
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
            // Restrict browser only to chess.com
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
            if (depthD5 > 0) levels.add(AnalysisLevel("d5", "d$depthD5", "#38BDF8", depthD5))
            if (depthD10 > 0) levels.add(AnalysisLevel("d10", "d$depthD10", "#2DD4BF", depthD10))
            if (depthD15 > 0) levels.add(AnalysisLevel("d15", "d$depthD15", "#4ADE80", depthD15))

            // Run multiple APIs simultaneously in parallel!
            val results = app.evaluationEngine.analyzeMultipleLevelsSimultaneously(
                position = position,
                levels = levels,
                webSource = app.webAnalysisSource,
                includeWebApis = useLichess || useStockfishOnline,
                onProgress = { currentResults ->
                    // Stream results back to webview immediately as shallow levels complete!
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
                    background: rgba(15, 23, 42, 0.9);
                    border: 2px solid #38bdf8;
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
                    bottom: 84px;
                    right: 20px;
                    width: 280px;
                    background: rgba(15, 23, 42, 0.95);
                    border: 1px solid rgba(255,255,255,0.12);
                    border-radius: 16px;
                    padding: 16px;
                    box-shadow: 0 20px 60px rgba(0,0,0,0.8);
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
                    padding: 8px 12px;
                    font-weight: bold;
                    font-size: 13px;
                    cursor: pointer;
                    width: 100%;
                }
                .cl-tag {
                    font-size: 11px;
                    padding: 2px 6px;
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
                    stroke-width: 1.8;
                    stroke-dasharray: 6 4;
                    stroke-linecap: round;
                    animation: cldash 1s linear infinite;
                }
                @keyframes cldash { to { stroke-dashoffset: -10; } }
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
                <button class="cl-btn" id="cl-analyze-btn">Analyze Position Now</button>
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
                circle.setAttribute("r", "2.2");
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
