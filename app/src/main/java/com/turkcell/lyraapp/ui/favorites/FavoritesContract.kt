package com.turkcell.lyraapp.ui.favorites

import com.turkcell.lyraapp.data.player.NowPlayingTrack

data class FavoritesUiState(
    val isLoading: Boolean = false,
    val tracks: List<NowPlayingTrack> = emptyList(),
    val trackCountText: String = "",
    val totalDurationText: String = "",
    val currentTrack: NowPlayingTrack? = null,
)

sealed interface FavoritesIntent {
    data object PlayAll : FavoritesIntent
    data object ShufflePlay : FavoritesIntent
    data object Download : FavoritesIntent
    data class PlayTrack(val track: NowPlayingTrack) : FavoritesIntent
    data class ToggleFavorite(val track: NowPlayingTrack) : FavoritesIntent
    data object NavigateBack : FavoritesIntent
    data object OpenNowPlaying : FavoritesIntent
}

sealed interface FavoritesEffect {
    data object NavigateBack : FavoritesEffect
    data object NavigateToNowPlaying : FavoritesEffect
}
