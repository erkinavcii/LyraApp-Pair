package com.turkcell.lyraapp.ui.library

import com.turkcell.lyraapp.data.library.Playlist

enum class LibraryTab {
    Playlists,
    Artists,
    Albums
}

data class LibraryUiState(
    val isLoading: Boolean = false,
    val selectedTab: LibraryTab = LibraryTab.Playlists,
    val isGridView: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val filteredPlaylists: List<Playlist> = emptyList(), // Türetilir
    val favoritesCount: Int = 0,
)

sealed interface LibraryIntent {
    data class SelectTab(val tab: LibraryTab) : LibraryIntent
    data object ToggleViewMode : LibraryIntent
    data class SearchQueryChanged(val query: String) : LibraryIntent
    data class ToggleSearchActive(val active: Boolean) : LibraryIntent
    data class ClickPlaylist(val playlistId: String) : LibraryIntent
    data object ClickFavorites : LibraryIntent
    data object CreatePlaylistClick : LibraryIntent
}

sealed interface LibraryEffect {
    data class NavigateToPlaylistDetail(val playlistId: String) : LibraryEffect
    data object NavigateToFavorites : LibraryEffect
    data object NavigateToNewPlaylist : LibraryEffect
    data class ShowError(val message: String) : LibraryEffect
}
