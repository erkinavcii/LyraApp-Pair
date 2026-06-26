package com.turkcell.lyraapp.data.home

import com.turkcell.lyraapp.data.player.NowPlayingTrack

interface HomeRepository {
    suspend fun getHomeFeed(): Result<HomeFeed>
    suspend fun getOfflineFeed(): OfflineFeed
    suspend fun recordPlay(track: NowPlayingTrack): Result<Unit> = Result.success(Unit)
}