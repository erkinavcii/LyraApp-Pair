package com.turkcell.lyraapp.ui.library.create

import com.turkcell.lyraapp.data.player.NowPlayingTrack

data class NewPlaylistUiState(
    val name: String = "",
    val description: String = "",
    val isPublic: Boolean = true,
    val selectedTrackIds: Set<String> = emptySet(),
    val availableTracks: List<NowPlayingTrack> = emptyList(),
    val isSaveEnabled: Boolean = false, // Türetilir
    val isLoading: Boolean = false,
)

sealed interface NewPlaylistIntent {
    data class NameChanged(val value: String) : NewPlaylistIntent
    data class DescriptionChanged(val value: String) : NewPlaylistIntent
    data class PublicToggled(val value: Boolean) : NewPlaylistIntent
    data class ToggleTrackSelection(val trackId: String) : NewPlaylistIntent
    data object SavePlaylist : NewPlaylistIntent
    data object NavigateBack : NewPlaylistIntent
}

sealed interface NewPlaylistEffect {
    data object NavigateBack : NewPlaylistEffect
    data class ShowError(val message: String) : NewPlaylistEffect
}
