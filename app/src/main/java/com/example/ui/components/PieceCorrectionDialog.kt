package com.example.ui.components

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChessPiece
import com.example.model.ChessPosition
import com.example.model.ChessSquare
import com.example.model.PieceColor
import com.example.model.PieceType
import com.example.ui.theme.ChessLensCyan
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun PieceCorrectionDialog(
    initialPosition: ChessPosition,
    onDismiss: () -> Unit,
    onApply: (ChessPosition) -> Unit
) {
    var editedPosition by remember { mutableStateOf(initialPosition.clone()) }
    var selectedSquare by remember { mutableStateOf<ChessSquare?>(null) }
    var selectedPalettePiece by remember { mutableStateOf<ChessPiece?>(null) }
    val validation = editedPosition.validate()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fix Position", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tap a square to select it, then tap a piece from the palette to place or clear.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mini board for editing
                ChessBoardView(
                    position = editedPosition,
                    selectedSquare = selectedSquare,
                    onSquareClicked = { clickedSquare ->
                        if (selectedPalettePiece != null) {
                            // Place selected piece directly
                            val updated = editedPosition.clone()
                            updated.setPiece(clickedSquare, selectedPalettePiece)
                            editedPosition = updated
                        } else {
                            selectedSquare = clickedSquare
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Side to move toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Side to Move:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    FilterChip(
                        selected = editedPosition.activeColor == PieceColor.WHITE,
                        onClick = {
                            val u = editedPosition.clone()
                            u.activeColor = PieceColor.WHITE
                            editedPosition = u
                        },
                        label = { Text("White") }
                    )
                    FilterChip(
                        selected = editedPosition.activeColor == PieceColor.BLACK,
                        onClick = {
                            val u = editedPosition.clone()
                            u.activeColor = PieceColor.BLACK
                            editedPosition = u
                        },
                        label = { Text("Black") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Piece Palette
                Text("Piece Palette:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))

                // White pieces
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val whitePieces = listOf(
                        ChessPiece(PieceType.KING, PieceColor.WHITE),
                        ChessPiece(PieceType.QUEEN, PieceColor.WHITE),
                        ChessPiece(PieceType.ROOK, PieceColor.WHITE),
                        ChessPiece(PieceType.BISHOP, PieceColor.WHITE),
                        ChessPiece(PieceType.KNIGHT, PieceColor.WHITE),
                        ChessPiece(PieceType.PAWN, PieceColor.WHITE)
                    )
                    whitePieces.forEach { p ->
                        PaletteButton(
                            piece = p,
                            isSelected = selectedPalettePiece == p,
                            onClick = {
                                selectedPalettePiece = if (selectedPalettePiece == p) null else p
                                selectedSquare?.let { sq ->
                                    val u = editedPosition.clone()
                                    u.setPiece(sq, selectedPalettePiece)
                                    editedPosition = u
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Black pieces & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val blackPieces = listOf(
                        ChessPiece(PieceType.KING, PieceColor.BLACK),
                        ChessPiece(PieceType.QUEEN, PieceColor.BLACK),
                        ChessPiece(PieceType.ROOK, PieceColor.BLACK),
                        ChessPiece(PieceType.BISHOP, PieceColor.BLACK),
                        ChessPiece(PieceType.KNIGHT, PieceColor.BLACK),
                        ChessPiece(PieceType.PAWN, PieceColor.BLACK)
                    )
                    blackPieces.forEach { p ->
                        PaletteButton(
                            piece = p,
                            isSelected = selectedPalettePiece == p,
                            onClick = {
                                selectedPalettePiece = if (selectedPalettePiece == p) null else p
                                selectedSquare?.let { sq ->
                                    val u = editedPosition.clone()
                                    u.setPiece(sq, selectedPalettePiece)
                                    editedPosition = u
                                }
                            }
                        )
                    }

                    // Clear square button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selectedPalettePiece == null && selectedSquare != null) ChessLensCyan.copy(alpha = 0.2f) else Slate800)
                            .clickable {
                                selectedSquare?.let { sq ->
                                    val u = editedPosition.clone()
                                    u.setPiece(sq, null)
                                    editedPosition = u
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Square", tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                    }
                }

                // Legality warning
                if (!validation.isValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠ ${validation.errors.firstOrNull() ?: "Invalid position"}",
                        color = Color(0xFFF87171),
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(editedPosition) },
                enabled = validation.isValid,
                colors = ButtonDefaults.buttonColors(containerColor = ChessLensCyan)
            ) {
                Text("Apply Position", color = Slate900, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PaletteButton(
    piece: ChessPiece,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) ChessLensCyan.copy(alpha = 0.35f) else Slate800)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ChessLensCyan else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = piece.symbol,
            fontSize = 22.sp,
            color = if (piece.color == PieceColor.WHITE) Color.White else Color(0xFF94A3B8)
        )
    }
}
