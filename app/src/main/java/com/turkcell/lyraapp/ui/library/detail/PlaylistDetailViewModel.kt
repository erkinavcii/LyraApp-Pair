package com.turkcell.lyraapp.ui.library.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.lyraapp.data.auth.AuthRepository
import com.turkcell.lyraapp.data.favorites.FavoritesRepository
import com.turkcell.lyraapp.data.library.LibraryRepository
import com.turkcell.lyraapp.data.library.Playlist
import com.turkcell.lyraapp.data.library.PlaylistFavoritesRepository
import com.turkcell.lyraapp.data.player.NowPlayingTrack
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
class PlaylistDetailViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playerRepository: PlayerRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playlistFavoritesRepository: PlaylistFavoritesRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    private val _effect = Channel<PlaylistDetailEffect>(Channel.BUFFERED)
    val effect: Flow<PlaylistDetailEffect> = _effect.receiveAsFlow()

    init {
        _uiState.update { it.copy(ownerName = authRepository.getLoggedInUserName() ?: "") }

        val playlistId: String? = savedStateHandle["playlistId"]
        if (playlistId != null) {
            loadPlaylist(playlistId)
        } else {
            viewModelScope.launch {
                _effect.send(PlaylistDetailEffect.ShowError("Çalma listesi ID'si bulunamadı."))
            }
        }

        // Oynatıcı bilgilerini gözlemleme
        viewModelScope.launch {
            playerRepository.currentTrack.collect { track ->
                _uiState.update { it.copy(currentTrack = track) }
            }
        }

        viewModelScope.launch {
            playerRepository.isPlaying.collect { playing ->
                _uiState.update { it.copy(isPlaying = playing) }
            }
        }

        // Favori durumlarını gözlemleme (O(1) arama performansı için Set'e dönüştürülüyor)
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favoritedTracks ->
                val favoritedIds = favoritedTracks.map { it.id }.toSet()
                _uiState.update { it.copy(favoritedTrackIds = favoritedIds) }
            }
        }

        viewModelScope.launch {
            playlistFavoritesRepository.favoritedPlaylistIds.collect { ids ->
                val playlistId = _uiState.value.playlist?.id ?: savedStateHandle["playlistId"] ?: ""
                _uiState.update { it.copy(isPlaylistFavorited = playlistId in ids) }
            }
        }
    }

    fun onIntent(intent: PlaylistDetailIntent) {
        when (intent) {
            is PlaylistDetailIntent.LoadPlaylist -> loadPlaylist(intent.playlistId)
            PlaylistDetailIntent.PlayAll -> playAll()
            PlaylistDetailIntent.ShufflePlay -> shufflePlay()
            PlaylistDetailIntent.TogglePlayPause -> playerRepository.togglePlayPause()
            is PlaylistDetailIntent.PlayTrack -> playTrack(intent.track)
            is PlaylistDetailIntent.ToggleFavorite -> favoritesRepository.toggleFavorite(intent.track)
            is PlaylistDetailIntent.RemoveTrack -> removeTrack(intent.trackId)
            PlaylistDetailIntent.DownloadClick -> downloadPlaylist()
            PlaylistDetailIntent.NavigateBack -> {
                viewModelScope.launch {
                    _effect.send(PlaylistDetailEffect.NavigateBack)
                }
            }
            PlaylistDetailIntent.TogglePlaylistFavorite -> togglePlaylistFavorite()
            PlaylistDetailIntent.ShowAddTrackSheet -> showAddTrackSheet()
            PlaylistDetailIntent.DismissAddTrackSheet -> {
                _uiState.update { it.copy(isAddTrackSheetVisible = false) }
            }
            is PlaylistDetailIntent.AddTrackToPlaylist -> addTrackToPlaylist(intent.track)
        }
    }

    private fun loadPlaylist(playlistId: String) {
        if (playlistId == "downloads") {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                playerRepository.downloadedTracks.collect { tracks ->
                    val downloadsPlaylist = Playlist(
                        id = "downloads",
                        name = "İndirilen Şarkılar",
                        description = "Cihazınıza indirilmiş ve çevrimdışı çalınabilir şarkılar.",
                        isPublic = false,
                        artworkStartColor = 0xFF34D399L,
                        artworkEndColor = 0xFF10B981L,
                        tracks = tracks,
                        isPinned = true
                    )
                    _uiState.update { it.copy(isLoading = false, playlist = downloadsPlaylist) }
                }
            }
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                libraryRepository.getPlaylistById(playlistId)
                    .onSuccess { loadedPlaylist ->
                        _uiState.update { it.copy(isLoading = false, playlist = loadedPlaylist) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.send(PlaylistDetailEffect.ShowError(error.message ?: "Çalma listesi yüklenemedi."))
                    }
            }
        }
    }

    private fun playAll() {
        val tracks = _uiState.value.playlist?.tracks ?: return
        if (tracks.isEmpty()) return
        playerRepository.playQueue(tracks, 0)
        viewModelScope.launch {
            _effect.send(PlaylistDetailEffect.NavigateToNowPlaying)
        }
    }

    private fun shufflePlay() {
        val tracks = _uiState.value.playlist?.tracks ?: return
        if (tracks.isEmpty()) return
        playerRepository.playQueue(tracks.shuffled(), 0)
        viewModelScope.launch {
            _effect.send(PlaylistDetailEffect.NavigateToNowPlaying)
        }
    }

    private fun playTrack(track: NowPlayingTrack) {
        val tracks = _uiState.value.playlist?.tracks ?: return
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerRepository.playQueue(tracks, index)
        viewModelScope.launch {
            _effect.send(PlaylistDetailEffect.NavigateToNowPlaying)
        }
    }

    private fun downloadPlaylist() {
        val tracks = _uiState.value.playlist?.tracks ?: return
        if (_uiState.value.isDownloading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }
            tracks.forEach { track ->
                try {
                    playerRepository.downloadTrack(track)
                } catch (ignored: Exception) {}
            }
            _uiState.update { it.copy(isDownloading = false) }
        }
    }

    private fun togglePlaylistFavorite() {
        val playlistId = _uiState.value.playlist?.id ?: return
        playlistFavoritesRepository.togglePlaylistFavorite(playlistId)
    }

    private fun showAddTrackSheet() {
        viewModelScope.launch {
            val currentTrackIds = _uiState.value.playlist?.tracks?.map { it.id }?.toSet() ?: emptySet()
            libraryRepository.getAvailableTracks()
                .onSuccess { all ->
                    val filtered = all.filter { it.id !in currentTrackIds }
                    _uiState.update { it.copy(availableTracks = filtered, isAddTrackSheetVisible = true) }
                }
                .onFailure { error ->
                    _effect.send(PlaylistDetailEffect.ShowError(error.message ?: "Şarkı listesi yüklenemedi."))
                }
        }
    }

    private fun addTrackToPlaylist(track: NowPlayingTrack) {
        val playlistId = _uiState.value.playlist?.id ?: return
        if (_uiState.value.isAddingTrack) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingTrack = true) }
            libraryRepository.addTrackToPlaylist(playlistId, track)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isAddingTrack = false,
                            playlist = state.playlist?.copy(tracks = state.playlist.tracks + track),
                            availableTracks = state.availableTracks.filter { it.id != track.id }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isAddingTrack = false) }
                    _effect.send(PlaylistDetailEffect.ShowError(error.message ?: "Şarkı eklenemedi."))
                }
        }
    }

    private fun removeTrack(trackId: String) {
        val playlistId = _uiState.value.playlist?.id ?: return
        if (playlistId == "downloads") {
            playerRepository.deleteDownloadedTrack(trackId)
        } else {
            viewModelScope.launch {
                libraryRepository.removeTrackFromPlaylist(playlistId, trackId)
                    .onSuccess {
                        _uiState.update { state ->
                            state.copy(
                                playlist = state.playlist?.copy(
                                    tracks = state.playlist.tracks.filter { it.id != trackId }
                                )
                            )
                        }
                    }
                    .onFailure { error ->
                        _effect.send(PlaylistDetailEffect.ShowError(error.message ?: "Şarkı çalma listesinden silinemedi."))
                    }
            }
        }
    }
}
