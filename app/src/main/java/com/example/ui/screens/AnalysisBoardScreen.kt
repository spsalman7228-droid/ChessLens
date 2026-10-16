package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.ChessLensApplication
import com.example.model.AnalysisLevel
import com.example.model.AnalysisSourceType
import com.example.model.ChessMove
import com.example.model.ChessPosition
import com.example.model.ChessSquare
import com.example.model.MoveAnalysis
import com.example.model.PieceColor
import com.example.ui.components.AnalysisResultCard
import com.example.ui.components.ChessBoardView
import com.example.ui.components.EvaluationBar
import com.example.ui.components.PieceCorrectionDialog
import com.example.ui.theme.ChessLensCyan
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisBoardScreen(
    initialFen: String?,
    onNavigateBack: () -> Unit
) {
    val app = ChessLensApplication.instance
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var position by remember {
        mutableStateOf(if (initialFen != null) ChessPosition.fromFen(initialFen) ?: ChessPosition.initial() else ChessPosition.initial())
    }
    var isFlipped by remember { mutableStateOf(false) }
    var selectedSquare by remember { mutableStateOf<ChessSquare?>(null) }
    var lastMove by remember { mutableStateOf<ChessMove?>(null) }
    val moveHistory = remember { mutableStateListOf<Pair<ChessPosition, ChessMove>>() }

    var analyses by remember { mutableStateOf<List<MoveAnalysis>>(emptyList()) }
    var avoidMoves by remember { mutableStateOf<List<MoveAnalysis>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Think-First mode
    val thinkFirstSetting by app.preferencesManager.thinkFirst.collectAsState(initial = false)
    var isRevealed by remember { mutableStateOf(!thinkFirstSetting) }
    var userGuessedMove by remember { mutableStateOf<ChessMove?>(null) }

    var showFixDialog by remember { mutableStateOf(false) }

    // Active Levels
    val activeLevels = listOf(
        AnalysisLevel("l1", "Level 1", "#38BDF8", depth = 5),
        AnalysisLevel("l2", "Level 2", "#2DD4BF", depth = 10),
        AnalysisLevel("l3", "Level 3", "#4ADE80", depth = 14)
    )

    // Trigger analysis whenever position changes
    fun runEngineAnalysis() {
        scope.launch {
            isAnalyzing = true
            try {
                val combinedAnalyses = mutableListOf<MoveAnalysis>()
                for (lvl in activeLevels) {
                    val res = app.evaluationEngine.analyzePosition(position, lvl)
                    combinedAnalyses.addAll(res)
                }
                analyses = combinedAnalyses

                val avoids = app.evaluationEngine.findMovesToAvoid(position, activeLevels.first())
                avoidMoves = avoids
            } catch (e: Exception) {
                // Ignore transient errors
            } finally {
                isAnalyzing = false
            }
        }
    }

    LaunchedEffect(position) {
        userGuessedMove = null
        if (!thinkFirstSetting) {
            isRevealed = true
        }
        runEngineAnalysis()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis Studio", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { isFlipped = !isFlipped }) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip Board", tint = Color.White)
                    }
                    IconButton(onClick = {
                        position = ChessPosition.initial()
                        moveHistory.clear()
                        lastMove = null
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Position", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        containerColor = Slate950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Board and Evaluation Bar Row
            val topEval = analyses.firstOrNull()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EvaluationBar(
                    evalCentipawns = topEval?.evalCentipawns ?: 0,
                    isMate = topEval?.isMate ?: false,
                    mateInMoves = topEval?.mateInMoves ?: 0,
                    isFlipped = isFlipped,
                    modifier = Modifier.height(310.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                val legalMoves = remember(position) { app.moveGenerator.generateLegalMoves(position) }
                val movesFromSelected = if (selectedSquare != null) {
                    legalMoves.filter { it.from == selectedSquare }
                } else emptyList()

                ChessBoardView(
                    position = position,
                    isFlipped = isFlipped,
                    selectedSquare = selectedSquare,
                    legalMovesFromSelected = movesFromSelected,
                    lastMove = lastMove,
                    analyses = if (isRevealed) analyses else emptyList(),
                    avoidMoves = if (isRevealed) avoidMoves else emptyList(),
                    onSquareClicked = { clickedSquare ->
                        if (selectedSquare == null) {
                            val piece = position.getPiece(clickedSquare)
                            if (piece != null && piece.color == position.activeColor) {
                                selectedSquare = clickedSquare
                            }
                        } else {
                            val candidateMove = legalMoves.find { it.from == selectedSquare && it.to == clickedSquare }
                            if (candidateMove != null) {
                                // If in think-first mode before reveal, record user's guess
                                if (thinkFirstSetting && !isRevealed) {
                                    userGuessedMove = candidateMove
                                }
                                val nextPos = app.moveGenerator.applyMove(position, candidateMove)
                                moveHistory.add(Pair(position, candidateMove))
                                lastMove = candidateMove
                                position = nextPos
                                selectedSquare = null
                            } else {
                                val piece = position.getPiece(clickedSquare)
                                if (piece != null && piece.color == position.activeColor) {
                                    selectedSquare = clickedSquare
                                } else {
                                    selectedSquare = null
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation bar: Back 1 move, Forward, Move list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (moveHistory.isNotEmpty()) {
                            val last = moveHistory.removeLast()
                            position = last.first
                            lastMove = moveHistory.lastOrNull()?.second
                        }
                    },
                    enabled = moveHistory.isNotEmpty()
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Undo Move", tint = if (moveHistory.isNotEmpty()) Color.White else Slate800)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900
                ) {
                    Text(
                        text = if (position.activeColor == PieceColor.WHITE) "White to move" else "Black to move",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ChessLensCyan)
                } else {
                    Spacer(modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Result Card
            AnalysisResultCard(
                position = position,
                analyses = analyses,
                avoidMoves = avoidMoves,
                isThinkFirst = thinkFirstSetting,
                isRevealed = isRevealed,
                userGuessedMove = userGuessedMove,
                onRevealClicked = { isRevealed = true },
                onFixPositionClicked = { showFixDialog = true },
                onSaveToHistory = {
                    val top = analyses.firstOrNull()
                    if (top != null) {
                        scope.launch {
                            app.historyRepository.saveAnalysis(
                                fen = position.toFen(),
                                bestMoveSan = top.move.san,
                                bestMoveUci = top.move.uci,
                                evalCp = top.evalCentipawns,
                                explanation = top.explanation,
                                sourceTag = "Analysis Studio"
                            )
                            Toast.makeText(context, "Saved to History!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onShareClicked = {
                    val shareIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, "ChessLens Analysis:\nFEN: ${position.toFen()}\nBest move: ${analyses.firstOrNull()?.move?.san}\nEval: ${analyses.firstOrNull()?.evalCentipawns?.div(100f)}")
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Analysis"))
                }
            )
        }
    }

    if (showFixDialog) {
        PieceCorrectionDialog(
            initialPosition = position,
            onDismiss = { showFixDialog = false },
            onApply = { corrected ->
                position = corrected
                showFixDialog = false
            }
        )
    }
}
