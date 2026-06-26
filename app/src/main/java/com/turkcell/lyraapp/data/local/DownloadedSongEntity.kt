package com.turkcell.lyraapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val startColor: Long,
    val endColor: Long,
    val durationMs: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
)
