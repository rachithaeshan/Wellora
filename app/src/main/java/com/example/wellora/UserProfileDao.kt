package com.example.wellora

import androidx.room.*

@Dao
interface UserProfileDao {

    // Insert a new user, returns the new row ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(profile: UserProfileEntity): Long

    // Update existing user
    @Update
    suspend fun updateUser(profile: UserProfileEntity)

    // Get all users (used in ProfileSettingsActivity)
    @Query("SELECT * FROM user_profile")
    suspend fun getAllUsers(): List<UserProfileEntity>

    // Get a user by email and password for login
    @Query("SELECT * FROM user_profile WHERE email = :email AND password = :password LIMIT 1")
    suspend fun getUserByCredentials(email: String, password: String): UserProfileEntity?

    // Check if email already exists for signup
    @Query("SELECT * FROM user_profile WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserProfileEntity?
}
