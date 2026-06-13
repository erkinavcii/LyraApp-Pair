package com.turkcell.lyraapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.lyraapp.data.auth.AuthRepository
import com.turkcell.lyraapp.data.home.HomeRepository
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
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
    private val playerRepository: PlayerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(greeting = greetingForNow()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    init {
        loadFeed()
        observeTheme()
        observeCurrentTrack()
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Retry -> loadFeed()
            is HomeIntent.Logout -> logout()
            is HomeIntent.ToggleTheme -> toggleTheme()
            is HomeIntent.PlayTrack -> playTrack(intent.track, intent.queue)
            is HomeIntent.OpenNowPlaying -> viewModelScope.launch {
                _effect.send(HomeEffect.NavigateToNowPlaying)
            }
            is HomeIntent.SkipNext -> playerRepository.skipNext()
        }
    }

    // --- HOCANIN VERİ YÜKLEME MANTIĞI ---
    private fun loadFeed() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = homeRepository.getHomeFeed()
            _uiState.update { it.copy(isLoading = false) }

            result
                .onSuccess { feed ->
                    _uiState.update {
                        it.copy(
                            userInitials = feed.userInitials,
                            quickPicks = feed.quickPicks,
                            recentlyPlayed = feed.recentlyPlayed,
                            playlistsForYou = feed.playlistsForYou,
                        )
                    }
                }
                .onFailure { error ->
                    _effect.send(HomeEffect.ShowError(error.message ?: "Ana sayfa yüklenemedi."))
                }
        }
    }

    private fun greetingForNow(): String =
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Günaydın"
            in 12..17 -> "İyi günler"
            else -> "İyi akşamlar"
        }

    private fun observeTheme() {
        viewModelScope.launch {
            themeRepository.isDarkMode.collect { isDark ->
                _uiState.update { it.copy(isDarkMode = isDark) }
            }
        }
    }

    private fun toggleTheme() {
        themeRepository.toggleTheme()
    }

    private fun observeCurrentTrack() {
        viewModelScope.launch {
            playerRepository.currentTrack.collect { track ->
                _uiState.update { it.copy(currentTrack = track) }
            }
        }
    }

    private fun playTrack(track: NowPlayingTrack, queue: List<NowPlayingTrack>) {
        val startIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerRepository.playQueue(queue, startIndex)
        viewModelScope.launch { _effect.send(HomeEffect.NavigateToNowPlaying) }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _effect.send(HomeEffect.NavigateToLogin)
        }
    }
}