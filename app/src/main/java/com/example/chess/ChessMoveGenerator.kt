package com.example.chess

import com.example.model.ChessMove
import com.example.model.ChessPiece
import com.example.model.ChessPosition
import com.example.model.ChessSquare
import com.example.model.PieceColor
import com.example.model.PieceType

class ChessMoveGenerator {

    companion object {
        private val KNIGHT_OFFSETS = arrayOf(
            intArrayOf(-2, -1), intArrayOf(-2, 1),
            intArrayOf(-1, -2), intArrayOf(-1, 2),
            intArrayOf(1, -2), intArrayOf(1, 2),
            intArrayOf(2, -1), intArrayOf(2, 1)
        )

        private val BISHOP_DIRS = arrayOf(
            intArrayOf(-1, -1), intArrayOf(-1, 1),
            intArrayOf(1, -1), intArrayOf(1, 1)
        )

        private val ROOK_DIRS = arrayOf(
            intArrayOf(-1, 0), intArrayOf(1, 0),
            intArrayOf(0, -1), intArrayOf(0, 1)
        )

        private val QUEEN_KING_DIRS = arrayOf(
            intArrayOf(-1, -1), intArrayOf(-1, 0), intArrayOf(-1, 1),
            intArrayOf(0, -1), intArrayOf(0, 1),
            intArrayOf(1, -1), intArrayOf(1, 0), intArrayOf(1, 1)
        )
    }

    fun generateLegalMoves(position: ChessPosition): List<ChessMove> {
        val pseudoMoves = generatePseudoMoves(position, position.activeColor)
        val legalMoves = mutableListOf<ChessMove>()

        for (move in pseudoMoves) {
            val nextPos = applyMove(position, move)
            // If the moving side's king is NOT in check after the move, it's legal
            if (!isKingInCheck(nextPos, position.activeColor)) {
                // Determine SAN
                move.san = formatSan(position, move, nextPos)
                legalMoves.add(move)
            }
        }

        return legalMoves
    }

    fun isKingInCheck(position: ChessPosition, color: PieceColor): Boolean {
        // Find king square
        var kingSquare: ChessSquare? = null
        for (r in 0..7) {
            for (c in 0..7) {
                val p = position.getPiece(r, c)
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    kingSquare = ChessSquare(r, c)
                    break
                }
            }
            if (kingSquare != null) break
        }
        if (kingSquare == null) return false

