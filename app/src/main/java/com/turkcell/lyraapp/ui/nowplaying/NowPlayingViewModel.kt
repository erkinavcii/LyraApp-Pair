package com.turkcell.lyraapp.ui.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(NowPlayingUiState())
    val uiState: StateFlow<NowPlayingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<NowPlayingEffect>(Channel.BUFFERED)
    val effect: Flow<NowPlayingEffect> = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            playerRepository.currentTrack.collect { track ->
                _uiState.update { state ->
                    state.copy(
                        track = track,
                        currentPositionMs = (track?.durationMs?.times(state.progress))?.toLong() ?: 0L,
                    )
                }
            }
        }
    }

    fun onIntent(intent: NowPlayingIntent) {
        when (intent) {
            is NowPlayingIntent.TogglePlayPause -> _uiState.update { it.copy(isPlaying = !it.isPlaying) }
            is NowPlayingIntent.ToggleFavorite -> _uiState.update { it.copy(isFavorited = !it.isFavorited) }
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
            is NowPlayingIntent.Dismiss -> viewModelScope.launch { _effect.send(NowPlayingEffect.NavigateBack) }
        }
    }

    private fun seekTo(progress: Float) {
        _uiState.update { state ->
            val clamped = progress.coerceIn(0f, 1f)
            state.copy(
                progress = clamped,
                currentPositionMs = ((state.track?.durationMs ?: 0L) * clamped).toLong(),
            )
        }
    }
}
