package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.Repository

class TutorApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "tutor_database"
        ).fallbackToDestructiveMigration().build()
        repository = Repository(database.appDao())
    }

    companion object {
        lateinit var instance: TutorApp
            private set
    }
}

