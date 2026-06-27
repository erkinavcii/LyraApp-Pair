package com.turkcell.lyraapp.ui.navigation

enum class LyraDestination(val route: String) {
    Login("login"),
    Register("register"),
    Otp("otp/{phoneNumber}"),
    CompleteProfile("complete_profile"),
    Home("home"),
    Search("search"),
    Library("library"),
    Favorites("favorites"),
    Profile("profile"),
    NowPlaying("now_playing"),
    PlaylistDetail("playlist_detail/{playlistId}"),
    NewPlaylist("new_playlist"),
    PremiumPlans("premium_plans"),
    Payment("payment/{planId}"),
}