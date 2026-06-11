package com.example.wellora

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hydration_entries")
data class HydrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var glassesDrunk: Int,
    var reminderEnabled: Boolean,
    var reminderInterval: Int,
    val date: Long
)
