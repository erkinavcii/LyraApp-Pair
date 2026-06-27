package com.turkcell.lyraapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.lyraapp.data.auth.AuthRepository
import com.turkcell.lyraapp.data.library.LibraryRepository
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
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val themeRepository: ThemeRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = Channel<ProfileEffect>(Channel.BUFFERED)
    val effect: Flow<ProfileEffect> = _effect.receiveAsFlow()

    init {
        val name = authRepository.getLoggedInUserName().orEmpty()
        _uiState.update {
            it.copy(
                displayName = name,
                initials = computeInitials(name),
            )
        }
        viewModelScope.launch {
            themeRepository.isDarkMode.collect { isDark ->
                _uiState.update { it.copy(isDarkMode = isDark) }
            }
        }
        viewModelScope.launch {
            libraryRepository.playlists.collect { list ->
                _uiState.update { it.copy(playlistCount = list.size) }
            }
        }
        viewModelScope.launch {
            authRepository.fetchUserProfile()
                .onSuccess {
                    val updatedName = authRepository.getLoggedInUserName().orEmpty()
                    val membership = authRepository.getMembership()
                    val daysLeft = membership?.expiresAt?.let { expiresAt ->
                        runCatching {
                            val expires = OffsetDateTime.parse(expiresAt)
                            ChronoUnit.DAYS.between(OffsetDateTime.now(), expires).toInt().coerceAtLeast(0)
                        }.getOrNull()
                    }
                    _uiState.update {
                        it.copy(
                            displayName = updatedName,
                            initials = computeInitials(updatedName),
                            isPremium = membership?.status == "active",
                            membershipDaysLeft = daysLeft,
                            membershipType = membership?.type,
                        )
                    }
                }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.ToggleTheme -> themeRepository.toggleTheme()
            ProfileIntent.OpenSettings -> Unit
            ProfileIntent.OpenSoundQuality -> Unit
            ProfileIntent.OpenOfflineDownload -> Unit
            ProfileIntent.OpenNotifications -> Unit
            ProfileIntent.OpenPrivacy -> Unit
            ProfileIntent.OpenHelpAndSupport -> Unit
            ProfileIntent.OpenPremium -> viewModelScope.launch {
                _effect.send(ProfileEffect.NavigateToPremiumPlans)
            }
            ProfileIntent.Logout -> {
                viewModelScope.launch {
                    authRepository.logout()
                }
            }
        }
    }

    private fun computeInitials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotEmpty() }
        return when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}"
            parts.size == 1 -> parts[0].take(2)
            else -> "?"
        }.uppercase()
    }
}
