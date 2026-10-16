package com.example.model

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class ChessPosition(
    val board: Array<ChessPiece?> = Array(64) { null },
    var activeColor: PieceColor = PieceColor.WHITE,
    var whiteCanCastleKingside: Boolean = true,
    var whiteCanCastleQueenside: Boolean = true,
    var blackCanCastleKingside: Boolean = true,
    var blackCanCastleQueenside: Boolean = true,
    var enPassantTarget: ChessSquare? = null,
    var halfmoveClock: Int = 0,
    var fullmoveNumber: Int = 1
) {
    fun getPiece(row: Int, col: Int): ChessPiece? {
        if (row !in 0..7 || col !in 0..7) return null
        return board[row * 8 + col]
    }

    fun getPiece(square: ChessSquare): ChessPiece? {
        return getPiece(square.row, square.col)
    }

    fun setPiece(row: Int, col: Int, piece: ChessPiece?) {
        if (row in 0..7 && col in 0..7) {
            board[row * 8 + col] = piece
        }
    }

    fun setPiece(square: ChessSquare, piece: ChessPiece?) {
        setPiece(square.row, square.col, piece)
    }

    fun clone(): ChessPosition {
        return ChessPosition(
            board = board.copyOf(),
            activeColor = activeColor,
            whiteCanCastleKingside = whiteCanCastleKingside,
            whiteCanCastleQueenside = whiteCanCastleQueenside,
            blackCanCastleKingside = blackCanCastleKingside,
            blackCanCastleQueenside = blackCanCastleQueenside,
            enPassantTarget = enPassantTarget,
            halfmoveClock = halfmoveClock,
            fullmoveNumber = fullmoveNumber
        )
    }

    fun toFen(): String {
        val sb = StringBuilder()
        // 1. Board placement
        for (r in 0..7) {
            var emptyCount = 0
            for (c in 0..7) {
                val piece = getPiece(r, c)
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(piece.fenChar)
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (r < 7) {
                sb.append('/')
            }
        }

        // 2. Active color
        sb.append(if (activeColor == PieceColor.WHITE) " w " else " b ")

        // 3. Castling rights
        val castling = StringBuilder()
        if (whiteCanCastleKingside) castling.append('K')
        if (whiteCanCastleQueenside) castling.append('Q')
        if (blackCanCastleKingside) castling.append('k')
        if (blackCanCastleQueenside) castling.append('q')
        if (castling.isEmpty()) castling.append('-')
        sb.append(castling)
        sb.append(' ')

        // 4. En passant
        sb.append(enPassantTarget?.algebraic ?: "-")
        sb.append(' ')

        // 5. Halfmove clock & fullmove number
        sb.append(halfmoveClock)
        sb.append(' ')
        sb.append(fullmoveNumber)

        return sb.toString()
    }

    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        var whiteKings = 0
        var blackKings = 0
        var whitePawns = 0
        var blackPawns = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val p = getPiece(r, c) ?: continue
                when (p.type) {
                    PieceType.KING -> {
                        if (p.color == PieceColor.WHITE) whiteKings++ else blackKings++
                    }
                    PieceType.PAWN -> {
                        if (p.color == PieceColor.WHITE) whitePawns++ else blackPawns++
                        if (r == 0 || r == 7) {
                            errors.add("Pawns cannot exist on 1st or 8th rank: ${ChessSquare(r, c).algebraic}")
                        }
                    }
                    else -> {}
                }
            }
        }

        if (whiteKings != 1) errors.add("White must have exactly 1 king (found $whiteKings)")
        if (blackKings != 1) errors.add("Black must have exactly 1 king (found $blackKings)")
        if (whitePawns > 8) warnings.add("White has more than 8 pawns ($whitePawns)")
        if (blackPawns > 8) warnings.add("Black has more than 8 pawns ($blackPawns)")

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChessPosition) return false
        if (!board.contentEquals(other.board)) return false
        if (activeColor != other.activeColor) return false
        if (whiteCanCastleKingside != other.whiteCanCastleKingside) return false
        if (whiteCanCastleQueenside != other.whiteCanCastleQueenside) return false
        if (blackCanCastleKingside != other.blackCanCastleKingside) return false
        if (blackCanCastleQueenside != other.blackCanCastleQueenside) return false
        if (enPassantTarget != other.enPassantTarget) return false
        return true
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + activeColor.hashCode()
        result = 31 * result + whiteCanCastleKingside.hashCode()
        result = 31 * result + whiteCanCastleQueenside.hashCode()
        result = 31 * result + blackCanCastleKingside.hashCode()
        result = 31 * result + blackCanCastleQueenside.hashCode()
        result = 31 * result + (enPassantTarget?.hashCode() ?: 0)
        return result
    }

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun initial(): ChessPosition = fromFen(STARTING_FEN) ?: ChessPosition()

        fun fromFen(fen: String): ChessPosition? {
            try {
                val parts = fen.trim().split("\\s+".toRegex())
                if (parts.isEmpty()) return null

                val board = Array<ChessPiece?>(64) { null }
                val rows = parts[0].split('/')
                if (rows.size != 8) return null

                for (r in 0..7) {
                    val rowStr = rows[r]
                    var col = 0
                    for (char in rowStr) {
                        if (char.isDigit()) {
                            col += char.digitToInt()
                        } else {
                            val piece = ChessPiece.fromFenChar(char) ?: return null
                            if (col < 8) {
                                board[r * 8 + col] = piece
                            }
                            col++
                        }
                    }
                }

                val activeColor = if (parts.size > 1 && parts[1].lowercase() == "b") PieceColor.BLACK else PieceColor.WHITE

                val castling = if (parts.size > 2) parts[2] else "-"
                val wck = castling.contains('K')
                val wcq = castling.contains('Q')
                val bck = castling.contains('k')
                val bcq = castling.contains('q')

                val ep = if (parts.size > 3 && parts[3] != "-") ChessSquare.fromAlgebraic(parts[3]) else null
                val halfmove = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
                val fullmove = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1

                return ChessPosition(
                    board = board,
                    activeColor = activeColor,
                    whiteCanCastleKingside = wck,
                    whiteCanCastleQueenside = wcq,
                    blackCanCastleKingside = bck,
                    blackCanCastleQueenside = bcq,
                    enPassantTarget = ep,
                    halfmoveClock = halfmove,
                    fullmoveNumber = fullmove
                )
            } catch (e: Exception) {
                return null
            }
        }
    }
}
