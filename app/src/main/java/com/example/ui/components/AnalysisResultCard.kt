package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChessMove
import com.example.model.ChessPosition
import com.example.model.MoveAnalysis
import com.example.model.MoveQuality
import com.example.ui.theme.ChessLensCyan
import com.example.ui.theme.ChessLensRed
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun AnalysisResultCard(
    position: ChessPosition,
    analyses: List<MoveAnalysis>,
    avoidMoves: List<MoveAnalysis>,
    isThinkFirst: Boolean,
    isRevealed: Boolean,
    userGuessedMove: ChessMove? = null,
    onRevealClicked: () -> Unit,
    onFixPositionClicked: () -> Unit,
    onSaveToHistory: () -> Unit,
    onShareClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedFeedback by remember { mutableStateOf<String?>(null) }
    val topAnalysis = analyses.firstOrNull()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Slate800))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Eval & Quality badge & Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Eval pill
                    if (isRevealed || !isThinkFirst) {
                        val evalText = if (topAnalysis != null) {
                            if (topAnalysis.isMate) "M${topAnalysis.mateInMoves}" else "${if (topAnalysis.evalCentipawns > 0) "+" else ""}%.1f".format(topAnalysis.evalCentipawns / 100f)
                        } else "0.0"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate800,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Eval $evalText",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChessLensCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        topAnalysis?.quality?.let { quality ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(android.graphics.Color.parseColor(quality.badgeColorHex)).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(android.graphics.Color.parseColor(quality.badgeColorHex)))
                            ) {
                                Text(
                                    text = quality.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(android.graphics.Color.parseColor(quality.badgeColorHex)),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate800
                        ) {
                            Text(
                                text = "🧠 Think First Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFBBF24),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Action buttons: Fix position, Copy FEN, Save
                Row {
                    IconButton(onClick = onFixPositionClicked) {
                        Icon(Icons.Default.Edit, contentDescription = "Fix position", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(position.toFen()))
                            copiedFeedback = "FEN Copied!"
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy FEN", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onSaveToHistory) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Save Position", tint = ChessLensCyan, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onShareClicked) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
            }

            copiedFeedback?.let { msg ->
                Text(
                    text = msg,
                    color = ChessLensCyan,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Think-First Guess card
            if (isThinkFirst && !isRevealed) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate800)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (userGuessedMove != null) "Your guess: ${userGuessedMove.san}" else "Tap two squares on the board to guess the move first!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRevealClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = ChessLensCyan)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Slate900, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reveal Analysis", color = Slate900, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Revealed Analysis lines
                topAnalysis?.let { analysis ->
                    Text(
                        text = "Best Move: ${analysis.move.san}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = analysis.explanation,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Multi-level lines
                    Text("Top Lines:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(4.dp))
                    analyses.take(3).forEach { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(line.levelColorHex)))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${line.levelName}: ${line.move.san}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = if (line.isMate) "M${line.mateInMoves}" else "${if (line.evalCentipawns > 0) "+" else ""}%.1f".format(line.evalCentipawns / 100f),
                                fontSize = 12.sp,
                                color = ChessLensCyan
                            )
                        }
                    }
                }

                // Moves to avoid section
                if (avoidMoves.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "⚠ Moves to Avoid:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChessLensRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    avoidMoves.forEach { avoid ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ChessLensRed.copy(alpha = 0.1f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Avoid ${avoid.move.san}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChessLensRed
                            )
                            Text(
                                text = avoid.avoidReason ?: avoid.explanation,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
