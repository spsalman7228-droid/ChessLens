package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "analysis_history")
data class AnalysisHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val fen: String,
    val bestMoveSan: String,
    val bestMoveUci: String,
    val evalCentipawns: Int,
    val explanation: String,
    val sourceTag: String = "Snapshot",
    val notes: String = "",
    val pgnSnippet: String = ""
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM analysis_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<AnalysisHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AnalysisHistoryEntity): Long

    @Query("DELETE FROM analysis_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM analysis_history")
    suspend fun clearAll()
}

@Database(entities = [AnalysisHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}

class HistoryRepository(private val dao: HistoryDao) {
    val allHistory: Flow<List<AnalysisHistoryEntity>> = dao.getAllHistory()

    suspend fun saveAnalysis(
        fen: String,
        bestMoveSan: String,
        bestMoveUci: String,
        evalCp: Int,
        explanation: String,
        sourceTag: String = "Snapshot",
        notes: String = ""
    ) {
        dao.insert(
            AnalysisHistoryEntity(
                fen = fen,
                bestMoveSan = bestMoveSan,
                bestMoveUci = bestMoveUci,
                evalCentipawns = evalCp,
                explanation = explanation,
                sourceTag = sourceTag,
                notes = notes
            )
        )
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun clearAll() = dao.clearAll()
}
