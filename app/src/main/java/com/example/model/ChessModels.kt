package com.example.model

enum class PieceColor {
    WHITE,
    BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val code: Char, val value: Int, val symbolWhite: String, val symbolBlack: String) {
    PAWN('p', 100, "♙", "♟"),
    KNIGHT('n', 320, "♘", "♞"),
    BISHOP('b', 330, "♗", "♝"),
    ROOK('r', 500, "♖", "♜"),
    QUEEN('q', 900, "♕", "♛"),
    KING('k', 20000, "♔", "♚");

    companion object {
        fun fromChar(char: Char): PieceType? {
            return when (char.lowercaseChar()) {
                'p' -> PAWN
                'n' -> KNIGHT
                'b' -> BISHOP
                'r' -> ROOK
                'q' -> QUEEN
                'k' -> KING
                else -> null
            }
        }
    }
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor
) {
    val fenChar: Char
        get() = if (color == PieceColor.WHITE) type.code.uppercaseChar() else type.code

    val symbol: String
        get() = if (color == PieceColor.WHITE) type.symbolWhite else type.symbolBlack

    companion object {
        fun fromFenChar(c: Char): ChessPiece? {
            val type = PieceType.fromChar(c) ?: return null
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            return ChessPiece(type, color)
        }
    }
}

data class ChessSquare(
    val row: Int, // 0 is rank 8 (top), 7 is rank 1 (bottom)
    val col: Int  // 0 is file a (left), 7 is file h (right)
) {
    val algebraic: String
        get() = "${('a' + col)}${8 - row}"

    val index: Int
        get() = row * 8 + col

    val isLight: Boolean
        get() = (row + col) % 2 == 0

    companion object {
        fun fromAlgebraic(alg: String): ChessSquare? {
            if (alg.length < 2) return null
            val file = alg[0].lowercaseChar() - 'a'
            val rank = alg[1].digitToIntOrNull() ?: return null
            if (file !in 0..7 || rank !in 1..8) return null
            return ChessSquare(row = 8 - rank, col = file)
        }

        fun fromIndex(index: Int): ChessSquare {
            return ChessSquare(row = index / 8, col = index % 8)
        }
    }
}

data class ChessMove(
    val from: ChessSquare,
    val to: ChessSquare,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
    var san: String = ""
) {
    val uci: String
        get() = "${from.algebraic}${to.algebraic}${promotion?.code?.lowercaseChar() ?: ""}"
}

enum class MoveQuality(val label: String, val badgeColorHex: String) {
    BEST("Best", "#38BDF8"),
    GOOD("Good", "#4ADE80"),
    INACCURACY("Inaccuracy", "#FBBF24"),
    MISTAKE("Mistake", "#FB923C"),
    BLUNDER("Blunder", "#F87171"),
    BRILLIANT("Brilliant!!", "#06B6D4"),
    MISSED_WIN("Missed Win", "#EF4444")
}

data class MoveAnalysis(
    val move: ChessMove,
    val evalCentipawns: Int,
    val isMate: Boolean = false,
    val mateInMoves: Int = 0,
    val quality: MoveQuality = MoveQuality.BEST,
    val explanation: String = "",
    val avoidReason: String? = null,
    val levelName: String = "",
    val levelColorHex: String = "#38BDF8",
    val pv: List<ChessMove> = emptyList(),
    val depth: Int = 10,
    val nodesPerSec: Long = 0,
    val winPercent: Int = 50,
    val drawPercent: Int = 50,
    val lossPercent: Int = 0
)

data class AnalysisLevel(
    val id: String,
    val name: String,
    val colorHex: String,
    val depth: Int,
    val multiPv: Int = 1,
    val enabled: Boolean = true,
    val source: AnalysisSourceType = AnalysisSourceType.LOCAL
)

enum class AnalysisSourceType {
    LOCAL,
    LICHESS,
    STOCKFISH_ONLINE,
    AVOID
}
