package com.turkcell.lyraapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecentlyPlayedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentlyPlayedEntity)

    @Query("SELECT * FROM recently_played ORDER BY playedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<RecentlyPlayedEntity>
}
