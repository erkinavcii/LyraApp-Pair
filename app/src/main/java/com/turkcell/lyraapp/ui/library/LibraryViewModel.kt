package com.turkcell.lyraapp.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.lyraapp.data.favorites.FavoritesRepository
import com.turkcell.lyraapp.data.library.LibraryRepository
import com.turkcell.lyraapp.data.player.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _effect = Channel<LibraryEffect>(Channel.BUFFERED)
    val effect: Flow<LibraryEffect> = _effect.receiveAsFlow()

    init {
        // Playlists flow gözlemleniyor
        viewModelScope.launch {
            libraryRepository.playlists.collect { list ->
                updateState { it.copy(playlists = list) }
            }
        }

        // Favorites flow gözlemlenerek "Beğenilen Şarkılar" sayısının canlı kalması sağlanıyor
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favoritedTracks ->
                updateState { it.copy(favoritesCount = favoritedTracks.size) }
            }
        }

        // Downloaded tracks flow gözlemlenerek "İndirilen Şarkılar" sayısının canlı kalması sağlanıyor
        viewModelScope.launch {
            playerRepository.downloadedTracks.collect { downloadedTracks ->
                updateState { it.copy(downloadedCount = downloadedTracks.size) }
            }
        }
    }

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.SelectTab -> updateState { it.copy(selectedTab = intent.tab) }
            is LibraryIntent.ToggleViewMode -> updateState { it.copy(isGridView = !it.isGridView) }
            is LibraryIntent.SearchQueryChanged -> updateState { it.copy(searchQuery = intent.query) }
            is LibraryIntent.ToggleSearchActive -> updateState { it.copy(isSearchActive = intent.active, searchQuery = "") }
            is LibraryIntent.ClickPlaylist -> {
                viewModelScope.launch {
                    _effect.send(LibraryEffect.NavigateToPlaylistDetail(intent.playlistId))
                }
            }
            is LibraryIntent.ClickFavorites -> {
                viewModelScope.launch {
                    _effect.send(LibraryEffect.NavigateToFavorites)
                }
            }
            is LibraryIntent.CreatePlaylistClick -> {
                viewModelScope.launch {
                    _effect.send(LibraryEffect.NavigateToNewPlaylist)
                }
            }
            is LibraryIntent.DeletePlaylist -> {
                viewModelScope.launch {
                    libraryRepository.deletePlaylist(intent.playlistId)
                        .onFailure {
                            _effect.send(LibraryEffect.ShowError(it.message ?: "Silme işlemi başarısız."))
                        }
                }
            }
        }
    }

    private fun updateState(transform: (LibraryUiState) -> LibraryUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            updated.copy(
                filteredPlaylists = updated.playlists.filter {
                    it.name.contains(updated.searchQuery, ignoreCase = true) ||
                    it.description.contains(updated.searchQuery, ignoreCase = true)
                }
            )
        }
    }
}
