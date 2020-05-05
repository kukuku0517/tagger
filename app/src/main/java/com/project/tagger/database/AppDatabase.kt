package com.project.tagger.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PhotoPojo::class,
        TagPOJO::class,
        PhotoTagJoin::class,
        RepoPojo::class,
        RepoUserJoin::class,
        UserPojo::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun photoDao(): PhotoDao
    abstract fun tagDao(): TagDao
    abstract fun repoDao(): RepoDao

    companion object {
        fun getInstance(context: Context): AppDatabase {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java, "tagger"
            )
                .addMigrations(MIGRATION_0)
                .build()
            return db
        }

        private val MIGRATION_0: Migration = object : Migration(1,2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE photo ADD COLUMN createdAt TEXT NOT NULL default '2020-05-05T00:00:00'")
                database.execSQL("ALTER TABLE photo ADD COLUMN updatedAt TEXT NOT NULL default '2020-05-05T00:00:00'")
                database.execSQL("ALTER TABLE photo ADD COLUMN usedAt TEXT NOT NULL default '2020-05-05T00:00:00'")
                database.execSQL("ALTER TABLE photo ADD COLUMN sharedCount INTEGER NOT NULL default 0")
            }
        }
    }


}