        return isSquareAttackedBy(position, kingSquare, color.opposite())
    }

    fun isSquareAttackedBy(position: ChessPosition, target: ChessSquare, attackerColor: PieceColor): Boolean {
        val tr = target.row
        val tc = target.col

        // 1. Pawn attacks
        val pawnPushes = if (attackerColor == PieceColor.WHITE) 1 else -1 // attacker row offset relative to target
        val attackerPawnRow = tr + pawnPushes
        if (attackerPawnRow in 0..7) {
            for (dc in intArrayOf(-1, 1)) {
                val ac = tc + dc
                if (ac in 0..7) {
                    val piece = position.getPiece(attackerPawnRow, ac)
                    if (piece != null && piece.color == attackerColor && piece.type == PieceType.PAWN) {
                        return true
                    }
                }
            }
        }

        // 2. Knight attacks
        for (offset in KNIGHT_OFFSETS) {
            val r = tr + offset[0]
            val c = tc + offset[1]
            if (r in 0..7 && c in 0..7) {
                val piece = position.getPiece(r, c)
                if (piece != null && piece.color == attackerColor && piece.type == PieceType.KNIGHT) {
                    return true
                }
            }
        }

        // 3. Bishop / Queen diagonal attacks
        for (dir in BISHOP_DIRS) {
            var r = tr + dir[0]
            var c = tc + dir[1]
            while (r in 0..7 && c in 0..7) {
                val piece = position.getPiece(r, c)
                if (piece != null) {
                    if (piece.color == attackerColor && (piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dir[0]
                c += dir[1]
            }
        }

        // 4. Rook / Queen straight attacks
        for (dir in ROOK_DIRS) {
            var r = tr + dir[0]
            var c = tc + dir[1]
            while (r in 0..7 && c in 0..7) {
                val piece = position.getPiece(r, c)
                if (piece != null) {
                    if (piece.color == attackerColor && (piece.type == PieceType.ROOK || piece.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dir[0]
                c += dir[1]
            }
        }

        // 5. King attacks (distance 1)
        for (dir in QUEEN_KING_DIRS) {
            val r = tr + dir[0]
            val c = tc + dir[1]
            if (r in 0..7 && c in 0..7) {
                val piece = position.getPiece(r, c)
                if (piece != null && piece.color == attackerColor && piece.type == PieceType.KING) {
                    return true
                }
            }
        }

        return false
    }

    private fun generatePseudoMoves(position: ChessPosition, color: PieceColor): List<ChessMove> {
        val moves = mutableListOf<ChessMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = position.getPiece(r, c) ?: continue
                if (piece.color != color) continue
                val from = ChessSquare(r, c)
                when (piece.type) {
                    PieceType.PAWN -> generatePawnMoves(position, from, color, moves)
                    PieceType.KNIGHT -> generateKnightMoves(position, from, color, moves)
                    PieceType.BISHOP -> generateRayMoves(position, from, color, BISHOP_DIRS, moves)
                    PieceType.ROOK -> generateRayMoves(position, from, color, ROOK_DIRS, moves)
                    PieceType.QUEEN -> generateRayMoves(position, from, color, QUEEN_KING_DIRS, moves)
                    PieceType.KING -> generateKingMoves(position, from, color, moves)
                }
            }
        }
        return moves
    }

    private fun generatePawnMoves(position: ChessPosition, from: ChessSquare, color: PieceColor, moves: MutableList<ChessMove>) {
        val forward = if (color == PieceColor.WHITE) -1 else 1
        val startRow = if (color == PieceColor.WHITE) 6 else 1
        val promoRow = if (color == PieceColor.WHITE) 0 else 7

        // 1 square forward
        val r1 = from.row + forward
        if (r1 in 0..7 && position.getPiece(r1, from.col) == null) {
            val to = ChessSquare(r1, from.col)
            if (r1 == promoRow) {
                for (promo in arrayOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                    moves.add(ChessMove(from, to, promotion = promo))
                }
            } else {
                moves.add(ChessMove(from, to))
                // 2 squares forward
                val r2 = from.row + 2 * forward
                if (from.row == startRow && position.getPiece(r2, from.col) == null) {
                    moves.add(ChessMove(from, ChessSquare(r2, from.col)))
                }
            }
        }

        // Captures
        for (dc in intArrayOf(-1, 1)) {
            val cc = from.col + dc
            if (r1 in 0..7 && cc in 0..7) {
                val to = ChessSquare(r1, cc)
                val targetPiece = position.getPiece(r1, cc)
                if (targetPiece != null && targetPiece.color != color) {
                    if (r1 == promoRow) {
                        for (promo in arrayOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                            moves.add(ChessMove(from, to, promotion = promo, isCapture = true))
                        }
                    } else {
                        moves.add(ChessMove(from, to, isCapture = true))
                    }
                } else if (position.enPassantTarget == to) {
                    moves.add(ChessMove(from, to, isCapture = true, isEnPassant = true))
                }
            }
        }
    }

    private fun generateKnightMoves(position: ChessPosition, from: ChessSquare, color: PieceColor, moves: MutableList<ChessMove>) {
        for (offset in KNIGHT_OFFSETS) {
            val r = from.row + offset[0]
            val c = from.col + offset[1]
            if (r in 0..7 && c in 0..7) {
                val target = position.getPiece(r, c)
                if (target == null) {
                    moves.add(ChessMove(from, ChessSquare(r, c)))
                } else if (target.color != color) {
                    moves.add(ChessMove(from, ChessSquare(r, c), isCapture = true))
                }
            }
        }
    }

    private fun generateRayMoves(
        position: ChessPosition,
        from: ChessSquare,
        color: PieceColor,
        directions: Array<IntArray>,
        moves: MutableList<ChessMove>
    ) {
        for (dir in directions) {
            var r = from.row + dir[0]
            var c = from.col + dir[1]
            while (r in 0..7 && c in 0..7) {
                val target = position.getPiece(r, c)
                if (target == null) {
                    moves.add(ChessMove(from, ChessSquare(r, c)))
                } else {
                    if (target.color != color) {
                        moves.add(ChessMove(from, ChessSquare(r, c), isCapture = true))
                    }
                    break
                }
                r += dir[0]
                c += dir[1]
            }
        }
    }

    private fun generateKingMoves(position: ChessPosition, from: ChessSquare, color: PieceColor, moves: MutableList<ChessMove>) {
        for (dir in QUEEN_KING_DIRS) {
            val r = from.row + dir[0]
            val c = from.col + dir[1]
            if (r in 0..7 && c in 0..7) {
                val target = position.getPiece(r, c)
                if (target == null) {
                    moves.add(ChessMove(from, ChessSquare(r, c)))
                } else if (target.color != color) {
                    moves.add(ChessMove(from, ChessSquare(r, c), isCapture = true))
                }
            }
        }

        // Castling
        val isWhite = color == PieceColor.WHITE
        val kingRow = if (isWhite) 7 else 0
        if (from.row == kingRow && from.col == 4 && !isKingInCheck(position, color)) {
            // Kingside (O-O)
            val canKingside = if (isWhite) position.whiteCanCastleKingside else position.blackCanCastleKingside
            if (canKingside &&
                position.getPiece(kingRow, 5) == null &&
                position.getPiece(kingRow, 6) == null &&
                !isSquareAttackedBy(position, ChessSquare(kingRow, 5), color.opposite()) &&
                !isSquareAttackedBy(position, ChessSquare(kingRow, 6), color.opposite())
            ) {
                moves.add(ChessMove(from, ChessSquare(kingRow, 6), isCastling = true))
            }

            // Queenside (O-O-O)
            val canQueenside = if (isWhite) position.whiteCanCastleQueenside else position.blackCanCastleQueenside
            if (canQueenside &&
                position.getPiece(kingRow, 1) == null &&
                position.getPiece(kingRow, 2) == null &&
                position.getPiece(kingRow, 3) == null &&
                !isSquareAttackedBy(position, ChessSquare(kingRow, 3), color.opposite()) &&
                !isSquareAttackedBy(position, ChessSquare(kingRow, 2), color.opposite())
            ) {
                moves.add(ChessMove(from, ChessSquare(kingRow, 2), isCastling = true))
            }
        }
    }

    fun applyMove(position: ChessPosition, move: ChessMove): ChessPosition {
        val next = position.clone()
        val piece = next.getPiece(move.from) ?: return next

        next.setPiece(move.from, null)

        // Handle promotion
        val placedPiece = if (move.promotion != null) {
            ChessPiece(move.promotion, piece.color)
        } else {
            piece
        }
        next.setPiece(move.to, placedPiece)

        // Handle En Passant capture
        if (move.isEnPassant && piece.type == PieceType.PAWN) {
            val capturedPawnRow = move.from.row
            val capturedPawnCol = move.to.col
            next.setPiece(capturedPawnRow, capturedPawnCol, null)
        }

        // Set en passant target for double pawn push
        if (piece.type == PieceType.PAWN && Math.abs(move.to.row - move.from.row) == 2) {
            val epRow = (move.from.row + move.to.row) / 2
            next.enPassantTarget = ChessSquare(epRow, move.from.col)
        } else {
            next.enPassantTarget = null
        }

        // Handle Castling rook move
        if (move.isCastling && piece.type == PieceType.KING) {
            val r = move.from.row
            if (move.to.col == 6) { // Kingside
                val rook = next.getPiece(r, 7)
                next.setPiece(r, 7, null)
                next.setPiece(r, 5, rook)
            } else if (move.to.col == 2) { // Queenside
                val rook = next.getPiece(r, 0)
                next.setPiece(r, 0, null)
                next.setPiece(r, 3, rook)
            }
        }

        // Revoke castling rights on king or rook moves
        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) {
                next.whiteCanCastleKingside = false
                next.whiteCanCastleQueenside = false
            } else {
                next.blackCanCastleKingside = false
                next.blackCanCastleQueenside = false
            }
        }
        if (piece.type == PieceType.ROOK) {
            if (move.from == ChessSquare(7, 0)) next.whiteCanCastleQueenside = false
            if (move.from == ChessSquare(7, 7)) next.whiteCanCastleKingside = false
            if (move.from == ChessSquare(0, 0)) next.blackCanCastleQueenside = false
            if (move.from == ChessSquare(0, 7)) next.blackCanCastleKingside = false
        }
        // If rook is captured in corner
        if (move.to == ChessSquare(7, 0)) next.whiteCanCastleQueenside = false
        if (move.to == ChessSquare(7, 7)) next.whiteCanCastleKingside = false
        if (move.to == ChessSquare(0, 0)) next.blackCanCastleQueenside = false
        if (move.to == ChessSquare(0, 7)) next.blackCanCastleKingside = false

        // Clocks & active color
        if (piece.type == PieceType.PAWN || move.isCapture) {
            next.halfmoveClock = 0
        } else {
            next.halfmoveClock++
        }

        if (next.activeColor == PieceColor.BLACK) {
            next.fullmoveNumber++
        }
        next.activeColor = next.activeColor.opposite()

        return next
    }

    private fun formatSan(currentPos: ChessPosition, move: ChessMove, nextPos: ChessPosition): String {
        if (move.isCastling) {
            return if (move.to.col == 6) "O-O" else "O-O-O"
        }
        val piece = currentPos.getPiece(move.from) ?: return move.uci
        val sb = StringBuilder()

        if (piece.type != PieceType.PAWN) {
            sb.append(piece.type.code.uppercaseChar())
            // Disambiguation
            val disambiguate = currentPos.board.indices
                .mapNotNull { i ->
                    val sq = ChessSquare.fromIndex(i)
                    val p = currentPos.getPiece(sq)
                    if (sq != move.from && p != null && p.color == piece.color && p.type == piece.type) {
                        val testMove = ChessMove(sq, move.to)
                        val testNext = applyMove(currentPos, testMove)
                        if (!isKingInCheck(testNext, piece.color)) sq else null
                    } else null
                }
            if (disambiguate.isNotEmpty()) {
                val sameFile = disambiguate.any { it.col == move.from.col }
                val sameRank = disambiguate.any { it.row == move.from.row }
                if (!sameFile) {
                    sb.append(('a' + move.from.col))
                } else if (!sameRank) {
                    sb.append((8 - move.from.row))
                } else {
                    sb.append(move.from.algebraic)
                }
            }
        } else if (move.isCapture) {
            sb.append(('a' + move.from.col))
        }

        if (move.isCapture) {
            sb.append('x')
        }

        sb.append(move.to.algebraic)

        if (move.promotion != null) {
            sb.append('=')
            sb.append(move.promotion.code.uppercaseChar())
        }

        // Check or Checkmate suffix
        val isOpponentInCheck = isKingInCheck(nextPos, nextPos.activeColor)
        if (isOpponentInCheck) {
            val opponentMoves = generateLegalMoves(nextPos)
            if (opponentMoves.isEmpty()) {
                sb.append('#')
            } else {
                sb.append('+')
            }
        }

        return sb.toString()
    }
}
