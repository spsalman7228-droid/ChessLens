package com.example.chess

import com.example.model.ChessMove
import com.example.model.ChessPosition
import com.example.model.ChessSquare
import com.example.model.MoveAnalysis
import com.example.model.MoveQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WebAnalysisSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
) {
    suspend fun queryLichessCloud(position: ChessPosition): List<MoveAnalysis>? = withContext(Dispatchers.IO) {
        try {
            val fen = URLEncoder.encode(position.toFen(), "UTF-8")
            val url = "https://lichess.org/api/cloud-eval?fen=$fen&multiPv=3"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ChessLens-Android-LearningTool")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            val pvs = json.optJSONArray("pvs") ?: return@withContext null
            val results = mutableListOf<MoveAnalysis>()

            for (i in 0 until pvs.length()) {
                val pvObj = pvs.getJSONObject(i)
                val movesStr = pvObj.optString("moves", "")
                val uciList = movesStr.split(" ")
                val firstUci = uciList.firstOrNull() ?: continue
                if (firstUci.length < 4) continue

                val from = ChessSquare.fromAlgebraic(firstUci.substring(0, 2)) ?: continue
                val to = ChessSquare.fromAlgebraic(firstUci.substring(2, 4)) ?: continue
                val move = ChessMove(from, to)

                val cp = pvObj.optInt("cp", 0)
                val mate = pvObj.optInt("mate", 0)
                val isMate = mate != 0

                results.add(
                    MoveAnalysis(
                        move = move,
                        evalCentipawns = if (isMate) (if (mate > 0) 10000 - mate else -10000 - mate) else cp,
                        isMate = isMate,
                        mateInMoves = Math.abs(mate),
                        quality = MoveQuality.BEST,
                        explanation = if (isMate) "Lichess Cloud: Mate in $mate" else "Lichess Master Book evaluation (+${cp/100}.${Math.abs(cp%100)/10})",
                        levelName = "Lichess Cloud API",
                        levelColorHex = "#5AA4FF",
                        pv = listOf(move),
                        depth = json.optInt("depth", 25)
                    )
                )
            }
            results
        } catch (e: Exception) {
            null
        }
    }

    suspend fun queryStockfishOnline(position: ChessPosition, depth: Int = 12): MoveAnalysis? = withContext(Dispatchers.IO) {
        try {
            val fen = URLEncoder.encode(position.toFen(), "UTF-8")
            val url = "https://stockfish.online/api/s/v2.php?fen=$fen&depth=$depth"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ChessLens-Android-LearningTool")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            if (!json.optBoolean("success", false)) return@withContext null

            val bestmoveStr = json.optString("bestmove", "")
            // format: "bestmove e2e4 ponder e7e5"
            val parts = bestmoveStr.split(" ")
            val uci = if (parts.size >= 2) parts[1] else return@withContext null
            if (uci.length < 4) return@withContext null

            val from = ChessSquare.fromAlgebraic(uci.substring(0, 2)) ?: return@withContext null
            val to = ChessSquare.fromAlgebraic(uci.substring(2, 4)) ?: return@withContext null
            val move = ChessMove(from, to)

            val eval = json.optDouble("evaluation", 0.0)
            val mate = json.optInt("mate", 0)
            val isMate = mate != 0
            val evalCp = if (isMate) (if (mate > 0) 10000 - mate else -10000 - mate) else (eval * 100).toInt()

            MoveAnalysis(
                move = move,
                evalCentipawns = evalCp,
                isMate = isMate,
                mateInMoves = Math.abs(mate),
                quality = MoveQuality.BEST,
                explanation = if (isMate) "Stockfish Online: Mate in $mate" else "Stockfish Online API (d$depth): eval %.2f".format(eval),
                levelName = "Stockfish Online API",
                levelColorHex = "#C084FC",
                pv = listOf(move),
                depth = depth
            )
        } catch (e: Exception) {
            null
        }
    }
}
