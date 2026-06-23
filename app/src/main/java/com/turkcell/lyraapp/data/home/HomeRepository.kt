package com.turkcell.lyraapp.data.home

interface HomeRepository {
    suspend fun getHomeFeed(): Result<HomeFeed>
    suspend fun recordPlay(songId: String): Result<Unit> = Result.success(Unit)
}