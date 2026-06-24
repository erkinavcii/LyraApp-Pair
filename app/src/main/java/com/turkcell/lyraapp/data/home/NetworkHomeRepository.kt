package com.turkcell.lyraapp.data.home

import com.turkcell.lyraapp.data.auth.AuthRepository
import com.turkcell.lyraapp.data.network.LyraApiService
import com.turkcell.lyraapp.data.network.RecordPlayRequest
import com.turkcell.lyraapp.data.player.NowPlayingTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class NetworkHomeRepository @Inject constructor(
    private val apiService: LyraApiService,
    private val authRepository: AuthRepository,
) : HomeRepository {

    override suspend fun getHomeFeed(): Result<HomeFeed> = withContext(Dispatchers.IO) {
        try {
            val forYouResponse = apiService.getForYou(limit = 6)
            val recentlyPlayedResponse = apiService.getRecentlyPlayed(limit = 20)
            val playlistsResponse = apiService.getPlaylists()
            val recommendationsResponse = apiService.getRecommendations(limit = 20)

            val quickPicks = forYouResponse.data.map { song ->
                val colors = NowPlayingTrack.getColorsForId(song.id)
                QuickPick(
                    id = song.id,
                    title = song.title,
                    artworkStartColor = colors.first,
                    artworkEndColor = colors.second,
                )
            }

            val recentlyPlayed = recentlyPlayedResponse.data.map { song ->
                val colors = NowPlayingTrack.getColorsForId(song.id)
                RecentlyPlayed(
                    id = song.id,
                    title = song.title,
                    subtitle = song.artist,
                    artworkStartColor = colors.first,
                    artworkEndColor = colors.second,
                )
            }

            val playlistsForYou = playlistsResponse.data.map { playlist ->
                val colors = NowPlayingTrack.getColorsForId(playlist.id)
                PlaylistForYou(
                    id = playlist.id,
                    title = playlist.name,
                    artworkStartColor = colors.first,
                    artworkEndColor = colors.second,
                )
            }

            val recommendations = recommendationsResponse.data.map { song ->
                val colors = NowPlayingTrack.getColorsForId(song.id)
                Recommendation(
                    id = song.id,
                    title = song.title,
                    subtitle = song.artist,
                    artworkStartColor = colors.first,
                    artworkEndColor = colors.second,
                )
            }

            val userName = authRepository.getLoggedInUserName()
            val initials = if (!userName.isNullOrBlank()) {
                userName.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercase() }
                    .joinToString("")
            } else {
                "?"
            }

            Result.success(
                HomeFeed(
                    userInitials = initials,
                    quickPicks = quickPicks,
                    recentlyPlayed = recentlyPlayed,
                    playlistsForYou = playlistsForYou,
                    recommendations = recommendations,
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordPlay(songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.recordPlay(RecordPlayRequest(songId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
