package com.turkcell.lyraapp.ui.home

import com.turkcell.lyraapp.data.home.PlaylistForYou
import com.turkcell.lyraapp.data.home.QuickPick
import com.turkcell.lyraapp.data.home.RecentlyPlayed
import com.turkcell.lyraapp.data.player.NowPlayingTrack


data class HomeUiState(
    val isLoading: Boolean = false,
    val isDarkMode: Boolean = true,
    val greeting: String = "",
    val userInitials: String = "",
    val quickPicks: List<QuickPick> = emptyList(),
    val recentlyPlayed: List<RecentlyPlayed> = emptyList(),
    val playlistsForYou: List<PlaylistForYou> = emptyList(),
    val currentTrack: NowPlayingTrack? = null,
    val isCurrentTrackFavorited: Boolean = false,
    val isPlaying: Boolean = false,
)

sealed interface HomeIntent {
    data object Retry : HomeIntent
    data object Logout : HomeIntent
    data object ToggleTheme : HomeIntent
    data class PlayTrack(val track: NowPlayingTrack, val queue: List<NowPlayingTrack>) : HomeIntent
    data object OpenNowPlaying : HomeIntent
    data object SkipNext : HomeIntent
    data object ToggleFavorite : HomeIntent
    data object TogglePlayPause : HomeIntent
    data class PlaylistClicked(val playlistId: String) : HomeIntent
}

sealed interface HomeEffect {
    data class ShowError(val message: String) : HomeEffect
    data object NavigateToLogin : HomeEffect
    data object NavigateToNowPlaying : HomeEffect
    data class NavigateToPlaylistDetail(val playlistId: String) : HomeEffect
}