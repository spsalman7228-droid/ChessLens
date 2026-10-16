package com.example

import com.example.chess.ChessMoveGenerator
import com.example.chess.DetailLevel
import com.example.chess.EvaluationEngine
import com.example.chess.TacticalDetector
import com.example.model.ChessPosition
import com.example.model.ChessSquare
import com.example.model.PieceColor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val moveGen = ChessMoveGenerator()
    private val tacticalDetector = TacticalDetector(moveGen)
    private val engine = EvaluationEngine(moveGen, tacticalDetector)

    @Test
    fun testInitialPositionLegalMoves() {
        val initial = ChessPosition.initial()
        val legalMoves = moveGen.generateLegalMoves(initial)
        // Standard chess starting position has exactly 20 legal moves (16 pawn pushes + 4 knight moves)
        assertEquals(20, legalMoves.size)
        assertEquals(PieceColor.WHITE, initial.activeColor)
    }

    @Test
    fun testFenRoundTrip() {
        val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val pos = ChessPosition.fromFen(fen)
        assertNotNull(pos)
        assertEquals(PieceColor.BLACK, pos!!.activeColor)
        assertEquals("e3", pos.enPassantTarget?.algebraic)
        assertEquals(fen, pos.toFen())
    }

    @Test
    fun testMoveExecution() {
        val initial = ChessPosition.initial()
        val legalMoves = moveGen.generateLegalMoves(initial)
        val e2e4 = legalMoves.find { it.from.algebraic == "e2" && it.to.algebraic == "e4" }
        assertNotNull(e2e4)

        val nextPos = moveGen.applyMove(initial, e2e4!!)
        assertEquals(PieceColor.BLACK, nextPos.activeColor)
        assertEquals("e3", nextPos.enPassantTarget?.algebraic)
        assertNotNull(nextPos.getPiece(ChessSquare.fromAlgebraic("e4")!!))
    }

    @Test
    fun testPositionValidation() {
        // Valid position
        val validPos = ChessPosition.initial()
        assertTrue(validPos.validate().isValid)

        // Invalid position missing white king
        val missingKingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1"
        val invalidPos = ChessPosition.fromFen(missingKingFen)
        assertNotNull(invalidPos)
        assertFalse(invalidPos!!.validate().isValid)
    }

    @Test
    fun testTacticalExplanationGeneration() {
        val pos = ChessPosition.initial()
        val legalMoves = moveGen.generateLegalMoves(pos)
        val e4 = legalMoves.find { it.to.algebraic == "e4" }
        assertNotNull(e4)
        val after = moveGen.applyMove(pos, e4!!)

        val explanation = tacticalDetector.generateExplanation(
            before = pos,
            move = e4,
            after = after,
            evalCp = 30,
            bestMoveEvalCp = 30,
            detailLevel = DetailLevel.BEGINNER
        )
        assertTrue(explanation.contains("center", ignoreCase = true) || explanation.contains("move", ignoreCase = true))
    }
}
