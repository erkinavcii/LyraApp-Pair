package com.turkcell.lyraapp.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.lyraapp.data.favorites.FavoritesRepository
import com.turkcell.lyraapp.data.player.NowPlayingTrack
import com.turkcell.lyraapp.data.player.PlayerRepository
import com.turkcell.lyraapp.data.theme.ThemeRepository
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
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val playerRepository: PlayerRepository,
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private val _effect = Channel<FavoritesEffect>(Channel.BUFFERED)
    val effect: Flow<FavoritesEffect> = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.favorites.collect { tracks ->
                val totalMs = tracks.sumOf { it.durationMs }
                val totalMin = totalMs / 60_000
                _uiState.update {
                    it.copy(
                        tracks = tracks,
                        trackCountText = "${tracks.size} şarkı",
                        totalDurationText = "$totalMin dk",
                    )
                }
            }
        }
        viewModelScope.launch {
            playerRepository.currentTrack.collect { track ->
                _uiState.update { it.copy(currentTrack = track) }
            }
        }
        viewModelScope.launch {
            themeRepository.isDarkMode.collect { isDark ->
                _uiState.update { it.copy(isDarkMode = isDark) }
            }
        }
    }

    fun onIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.PlayAll -> playAll()
            is FavoritesIntent.ShufflePlay -> shufflePlay()
            is FavoritesIntent.Download -> downloadAllFavorites()
            is FavoritesIntent.PlayTrack -> playTrack(intent.track)
            is FavoritesIntent.ToggleFavorite -> favoritesRepository.toggleFavorite(intent.track)
            is FavoritesIntent.NavigateBack -> viewModelScope.launch { _effect.send(FavoritesEffect.NavigateBack) }
            is FavoritesIntent.OpenNowPlaying -> viewModelScope.launch { _effect.send(FavoritesEffect.NavigateToNowPlaying) }
        }
    }

    private fun downloadAllFavorites() {
        viewModelScope.launch {
            val tracks = _uiState.value.tracks
            tracks.forEach { track ->
                try {
                    playerRepository.downloadTrack(track.id)
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun playAll() {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return
        playerRepository.playQueue(tracks, 0)
        viewModelScope.launch { _effect.send(FavoritesEffect.NavigateToNowPlaying) }
    }

    private fun shufflePlay() {
        val tracks = _uiState.value.tracks.shuffled()
        if (tracks.isEmpty()) return
        playerRepository.playQueue(tracks, 0)
        viewModelScope.launch { _effect.send(FavoritesEffect.NavigateToNowPlaying) }
    }

    private fun playTrack(track: NowPlayingTrack) {
        val tracks = _uiState.value.tracks
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerRepository.playQueue(tracks, index)
        viewModelScope.launch { _effect.send(FavoritesEffect.NavigateToNowPlaying) }
    }
}
