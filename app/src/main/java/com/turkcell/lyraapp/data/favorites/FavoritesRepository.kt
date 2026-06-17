package com.turkcell.lyraapp.data.favorites

import com.turkcell.lyraapp.data.player.NowPlayingTrack
import kotlinx.coroutines.flow.StateFlow

interface FavoritesRepository {
    val favorites: StateFlow<List<NowPlayingTrack>>
    fun toggleFavorite(track: NowPlayingTrack)
    fun isFavorite(trackId: String): Boolean
}
