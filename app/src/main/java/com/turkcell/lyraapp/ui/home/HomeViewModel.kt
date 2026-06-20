package com.turkcell.lyraapp.ui.home

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.lyraapp.data.auth.AuthRepository
import com.turkcell.lyraapp.data.favorites.FavoritesRepository
import com.turkcell.lyraapp.data.home.HomeRepository
import com.turkcell.lyraapp.data.player.NowPlayingTrack
import com.turkcell.lyraapp.data.player.PlayerRepository
import com.turkcell.lyraapp.data.theme.ThemeRepository
import com.turkcell.lyraapp.service.MediaPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val homeRepository: HomeRepository,
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
    private val playerRepository: PlayerRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(greeting = greetingForNow()))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
    val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

    init {
        loadFeed()
        observeTheme()
        observeCurrentTrack()
        observeIsPlaying()
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
            is HomeIntent.TogglePlayPause -> playerRepository.togglePlayPause()
            is HomeIntent.ToggleFavorite -> {
                val track = _uiState.value.currentTrack ?: return
                favoritesRepository.toggleFavorite(track)
            }
            is HomeIntent.PlaylistClicked -> viewModelScope.launch {
                _effect.send(HomeEffect.NavigateToPlaylistDetail(intent.playlistId))
            }
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

    private fun observeIsPlaying() {
        viewModelScope.launch {
            playerRepository.isPlaying.collect { playing ->
                _uiState.update { state -> state.copy(isPlaying = playing) }
            }
        }
    }

    private fun observeCurrentTrack() {
        viewModelScope.launch {
            playerRepository.currentTrack.collect { track ->
                val isFav = if (track != null) favoritesRepository.isFavorite(track.id) else false
                _uiState.update { state -> state.copy(currentTrack = track, isCurrentTrackFavorited = isFav) }
            }
        }
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favorites ->
                val currentId = _uiState.value.currentTrack?.id
                val isFav = currentId != null && favorites.any { fav -> fav.id == currentId }
                _uiState.update { state -> state.copy(isCurrentTrackFavorited = isFav) }
            }
        }
        viewModelScope.launch {
            playerRepository.isPlaying.collect { playing: Boolean ->
                _uiState.update { it.copy(isPlaying = playing) }
            }
        }
    }

    private fun playTrack(track: NowPlayingTrack, queue: List<NowPlayingTrack>) {
        val startIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerRepository.playQueue(queue, startIndex)
        startMediaService()
        viewModelScope.launch { _effect.send(HomeEffect.NavigateToNowPlaying) }
    }

    private fun startMediaService() {
        val intent = Intent(context, MediaPlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _effect.send(HomeEffect.NavigateToLogin)
        }
    }
}