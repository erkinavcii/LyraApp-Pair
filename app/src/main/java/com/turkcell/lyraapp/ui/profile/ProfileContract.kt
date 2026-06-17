package com.turkcell.lyraapp.ui.profile

data class ProfileUiState(
    val displayName: String = "",
    val initials: String = "",
    val playlistCount: Int = 0,
    val isDarkMode: Boolean = true,
)

sealed interface ProfileIntent {
    data object ToggleTheme : ProfileIntent
    data object OpenSettings : ProfileIntent
    data object OpenSoundQuality : ProfileIntent
    data object OpenOfflineDownload : ProfileIntent
    data object OpenNotifications : ProfileIntent
    data object OpenPrivacy : ProfileIntent
    data object OpenHelpAndSupport : ProfileIntent
}

sealed interface ProfileEffect
