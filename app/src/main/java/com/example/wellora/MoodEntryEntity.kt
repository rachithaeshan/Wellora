package com.example.wellora

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val emoji: String,
    val moodValue: Int,
    val note: String?,
    val timestamp: Long = System.currentTimeMillis()
)
