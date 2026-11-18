package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CommandHistoryEntity::class], version = 1, exportSchema = false)
abstract class CommandHistoryDatabase : RoomDatabase() {
    abstract fun commandHistoryDao(): CommandHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CommandHistoryDatabase? = null

        fun getInstance(context: Context): CommandHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CommandHistoryDatabase::class.java,
                    "baby_command_history.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
