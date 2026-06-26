package com.turkcell.lyraapp.ui.nowplaying

import com.turkcell.lyraapp.data.library.Playlist
import com.turkcell.lyraapp.data.player.NowPlayingTrack

data class NowPlayingUiState(
    val track: NowPlayingTrack? = null,
    val isPlaying: Boolean = true,
    val isFavorited: Boolean = false,
    val isShuffling: Boolean = false,
    val isRepeating: Boolean = false,
    val progress: Float = 0f,
    val currentPositionMs: Long = 0L,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val showPlaylistPicker: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
)

sealed interface NowPlayingIntent {
    data object TogglePlayPause : NowPlayingIntent
    data object ToggleFavorite : NowPlayingIntent
    data object ToggleShuffle : NowPlayingIntent
    data object ToggleRepeat : NowPlayingIntent
    data class SeekTo(val progress: Float) : NowPlayingIntent
    data object SkipPrevious : NowPlayingIntent
    data object SkipNext : NowPlayingIntent
    data object Dismiss : NowPlayingIntent
    data object DownloadClick : NowPlayingIntent
    data object AddToPlaylistClick : NowPlayingIntent
    data object DismissPlaylistPicker : NowPlayingIntent
    data class AddToPlaylist(val playlistId: String) : NowPlayingIntent
}

sealed interface NowPlayingEffect {
    data object NavigateBack : NowPlayingEffect
    data object ShowDownloadSuccess : NowPlayingEffect
    data object ShowDownloadError : NowPlayingEffect
    data class ShowAddToPlaylistSuccess(val playlistName: String) : NowPlayingEffect
    data object ShowAddToPlaylistError : NowPlayingEffect
}
