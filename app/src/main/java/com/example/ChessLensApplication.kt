package com.example

import android.app.Application
import androidx.room.Room
import com.example.chess.ChessMoveGenerator
import com.example.chess.EvaluationEngine
import com.example.chess.TacticalDetector
import com.example.chess.WebAnalysisSource
import com.example.data.AppDatabase
import com.example.data.HistoryRepository
import com.example.data.PreferencesManager
import com.example.vision.BoardDetector

class ChessLensApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var historyRepository: HistoryRepository
        private set

    val moveGenerator = ChessMoveGenerator()
    val tacticalDetector = TacticalDetector(moveGenerator)
    val evaluationEngine = EvaluationEngine(moveGenerator, tacticalDetector)
    val webAnalysisSource = WebAnalysisSource()
    val boardDetector = BoardDetector()

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "chesslens.db"
        ).fallbackToDestructiveMigration().build()
        historyRepository = HistoryRepository(database.historyDao())
    }

    companion object {
        lateinit var instance: ChessLensApplication
            private set
    }
}
