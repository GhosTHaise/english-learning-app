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
    val isLearned: Boolean = false
)

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val lastLoginTimestamp: Long = 0,
    val wordsLearnedCount: Int = 0
)

@Dao
interface AppDao {
    @Query("SELECT * FROM vocabulary ORDER BY id ASC")
    fun getAllWords(): Flow<List<VocabularyWord>>

    @Query("SELECT * FROM vocabulary WHERE isLearned = 0 LIMIT 10")
    fun getDailyWords(): Flow<List<VocabularyWord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<VocabularyWord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWord(word: VocabularyWord)

    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getUserProgress(): Flow<UserProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProgress(progress: UserProgress)
}

@Database(entities = [VocabularyWord::class, UserProgress::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}

class Repository(private val dao: AppDao) {
    val allWords = dao.getAllWords()
    val dailyWords = dao.getDailyWords()
    val userProgress = dao.getUserProgress()

    suspend fun insertInitialWordsIfEmpty(words: List<VocabularyWord>) {
        dao.insertWords(words)
    }

    suspend fun updateWord(word: VocabularyWord) {
        dao.updateWord(word)
    }

    suspend fun saveUserProgress(progress: UserProgress) {
        dao.saveUserProgress(progress)
    }
}
