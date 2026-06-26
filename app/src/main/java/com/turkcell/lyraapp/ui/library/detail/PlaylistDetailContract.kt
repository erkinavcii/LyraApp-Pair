package com.turkcell.lyraapp.ui.library.detail

import com.turkcell.lyraapp.data.library.Playlist
import com.turkcell.lyraapp.data.player.NowPlayingTrack

data class PlaylistDetailUiState(
    val isLoading: Boolean = false,
    val playlist: Playlist? = null,
    val isDownloading: Boolean = false,
    val currentTrack: NowPlayingTrack? = null,
    val isPlaying: Boolean = false,
    val favoritedTrackIds: Set<String> = emptySet(),
    val isPlaylistFavorited: Boolean = false,
    val isAddTrackSheetVisible: Boolean = false,
    val availableTracks: List<NowPlayingTrack> = emptyList(),
    val isAddingTrack: Boolean = false,
    val ownerName: String = "",
)

sealed interface PlaylistDetailIntent {
    data class LoadPlaylist(val playlistId: String) : PlaylistDetailIntent
    data object PlayAll : PlaylistDetailIntent
    data object ShufflePlay : PlaylistDetailIntent
    data object TogglePlayPause : PlaylistDetailIntent
    data class PlayTrack(val track: NowPlayingTrack) : PlaylistDetailIntent
    data class ToggleFavorite(val track: NowPlayingTrack) : PlaylistDetailIntent
    data class RemoveTrack(val trackId: String) : PlaylistDetailIntent
    data object DownloadClick : PlaylistDetailIntent
    data object NavigateBack : PlaylistDetailIntent
    data object TogglePlaylistFavorite : PlaylistDetailIntent
    data object ShowAddTrackSheet : PlaylistDetailIntent
    data object DismissAddTrackSheet : PlaylistDetailIntent
    data class AddTrackToPlaylist(val track: NowPlayingTrack) : PlaylistDetailIntent
}

sealed interface PlaylistDetailEffect {
    data object NavigateBack : PlaylistDetailEffect
    data object NavigateToNowPlaying : PlaylistDetailEffect
    data class ShowError(val message: String) : PlaylistDetailEffect
}
