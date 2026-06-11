package com.example.wellora

import androidx.room.*

@Dao
interface MoodDao {

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    suspend fun getAllMoods(): List<MoodEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(mood: MoodEntryEntity)

    @Delete
    suspend fun deleteMood(mood: MoodEntryEntity)

    @Query("DELETE FROM mood_entries")
    suspend fun deleteAllMoods()
}
