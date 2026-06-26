package com.turkcell.lyraapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DownloadedSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    suspend fun getAll(): List<DownloadedSongEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE id = :id)")
    suspend fun exists(id: String): Boolean
}
