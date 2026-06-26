package com.turkcell.lyraapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val startColor: Long,
    val endColor: Long,
    val playedAt: Long = System.currentTimeMillis(),
)
