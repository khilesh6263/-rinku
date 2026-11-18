package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val spokenText: String,
    val intentName: String,
    val intentLabel: String,
    val confidence: Float,
    val sourceName: String,
    val outcomeMessage: String,
    val isSuccess: Boolean,
    val languageName: String
)
