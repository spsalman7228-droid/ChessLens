package com.example.ui.screens

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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ChessLensApplication
import com.example.model.ChessMove
import com.example.model.ChessPosition
import com.example.model.ChessSquare
import com.example.model.MoveAnalysis
import com.example.model.PieceColor
import com.example.ui.components.ChessBoardView
import com.example.ui.theme.ChessLensCyan
import com.example.ui.theme.ChessLensGreen
import com.example.ui.theme.ChessLensRed
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

data class PuzzleItem(
    val id: String,
    val title: String,
    val fen: String,
    val targetMoveUci: String,
    val targetMoveSan: String,
    val hint: String,
    val solutionExplanation: String,
    val difficulty: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzlesScreen(
    onNavigateBack: () -> Unit
) {
    val app = ChessLensApplication.instance
    val puzzles = remember {
        listOf(
            PuzzleItem(
                id = "p1",
                title = "Smothered Threat",
                fen = "r1b2rk1/pp3ppp/8/3p4/4n3/2P2N2/PP1NBPPP/R2Q1RK1 b - - 0 12",
                targetMoveUci = "e4d2",
                targetMoveSan = "Nxd2",
                hint = "Look for an exchange that removes White's defensive piece on d2.",
                solutionExplanation = "Nxd2 eliminates White's defender and initiates a tactical clearance.",
                difficulty = "Intermediate"
            ),
            PuzzleItem(
                id = "p2",
                title = "Back-Rank Tactical Pin",
                fen = "6k1/5ppp/8/8/8/8/1r3PPP/4R1K1 w - - 0 1",
                targetMoveUci = "e1e8",
                targetMoveSan = "Re8#",
                hint = "Notice Black's king trapped by its own pawns on the 8th rank.",
                solutionExplanation = "Re8# delivers a classic back-rank checkmate, as the king has no escape square.",
                difficulty = "Beginner"
            ),
            PuzzleItem(
                id = "p3",
                title = "Knight Fork on King & Queen",
                fen = "r1bqk2r/pppp1ppp/2n5/4p3/2B1n3/2NP1N2/PPP2PPP/R1BQK2R b KQkq - 0 6",
                targetMoveUci = "e4c3",
                targetMoveSan = "Nxc3",
                hint = "Trade the centralized knight with tempo and damage White's pawn structure.",
                solutionExplanation = "Nxc3 doubles White's pawns on the c-file and wins a tempo.",
                difficulty = "Advanced"
            )
        )
    }

    var puzzleIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var isRevealed by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var selectedSquare by remember { mutableStateOf<ChessSquare?>(null) }
    var puzzleStateMessage by remember { mutableStateOf<String?>(null) }

    val currentPuzzle = puzzles[puzzleIndex]
    var position by remember(currentPuzzle) {
        mutableStateOf(ChessPosition.fromFen(currentPuzzle.fen) ?: ChessPosition.initial())
    }

    val legalMoves = remember(position) { app.moveGenerator.generateLegalMoves(position) }
    val movesFromSelected = if (selectedSquare != null) {
        legalMoves.filter { it.from == selectedSquare }
    } else emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tactical Puzzles", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate900,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Score: $score", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
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
            // Puzzle Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = currentPuzzle.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "${if (position.activeColor == PieceColor.WHITE) "White" else "Black"} to play • ${currentPuzzle.difficulty}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900
                ) {
                    Text(
                        text = "Puzzle ${puzzleIndex + 1}/${puzzles.size}",
                        fontSize = 12.sp,
                        color = ChessLensCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Board View
            ChessBoardView(
                position = position,
                selectedSquare = selectedSquare,
                legalMovesFromSelected = movesFromSelected,
                isFlipped = position.activeColor == PieceColor.BLACK,
                analyses = if (isRevealed) {
                    val from = ChessSquare.fromAlgebraic(currentPuzzle.targetMoveUci.substring(0, 2))
                    val to = ChessSquare.fromAlgebraic(currentPuzzle.targetMoveUci.substring(2, 4))
                    if (from != null && to != null) {
                        listOf(
                            MoveAnalysis(
                                move = ChessMove(from, to, san = currentPuzzle.targetMoveSan),
                                evalCentipawns = 350,
                                levelName = "Solution",
                                levelColorHex = "#4ADE80",
                                explanation = currentPuzzle.solutionExplanation
                            )
                        )
                    } else emptyList()
                } else emptyList(),
                onSquareClicked = { clickedSquare ->
                    if (isRevealed) return@ChessBoardView
                    if (selectedSquare == null) {
                        val piece = position.getPiece(clickedSquare)
                        if (piece != null && piece.color == position.activeColor) {
                            selectedSquare = clickedSquare
                        }
                    } else {
                        val candidate = legalMoves.find { it.from == selectedSquare && it.to == clickedSquare }
                        if (candidate != null) {
                            if (candidate.uci == currentPuzzle.targetMoveUci) {
                                // Correct!
                                puzzleStateMessage = "🎉 Correct! ${currentPuzzle.targetMoveSan} is the best move."
                                score += 10
                                isRevealed = true
                                position = app.moveGenerator.applyMove(position, candidate)
                            } else {
                                puzzleStateMessage = "❌ Not quite right. Try again or tap Hint!"
                            }
                            selectedSquare = null
                        } else {
                            selectedSquare = null
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Puzzle State Message (Feedback)
            puzzleStateMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (msg.contains("Correct")) ChessLensGreen.copy(alpha = 0.15f) else ChessLensRed.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (msg.contains("Correct")) ChessLensGreen else ChessLensRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Hint Card
            if (showHint && !isRevealed) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = currentPuzzle.hint, fontSize = 13.sp, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Solution explanation
            if (isRevealed) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Solution: ${currentPuzzle.targetMoveSan}", fontWeight = FontWeight.Bold, color = ChessLensGreen, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = currentPuzzle.solutionExplanation, fontSize = 13.sp, color = Color.White, lineHeight = 19.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isRevealed) {
                    OutlinedButton(
                        onClick = { showHint = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hint", color = Color.White)
                    }
                    Button(
                        onClick = { isRevealed = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = ChessLensCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reveal", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = {
                            if (puzzleIndex < puzzles.size - 1) {
                                puzzleIndex++
                            } else {
                                puzzleIndex = 0
                            }
                            isRevealed = false
                            showHint = false
                            puzzleStateMessage = null
                            selectedSquare = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ChessLensCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Next Puzzle", color = Slate900, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.NavigateNext, contentDescription = null, tint = Slate900)
                    }
                }
            }
        }
    }
}
