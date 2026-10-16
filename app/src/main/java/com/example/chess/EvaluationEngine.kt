package com.example.chess

import com.example.model.AnalysisLevel
import com.example.model.ChessMove
import com.example.model.ChessPiece
import com.example.model.ChessPosition
import com.example.model.ChessSquare
import com.example.model.MoveAnalysis
import com.example.model.MoveQuality
import com.example.model.PieceColor
import com.example.model.PieceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class EvaluationEngine(
    val moveGen: ChessMoveGenerator = ChessMoveGenerator(),
    val tacticalDetector: TacticalDetector = TacticalDetector(moveGen)
) {
    // Memory cache: FEN + levelId -> List<MoveAnalysis>
    private val memoryCache = ConcurrentHashMap<String, List<MoveAnalysis>>()

    // Piece-square tables for positional evaluation
    private val PAWN_TABLE = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 25, 25, 10,  5,  5,
        0,  0,  0, 20, 20,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-20,-20, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val KNIGHT_TABLE = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val BISHOP_TABLE = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  5,  0,  0,  5,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val ROOK_TABLE = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        0,  0,  0,  5,  5,  0,  0,  0
    )

    private val KING_MIDDLE_TABLE = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 30, 10,  0,  0, 10, 30, 20
    )

    suspend fun analyzePosition(
        position: ChessPosition,
        level: AnalysisLevel,
        detailLevel: DetailLevel = DetailLevel.INTERMEDIATE
    ): List<MoveAnalysis> = withContext(Dispatchers.Default) {
        val cacheKey = "${position.toFen()}_${level.id}_${level.depth}"
        memoryCache[cacheKey]?.let { return@withContext it }

        val legalMoves = moveGen.generateLegalMoves(position)
        if (legalMoves.isEmpty()) return@withContext emptyList()

        // Evaluate all candidate root moves
        val moveScores = mutableListOf<Pair<ChessMove, Int>>()
        val isWhite = position.activeColor == PieceColor.WHITE

        // MultiPV root search
        val searchDepth = max(1, min(level.depth, 16))
        for (move in legalMoves) {
            val after = moveGen.applyMove(position, move)
            // Minimax evaluation from opponent's perspective
            val score = -alphaBeta(
                pos = after,
                depth = searchDepth - 1,
                alpha = -30000,
                beta = 30000,
                maximizingColor = position.activeColor.opposite()
            )
            moveScores.add(Pair(move, score))
        }

        // Sort descending by score for active color
        moveScores.sortByDescending { it.second }

        val bestScore = moveScores.firstOrNull()?.second ?: 0
        val analyses = mutableListOf<MoveAnalysis>()

        // Take MultiPV lines
        val linesCount = min(level.multiPv, moveScores.size)
        for (i in 0 until linesCount) {
            val (move, score) = moveScores[i]
            val after = moveGen.applyMove(position, move)
            val evalDrop = bestScore - score

            val quality = classifyMoveQuality(evalDrop, move, position, after, score)
            val explanation = tacticalDetector.generateExplanation(
                before = position,
                move = move,
                after = after,
                evalCp = score,
                bestMoveEvalCp = bestScore,
                detailLevel = detailLevel
            )

            val (w, d, l) = calculateWDL(score)

            analyses.add(
                MoveAnalysis(
                    move = move,
                    evalCentipawns = score,
                    isMate = Math.abs(score) >= 9000,
                    mateInMoves = if (Math.abs(score) >= 9000) (10000 - Math.abs(score)) else 0,
                    quality = quality,
                    explanation = explanation,
                    levelName = level.name,
                    levelColorHex = level.colorHex,
                    pv = listOf(move),
                    depth = searchDepth,
                    nodesPerSec = 150000L,
                    winPercent = w,
                    drawPercent = d,
                    lossPercent = l
                )
            )
        }

        memoryCache[cacheKey] = analyses
        analyses
    }

    suspend fun findMovesToAvoid(
        position: ChessPosition,
        level: AnalysisLevel
    ): List<MoveAnalysis> = withContext(Dispatchers.Default) {
        val legalMoves = moveGen.generateLegalMoves(position)
        if (legalMoves.size < 2) return@withContext emptyList()

        val moveScores = mutableListOf<Pair<ChessMove, Int>>()
        for (move in legalMoves) {
            val after = moveGen.applyMove(position, move)
            val score = -alphaBeta(pos = after, depth = 2, alpha = -30000, beta = 30000, maximizingColor = position.activeColor.opposite())
            moveScores.add(Pair(move, score))
        }

        moveScores.sortByDescending { it.second }
        val bestScore = moveScores.first().second

        // Identify tempting moves (e.g. captures or checks or forward queen moves) with large eval drops (> 200cp)
        val avoidCandidates = moveScores.filter { (move, score) ->
            val drop = bestScore - score
            val isTempting = move.isCapture || move.san.contains("+") || move.from.row in 1..6
            drop >= 180 && isTempting
        }.take(3)

        avoidCandidates.map { (move, score) ->
            val after = moveGen.applyMove(position, move)
            val evalDrop = bestScore - score
            val avoidReason = tacticalDetector.generateAvoidReason(
                before = position,
                move = move,
                after = after,
                evalDrop = evalDrop
            )

            MoveAnalysis(
                move = move,
                evalCentipawns = score,
                quality = MoveQuality.BLUNDER,
                explanation = "Move to avoid: $avoidReason",
                avoidReason = avoidReason,
                levelName = "Avoid",
                levelColorHex = "#F87171",
                pv = listOf(move),
                depth = 3
            )
        }
    }

    private fun classifyMoveQuality(
        evalDrop: Int,
        move: ChessMove,
        before: ChessPosition,
        after: ChessPosition,
        finalScore: Int
    ): MoveQuality {
        // Brilliant check: sacrificed piece of value (e.g. Queen, Rook, Bishop) that leads to clear advantage
        val movedPiece = before.getPiece(move.from)
        val captured = before.getPiece(move.to)
        if (movedPiece != null && movedPiece.type.value >= 320 && captured == null) {
            val isHanging = moveGen.isSquareAttackedBy(after, move.to, before.activeColor.opposite())
            if (isHanging && finalScore >= 200 && evalDrop <= 10) {
                return MoveQuality.BRILLIANT
            }
        }

        return when {
            evalDrop <= 15 -> MoveQuality.BEST
            evalDrop <= 45 -> MoveQuality.GOOD
            evalDrop <= 100 -> MoveQuality.INACCURACY
            evalDrop <= 250 -> MoveQuality.MISTAKE
            else -> MoveQuality.BLUNDER
        }
    }

    private fun alphaBeta(
        pos: ChessPosition,
        depth: Int,
        alpha: Int,
        beta: Int,
        maximizingColor: PieceColor
    ): Int {
        if (depth <= 0) {
            return quiescence(pos, alpha, beta, maximizingColor)
        }

        val legalMoves = moveGen.generateLegalMoves(pos)
        if (legalMoves.isEmpty()) {
            return if (moveGen.isKingInCheck(pos, pos.activeColor)) {
                // Checkmate (prefer quicker mate)
                -10000 - depth
            } else {
                0 // Stalemate
            }
        }

        var currentAlpha = alpha
        // Move ordering: captures first
        val sortedMoves = legalMoves.sortedByDescending { if (it.isCapture) 10 else 0 }

        for (move in sortedMoves) {
            val next = moveGen.applyMove(pos, move)
            val score = -alphaBeta(next, depth - 1, -beta, -currentAlpha, maximizingColor.opposite())
            if (score >= beta) return beta // Beta cutoff
            if (score > currentAlpha) currentAlpha = score
        }

        return currentAlpha
    }

    private fun quiescence(
        pos: ChessPosition,
        alpha: Int,
        beta: Int,
        maximizingColor: PieceColor
    ): Int {
        val standPat = staticEvaluation(pos, maximizingColor)
        if (standPat >= beta) return beta
        var currentAlpha = max(alpha, standPat)

        // Only search captures in quiescence
        val captures = moveGen.generateLegalMoves(pos).filter { it.isCapture }
        for (capture in captures) {
            val next = moveGen.applyMove(pos, capture)
            val score = -quiescence(next, -beta, -currentAlpha, maximizingColor.opposite())
            if (score >= beta) return beta
            if (score > currentAlpha) currentAlpha = score
        }

        return currentAlpha
    }

    fun staticEvaluation(pos: ChessPosition, perspective: PieceColor): Int {
        var whiteScore = 0
        var blackScore = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = pos.getPiece(r, c) ?: continue
                val idx = r * 8 + c
                val flippedIdx = (7 - r) * 8 + c

                val valMat = piece.type.value
                val valPst = when (piece.type) {
                    PieceType.PAWN -> if (piece.color == PieceColor.WHITE) PAWN_TABLE[idx] else PAWN_TABLE[flippedIdx]
                    PieceType.KNIGHT -> if (piece.color == PieceColor.WHITE) KNIGHT_TABLE[idx] else KNIGHT_TABLE[flippedIdx]
                    PieceType.BISHOP -> if (piece.color == PieceColor.WHITE) BISHOP_TABLE[idx] else BISHOP_TABLE[flippedIdx]
                    PieceType.ROOK -> if (piece.color == PieceColor.WHITE) ROOK_TABLE[idx] else ROOK_TABLE[flippedIdx]
                    PieceType.QUEEN -> 0
                    PieceType.KING -> if (piece.color == PieceColor.WHITE) KING_MIDDLE_TABLE[idx] else KING_MIDDLE_TABLE[flippedIdx]
                }

                if (piece.color == PieceColor.WHITE) {
                    whiteScore += (valMat + valPst)
                } else {
                    blackScore += (valMat + valPst)
                }
            }
        }

        val netWhite = whiteScore - blackScore
        return if (perspective == PieceColor.WHITE) netWhite else -netWhite
    }

    private fun calculateWDL(evalCp: Int): Triple<Int, Int, Int> {
        val winP = 1.0 / (1.0 + exp(-0.00368208 * evalCp))
        val lossP = 1.0 / (1.0 + exp(0.00368208 * evalCp))
        val drawP = max(0.0, 1.0 - winP - lossP)

        val total = winP + drawP + lossP
        val w = ((winP / total) * 100).toInt()
        val d = ((drawP / total) * 100).toInt()
        val l = max(0, 100 - w - d)
        return Triple(w, d, l)
    }

    fun clearCache() {
        memoryCache.clear()
    }
}
