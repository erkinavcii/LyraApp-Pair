package com.turkcell.lyraapp.data.home

data class HomeFeed(
    val userInitials: String,
    val quickPicks: List<QuickPick>,
    val recentlyPlayed: List<RecentlyPlayed>,
    val playlistsForYou: List<PlaylistForYou>,
    val recommendations: List<Recommendation>,
)

data class QuickPick(
    val id: String,
    val title: String,
    val artworkStartColor: Long,
    val artworkEndColor: Long,
)

data class RecentlyPlayed(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkStartColor: Long,
    val artworkEndColor: Long,
)

data class PlaylistForYou(
    val id: String,
    val title: String,
    val artworkStartColor: Long,
    val artworkEndColor: Long,
)

data class Recommendation(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkStartColor: Long,
    val artworkEndColor: Long,
)

data class OfflineFeed(
    val recentlyPlayed: List<RecentlyPlayed>,
    val downloadedSongs: List<QuickPick>,
)
