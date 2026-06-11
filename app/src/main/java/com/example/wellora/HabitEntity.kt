package com.example.wellora

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String,          // changed to var
    var emoji: String,         // changed to var
    var isCompleted: Boolean = false, // changed to var
    val createdDate: Long = System.currentTimeMillis()
)
