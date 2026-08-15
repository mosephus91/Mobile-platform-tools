package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val command: String,
    val timestamp: Long = System.currentTimeMillis()
)
