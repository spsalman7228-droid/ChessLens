package com.example.chess

import com.example.model.ChessMove
import com.example.model.ChessPiece
import com.example.model.ChessPosition
import com.example.model.ChessSquare
import com.example.model.MoveQuality
import com.example.model.PieceColor
import com.example.model.PieceType

enum class DetailLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

class TacticalDetector(private val moveGen: ChessMoveGenerator) {

    fun generateExplanation(
        before: ChessPosition,
        move: ChessMove,
        after: ChessPosition,
        evalCp: Int,
        bestMoveEvalCp: Int,
        detailLevel: DetailLevel = DetailLevel.INTERMEDIATE
    ): String {
        val movingPiece = before.getPiece(move.from) ?: return "Solid developing move."
        val capturedPiece = before.getPiece(move.to)
        val isWhite = before.activeColor == PieceColor.WHITE
        val isMate = Math.abs(evalCp) >= 9000

        if (isMate) {
            val movesToMate = (10000 - Math.abs(evalCp))
            val isWinning = (isWhite && evalCp > 0) || (!isWhite && evalCp < 0)
            return if (isWinning) {
                if (movesToMate <= 1) "Delivers immediate checkmate!" else "Forces checkmate in $movesToMate moves."
            } else {
                "Facing forced checkmate in $movesToMate moves."
            }
        }

        // Check if capture wins material
        if (capturedPiece != null) {
            val gain = capturedPiece.type.value - movingPiece.type.value
            if (gain > 0) {
                return when (detailLevel) {
                    DetailLevel.BEGINNER -> "Wins a ${capturedPiece.type.name.lowercase()} with favorable material trade!"
                    DetailLevel.INTERMEDIATE -> "Wins an exchange (${capturedPiece.type.name.lowercase()} for ${movingPiece.type.name.lowercase()})."
                    DetailLevel.ADVANCED -> "Tactical liquidation winning +${gain / 100}.${(gain % 100) / 10} material."
                }
            } else if (gain == 0) {
                return "Trades ${movingPiece.type.name.lowercase()}s to simplify the position."
            } else {
                // Sacrificing or capturing undefended piece?
                val isTargetAttacked = moveGen.isSquareAttackedBy(after, move.to, before.activeColor.opposite())
                if (!isTargetAttacked) {
                    return "Captures free ${capturedPiece.type.name.lowercase()} on ${move.to.algebraic}."
                }
            }
        }

        // Fork detection (attacks 2 or more enemy pieces of value)
        val forkedPieces = findForkedPieces(after, move.to, before.activeColor)
        if (forkedPieces.size >= 2) {
            val targets = forkedPieces.joinToString(" and ") { it.type.name.lowercase() }
            return "Creates a lethal fork on $targets!"
        }

        // Castling
        if (move.isCastling) {
            return when (detailLevel) {
                DetailLevel.BEGINNER -> "Safeguards the king and activates the rook."
                DetailLevel.INTERMEDIATE -> "Castles for king safety and rook connection on the back rank."
                DetailLevel.ADVANCED -> "Completes king security; transitions into middlegame coordination."
            }
        }

        // Defending a hanging piece
        val hangingSquare = findHangingPieceDefended(before, after, before.activeColor)
        if (hangingSquare != null) {
            val defendedPiece = after.getPiece(hangingSquare)
            if (defendedPiece != null) {
                return "Defends the threatened ${defendedPiece.type.name.lowercase()} on ${hangingSquare.algebraic}."
            }
        }

        // Center control
        val centerSquares = listOf(ChessSquare(3, 3), ChessSquare(3, 4), ChessSquare(4, 3), ChessSquare(4, 4))
        if (move.to in centerSquares) {
            return when (detailLevel) {
                DetailLevel.BEGINNER -> "Places a piece directly into the center for maximum board control."
                DetailLevel.INTERMEDIATE -> "Controls critical central outposts (d4/d5/e4/e5)."
                DetailLevel.ADVANCED -> "Establishes central spatial dominance, increasing piece mobility."
            }
        }

        // Development
        if ((movingPiece.type == PieceType.KNIGHT || movingPiece.type == PieceType.BISHOP) && before.fullmoveNumber <= 10) {
            return "Develops the ${movingPiece.type.name.lowercase()} to active diagonals/squares."
        }

        // Pin / Skewer / Pressure
        val isOpponentInCheck = moveGen.isKingInCheck(after, before.activeColor.opposite())
        if (isOpponentInCheck) {
            return "Delivers a check, seizing initiative and forcing king reaction."
        }

        return "Positional move: enhances square control and improves coordination."
    }

