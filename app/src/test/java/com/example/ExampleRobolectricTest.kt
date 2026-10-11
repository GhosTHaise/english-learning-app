package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("L'Anglais Facile", appName)
  }

  @Test
  fun `room database stores daily practice log and updates user progress`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java).build()
    val dao = db.appDao()

    kotlinx.coroutines.runBlocking {
      val log = com.example.data.DailyPracticeLog(
        date = "2026-10-11",
        practiceCount = 2,
        minutesSpent = 15,
        activitiesSummary = "Prononciation, Vocabulaire"
      )
      dao.insertPracticeLog(log)

      val fetched = dao.getPracticeLogForDate("2026-10-11")
      assertNotNull(fetched)
      assertEquals("2026-10-11", fetched?.date)
      assertEquals(2, fetched?.practiceCount)

      val progress = com.example.data.UserProgress(
        id = 1,
        currentStreak = 5,
        longestStreak = 7,
        lastPracticedDate = "2026-10-11",
        totalPracticeDays = 12
      )
      dao.saveUserProgress(progress)

      val savedProgress = dao.getUserProgress()
      // First emission check
      val current = savedProgress.first()
      assertNotNull(current)
      assertEquals(5, current?.currentStreak)
      assertEquals(7, current?.longestStreak)
    }

    db.close()
  }
}
