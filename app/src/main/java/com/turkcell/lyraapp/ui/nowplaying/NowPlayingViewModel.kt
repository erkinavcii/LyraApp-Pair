package com.turkcell.lyraapp.ui.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.lyraapp.data.favorites.FavoritesRepository
import com.turkcell.lyraapp.data.home.HomeRepository
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
class NowPlayingViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val favoritesRepository: FavoritesRepository,
    private val homeRepository: HomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<NowPlayingEffect>(Channel.BUFFERED)
    val effect: Flow<NowPlayingEffect> = _effect.receiveAsFlow()

    private var lastRecordedTrackId: String? = null

    init {
        viewModelScope.launch {
            playerRepository.currentTrack.collect { track ->
                if (track != null && track.id != lastRecordedTrackId) {
                    lastRecordedTrackId = track.id
                    recordPlay(track)
                }
                val downloaded = track?.let { playerRepository.isTrackDownloaded(it.id) } ?: false
                _uiState.update { state ->
                    state.copy(
                        track = track,
                        isFavorited = track?.let { favoritesRepository.isFavorite(it.id) } ?: false,
                        isDownloaded = downloaded,
                        currentPositionMs = 0L,
                        progress = 0f
                    )
                }
            }
        }
        viewModelScope.launch {
            playerRepository.currentPositionMs.collect { positionMs ->
                _uiState.update { state ->
                    val duration = state.track?.durationMs ?: 0L
                    val progress = if (duration > 0L) positionMs.toFloat() / duration.toFloat() else 0f
                    state.copy(
                        currentPositionMs = positionMs,
                        progress = progress.coerceIn(0f, 1f)
                    )
                }
            }
        }
        viewModelScope.launch {
            playerRepository.isPlaying.collect { playing ->
                _uiState.update { it.copy(isPlaying = playing) }
            }
        }
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favorites ->
                val currentId = _uiState.value.track?.id
                _uiState.update { it.copy(isFavorited = currentId != null && favorites.any { f -> f.id == currentId }) }
            }
        }
        viewModelScope.launch {
            playerRepository.downloadingTrackIds.collect { downloadingIds ->
                val currentId = _uiState.value.track?.id
                val downloading = currentId != null && downloadingIds.contains(currentId)
                val downloaded = currentId != null && playerRepository.isTrackDownloaded(currentId)
                _uiState.update { it.copy(isDownloading = downloading, isDownloaded = downloaded) }
            }
        }
    }

    fun onIntent(intent: NowPlayingIntent) {
        when (intent) {
            is NowPlayingIntent.TogglePlayPause -> playerRepository.togglePlayPause()
            is NowPlayingIntent.ToggleFavorite -> {
                val track = _uiState.value.track ?: return
                favoritesRepository.toggleFavorite(track)
            }
            is NowPlayingIntent.ToggleShuffle -> _uiState.update { it.copy(isShuffling = !it.isShuffling) }
            is NowPlayingIntent.ToggleRepeat -> _uiState.update { it.copy(isRepeating = !it.isRepeating) }
            is NowPlayingIntent.SeekTo -> seekTo(intent.progress)
            is NowPlayingIntent.SkipPrevious -> {
                playerRepository.skipPrevious()
                seekTo(0f)
            }
            is NowPlayingIntent.SkipNext -> {
                playerRepository.skipNext()
                seekTo(0f)
            }
            is NowPlayingIntent.DownloadClick -> downloadCurrentTrack()
            is NowPlayingIntent.Dismiss -> viewModelScope.launch { _effect.send(NowPlayingEffect.NavigateBack) }
        }
    }

    private fun recordPlay(track: com.turkcell.lyraapp.data.player.NowPlayingTrack) {
        viewModelScope.launch { homeRepository.recordPlay(track) }
    }

    private fun seekTo(progress: Float) {
        val duration = _uiState.value.track?.durationMs ?: 0L
        val position = (duration * progress.coerceIn(0f, 1f)).toLong()
        playerRepository.seekTo(position)
        _uiState.update { state ->
            val clamped = progress.coerceIn(0f, 1f)
            state.copy(
                progress = clamped,
                currentPositionMs = position,
            )
        }
    }

    private fun downloadCurrentTrack() {
        val track = _uiState.value.track ?: return
        if (_uiState.value.isDownloading) return

        viewModelScope.launch {
            if (_uiState.value.isDownloaded) {
                playerRepository.deleteDownloadedTrack(track.id)
                _uiState.update { it.copy(isDownloaded = false) }
            } else {
                playerRepository.downloadTrack(track)
                    .onSuccess {
                        _uiState.update { it.copy(isDownloaded = true) }
                    }
                    .onFailure {
                        // ignore or show error
                    }
            }
        }
    }
}
