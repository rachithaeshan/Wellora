package com.example.wellora

import androidx.room.*

@Dao
interface HydrationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHydration(hydration: HydrationEntity): Long

    @Update
    suspend fun updateHydration(entry: HydrationEntity)

    @Query("SELECT * FROM hydration_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryForDate(date: Long): HydrationEntity?

    @Query("UPDATE hydration_entries SET glassesDrunk = :count WHERE date = :date")
    suspend fun updateWaterCount(date: Long, count: Int)

    @Query("DELETE FROM hydration_entries")
    suspend fun clearAll()
}
