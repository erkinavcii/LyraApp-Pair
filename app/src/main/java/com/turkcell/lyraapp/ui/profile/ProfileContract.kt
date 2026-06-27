package com.turkcell.lyraapp.ui.profile

data class ProfileUiState(
    val displayName: String = "",
    val initials: String = "",
    val playlistCount: Int = 0,
    val isDarkMode: Boolean = true,
    val isPremium: Boolean = false,
    val membershipDaysLeft: Int? = null,
    val membershipType: String? = null,
)

sealed interface ProfileIntent {
    data object ToggleTheme : ProfileIntent
    data object OpenSettings : ProfileIntent
    data object OpenSoundQuality : ProfileIntent
    data object OpenOfflineDownload : ProfileIntent
    data object OpenNotifications : ProfileIntent
    data object OpenPrivacy : ProfileIntent
    data object OpenHelpAndSupport : ProfileIntent
    data object OpenPremium : ProfileIntent
    data object Logout : ProfileIntent
}

sealed interface ProfileEffect {
    data object NavigateToPremiumPlans : ProfileEffect
}
