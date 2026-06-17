package com.turkcell.lyraapp.data.favorites

import com.turkcell.lyraapp.data.player.NowPlayingTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryFavoritesRepository @Inject constructor() : FavoritesRepository {

    private val _favorites = MutableStateFlow<List<NowPlayingTrack>>(emptyList())
    override val favorites: StateFlow<List<NowPlayingTrack>> = _favorites.asStateFlow()

    override fun toggleFavorite(track: NowPlayingTrack) {
        val current = _favorites.value
        _favorites.value = if (current.any { it.id == track.id }) {
            current.filter { it.id != track.id }
        } else {
            current + track
        }
    }

    override fun isFavorite(trackId: String): Boolean =
        _favorites.value.any { it.id == trackId }
}
