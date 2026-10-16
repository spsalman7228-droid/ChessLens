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
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
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
                        explanation = if (isMate) "Lichess database: Mate in $mate" else "Lichess Master Book evaluation (+${cp/100}.${Math.abs(cp%100)/10})",
                        levelName = "Database",
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
}