    fun generateAvoidReason(
        before: ChessPosition,
        move: ChessMove,
        after: ChessPosition,
        evalDrop: Int
    ): String {
        val movingPiece = before.getPiece(move.from) ?: return "Weak positional concession."
        val capturedPiece = before.getPiece(move.to)

        // Check if moving piece lands on an attacked square undefended
        val isSquareAttacked = moveGen.isSquareAttackedBy(after, move.to, before.activeColor.opposite())
        val isDefended = moveGen.isSquareAttackedBy(after, move.to, before.activeColor)

        if (isSquareAttacked && !isDefended) {
            return "Hangs the ${movingPiece.type.name.lowercase()} on ${move.to.algebraic} for free."
        }

        if (capturedPiece != null && isSquareAttacked) {
            return "Fails tactically: trading ${movingPiece.type.name.lowercase()} leaves a net loss of material."
        }

        // Opponent counter-tactic: find if opponent has a fork or immediate mate in reply
        val replies = moveGen.generateLegalMoves(after)
        for (reply in replies) {
            val replyAfter = moveGen.applyMove(after, reply)
            if (moveGen.isKingInCheck(replyAfter, before.activeColor)) {
                val nextReplies = moveGen.generateLegalMoves(replyAfter)
                if (nextReplies.isEmpty()) {
                    return "Blunders into checkmate in 1 via ${reply.san}!"
                }
            }
            val forked = findForkedPieces(replyAfter, reply.to, after.activeColor)
            if (forked.any { it.type == PieceType.QUEEN || it.type == PieceType.KING }) {
                return "Allows opponent fork on ${reply.to.algebraic} hitting queen/king."
            }
        }

        return if (evalDrop > 300) {
            "Severe blunder losing substantial positional and material advantage."
        } else {
            "Tempting but tactically flawed; drops ${evalDrop / 100}.${Math.abs(evalDrop % 100) / 10} in evaluation."
        }
    }

    private fun findForkedPieces(pos: ChessPosition, attackerSquare: ChessSquare, attackerColor: PieceColor): List<ChessPiece> {
        val forked = mutableListOf<ChessPiece>()
        val enemyColor = attackerColor.opposite()
        for (r in 0..7) {
            for (c in 0..7) {
                val sq = ChessSquare(r, c)
                val p = pos.getPiece(sq) ?: continue
                if (p.color == enemyColor && p.type != PieceType.PAWN) {
                    if (moveGen.isSquareAttackedBy(pos, sq, attackerColor)) {
                        forked.add(p)
                    }
                }
            }
        }
        return forked
    }

    private fun findHangingPieceDefended(before: ChessPosition, after: ChessPosition, friendlyColor: PieceColor): ChessSquare? {
        val enemyColor = friendlyColor.opposite()
        for (r in 0..7) {
            for (c in 0..7) {
                val sq = ChessSquare(r, c)
                val p = before.getPiece(sq) ?: continue
                if (p.color == friendlyColor && p.type != PieceType.KING) {
                    val wasAttacked = moveGen.isSquareAttackedBy(before, sq, enemyColor)
                    val wasDefended = moveGen.isSquareAttackedBy(before, sq, friendlyColor)
                    if (wasAttacked && !wasDefended) {
                        val isNowDefended = moveGen.isSquareAttackedBy(after, sq, friendlyColor)
                        if (isNowDefended) return sq
                    }
                }
            }
        }
        return null
    }
}
