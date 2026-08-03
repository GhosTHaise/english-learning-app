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

@Entity(tableName = "vocabulary")
data class VocabularyWord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val english: String,
    val french: String,
    val isLearned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val lastLoginTimestamp: Long = 0,
    val wordsLearnedCount: Int = 0
)

@Entity(tableName = "completed_lessons")
data class CompletedLesson(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lessonTitle: String,
    val topic: String,
    val level: String,
    val score: Int,
    val maxScore: Int = 100,
    val status: String = "COMPLETED",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface AppDao {
    @Query("SELECT * FROM vocabulary ORDER BY id ASC")
    fun getAllWords(): Flow<List<VocabularyWord>>

    @Query("SELECT * FROM vocabulary WHERE isLearned = 0 ORDER BY id DESC LIMIT 5")
    fun getDailyWords(): Flow<List<VocabularyWord>>

    @Query("SELECT * FROM vocabulary WHERE timestamp >= :sevenDaysAgo ORDER BY timestamp DESC")
    fun getHistoryWords(sevenDaysAgo: Long): Flow<List<VocabularyWord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<VocabularyWord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWord(word: VocabularyWord)

    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getUserProgress(): Flow<UserProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProgress(progress: UserProgress)

    @Query("SELECT * FROM completed_lessons ORDER BY timestamp DESC")
    fun getAllCompletedLessons(): Flow<List<CompletedLesson>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompletedLesson(lesson: CompletedLesson)

    @Query("DELETE FROM completed_lessons")
    suspend fun clearCompletedLessons()
}

@Database(entities = [VocabularyWord::class, UserProgress::class, CompletedLesson::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}

class Repository(private val dao: AppDao) {
    val allWords = dao.getAllWords()
    val dailyWords = dao.getDailyWords()
    fun getHistoryWords(sevenDaysAgo: Long) = dao.getHistoryWords(sevenDaysAgo)
    val userProgress = dao.getUserProgress()
    val completedLessons = dao.getAllCompletedLessons()

    suspend fun insertWords(words: List<VocabularyWord>) {
        dao.insertWords(words)
    }

    suspend fun updateWord(word: VocabularyWord) {
        dao.updateWord(word)
    }

    suspend fun saveUserProgress(progress: UserProgress) {
        dao.saveUserProgress(progress)
    }

    suspend fun saveCompletedLesson(lesson: CompletedLesson) {
        dao.saveCompletedLesson(lesson)
    }

    suspend fun clearCompletedLessons() {
        dao.clearCompletedLessons()
    }
}
