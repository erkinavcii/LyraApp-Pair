package com.turkcell.lyraapp.data.library

import com.turkcell.lyraapp.data.player.NowPlayingTrack

data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val isPublic: Boolean,
    val artworkStartColor: Long,
    val artworkEndColor: Long,
    val tracks: List<NowPlayingTrack>,
    val isPinned: Boolean = false,
)
