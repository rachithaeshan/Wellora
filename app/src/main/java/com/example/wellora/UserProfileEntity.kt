package com.example.wellora

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String = "",
    var email: String = "",
    var password: String = "",
    var age: Int = 0,
    var habitsGoal: Int = 4,
    var waterGoal: Int = 8
)
