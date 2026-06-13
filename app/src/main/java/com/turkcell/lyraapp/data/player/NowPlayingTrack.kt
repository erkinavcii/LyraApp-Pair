package com.turkcell.lyraapp.data.player

data class NowPlayingTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val startColor: Long,
    val endColor: Long,
    val durationMs: Long = 223_000L,
)
