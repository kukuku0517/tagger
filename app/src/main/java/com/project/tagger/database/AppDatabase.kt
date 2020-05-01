package com.project.tagger.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PhotoPojo::class, TagPOJO::class, PhotoTagJoin::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun photoDao(): PhotoDao
    abstract fun tagDao(): TagDao

    companion object {
        fun getInstance(context: Context): AppDatabase {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java, "tagger"
            ).build()
            return db
        }
    }
}

