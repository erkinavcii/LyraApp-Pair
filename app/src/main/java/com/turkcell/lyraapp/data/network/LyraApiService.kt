package com.turkcell.lyraapp.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
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

    @POST("api/v1/auth/otp/request")
    suspend fun requestOtp(
        @Body request: OtpRequest
    ): OtpRequestResponse

    @POST("api/v1/auth/otp/verify")
    suspend fun verifyOtp(
        @Body request: OtpVerifyRequest
    ): OtpVerifyResponse

    @POST("api/v1/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest
    ): RefreshResponse

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest
    ): retrofit2.Response<Unit>

    @POST("api/v1/me/update-informations")
    suspend fun updateInformations(
        @Body request: UpdateInfoRequest
    ): UpdateInfoResponse

    @GET("api/v1/me/recently-played")
    suspend fun getRecentlyPlayed(
        @Query("limit") limit: Int = 20
    ): SongsResponse

    @GET("api/v1/me/for-you")
    suspend fun getForYou(
        @Query("limit") limit: Int = 20
    ): SongsResponse

    @POST("api/v1/me/plays")
    suspend fun recordPlay(
        @Body request: RecordPlayRequest
    ): retrofit2.Response<Unit>

    @GET("api/v1/me")
    suspend fun getCurrentUser(): UserProfileResponse

    @GET("api/v1/me/recommendations")
    suspend fun getRecommendations(
        @Query("limit") limit: Int = 20
    ): SongsResponse

    @GET("api/v1/me/playlists")
    suspend fun getUserPlaylists(): PlaylistsResponse

    @POST("api/v1/me/playlists")
    suspend fun createPlaylist(
        @Body request: CreatePlaylistRequest
    ): PlaylistDetailResponse

    @POST("api/v1/me/playlists/{id}/tracks")
    suspend fun addTrackToPlaylist(
        @Path("id") playlistId: String,
        @Body request: AddTrackRequest
    ): retrofit2.Response<Unit>

    @DELETE("api/v1/me/playlists/{id}/tracks/{songId}")
    suspend fun removeTrackFromPlaylist(
        @Path("id") playlistId: String,
        @Path("songId") songId: String
    ): retrofit2.Response<Unit>

    // --- Memberships Endpoints ---

    @GET("api/v1/memberships/plans")
    suspend fun getPlans(): MembershipPlansResponse

    @POST("api/v1/memberships/checkout")
    suspend fun checkout(
        @Body request: CheckoutRequest
    ): CheckoutResponse

    // --- Playback Endpoints ---

    @POST("api/v1/me/playback/next")
    suspend fun getPlaybackNext(
        @Body request: PlaybackNextRequest
    ): PlaybackNextResponse

    @POST("api/v1/me/playback/ad-complete")
    suspend fun completeAd(
        @Body request: AdCompleteRequest
    ): AdCompleteResponse

    @DELETE("api/v1/me/playlists/{id}")
    suspend fun deletePlaylist(
        @Path("id") playlistId: String
    ): DeletePlaylistResponse
}
