package com.pixijoy.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, MediaRefEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun projects(): ProjectDao

    companion object {
        @Volatile
        private var instance: StudioDatabase? = null

        fun get(context: Context): StudioDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "pixijoy_studio.db"
                ).build().also { instance = it }
            }
        }
    }
}
