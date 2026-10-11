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
    val longestStreak: Int = 0,
    val lastPracticedDate: String = "", // Format "yyyy-MM-dd"
    val lastLoginTimestamp: Long = 0,
    val wordsLearnedCount: Int = 0,
    val totalPracticeDays: Int = 0,
    val streakFreezeCount: Int = 1
)

@Entity(tableName = "daily_practice_logs")
data class DailyPracticeLog(
    @PrimaryKey val date: String, // Format "yyyy-MM-dd"
    val practiceCount: Int = 1,
    val minutesSpent: Int = 5,
    val activitiesSummary: String = "Pratique d'anglais",
    val timestamp: Long = System.currentTimeMillis()
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

    @Query("SELECT * FROM daily_practice_logs ORDER BY date DESC")
    fun getAllPracticeLogs(): Flow<List<DailyPracticeLog>>

    @Query("SELECT * FROM daily_practice_logs WHERE date = :date LIMIT 1")
    suspend fun getPracticeLogForDate(date: String): DailyPracticeLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPracticeLog(log: DailyPracticeLog)

    @Query("DELETE FROM daily_practice_logs")
    suspend fun clearPracticeLogs()
}

@Database(entities = [VocabularyWord::class, UserProgress::class, CompletedLesson::class, DailyPracticeLog::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}

class Repository(private val dao: AppDao) {
    val allWords = dao.getAllWords()
    val dailyWords = dao.getDailyWords()
    fun getHistoryWords(sevenDaysAgo: Long) = dao.getHistoryWords(sevenDaysAgo)
    val userProgress = dao.getUserProgress()
    val completedLessons = dao.getAllCompletedLessons()
    val dailyPracticeLogs = dao.getAllPracticeLogs()

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

    suspend fun getPracticeLogForDate(date: String): DailyPracticeLog? {
        return dao.getPracticeLogForDate(date)
    }

    suspend fun insertPracticeLog(log: DailyPracticeLog) {
        dao.insertPracticeLog(log)
    }

    suspend fun clearPracticeLogs() {
        dao.clearPracticeLogs()
    }
}
