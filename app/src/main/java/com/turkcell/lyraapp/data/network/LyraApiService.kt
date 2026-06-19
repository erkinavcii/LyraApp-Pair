package com.turkcell.lyraapp.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LyraApiService {

    @GET("api/v1/songs")
    suspend fun getSongs(
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null,
        @Query("q") query: String? = null
    ): SongsResponse

    @GET("api/v1/songs/{id}/stream-url")
    suspend fun getStreamUrl(
        @Path("id") id: String
    ): StreamUrlResponse

    @GET("api/v1/playlists")
    suspend fun getPlaylists(): PlaylistsResponse

    @GET("api/v1/playlists/{id}")
    suspend fun getPlaylistDetail(
        @Path("id") id: String
    ): PlaylistDetailResponse
}
