package com.turkcell.lyraapp.data.network

import com.google.gson.annotations.SerializedName

data class SongDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String,
    @SerializedName("durationMs") val durationMs: Long,
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("sizeBytes") val sizeBytes: Long,
    @SerializedName("createdAt") val createdAt: String,
)

data class SongsResponse(
    @SerializedName("data") val data: List<SongDto>,
    @SerializedName("nextCursor") val nextCursor: String?,
)

data class StreamUrlDto(
    @SerializedName("url") val url: String,
    @SerializedName("expiresAt") val expiresAt: String,
    @SerializedName("mimeType") val mimeType: String,
)

data class StreamUrlResponse(
    @SerializedName("data") val data: StreamUrlDto,
)

data class PlaylistDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("createdAt") val createdAt: String,
)

data class PlaylistsResponse(
    @SerializedName("data") val data: List<PlaylistDto>,
)

data class PlaylistDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("songs") val songs: List<SongDto>,
)

data class PlaylistDetailResponse(
    @SerializedName("data") val data: PlaylistDetailDto,
)

data class OtpRequest(
    @SerializedName("phone") val phone: String,
)

data class OtpRequestData(
    @SerializedName("sent") val sent: Boolean,
    @SerializedName("firstTime") val firstTime: Boolean,
)

data class OtpRequestResponse(
    @SerializedName("data") val data: OtpRequestData,
)

data class OtpVerifyRequest(
    @SerializedName("phone") val phone: String,
    @SerializedName("code") val code: String,
)

data class LyraUser(
    @SerializedName("id") val id: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("birthDate") val birthDate: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("profileCompleted") val profileCompleted: Boolean,
    @SerializedName("membership") val membership: MembershipDto? = null,
)

data class OtpVerifyData(
    @SerializedName("user") val user: LyraUser,
    @SerializedName("firstTime") val firstTime: Boolean,
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("expiresIn") val expiresIn: Int,
)

data class OtpVerifyResponse(
    @SerializedName("data") val data: OtpVerifyData,
)

data class RefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String,
)

data class RefreshData(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("expiresIn") val expiresIn: Int,
)

data class RefreshResponse(
    @SerializedName("data") val data: RefreshData,
)

data class LogoutRequest(
    @SerializedName("refreshToken") val refreshToken: String,
)

data class UpdateInfoRequest(
    @SerializedName("firstName") val firstName: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("birthDate") val birthDate: String,
)

data class UpdateInfoResponse(
    @SerializedName("data") val data: LyraUser,
)

data class RecordPlayRequest(
    @SerializedName("songId") val songId: String,
)

data class CreatePlaylistRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
)

data class AddTrackRequest(
    @SerializedName("songId") val songId: String,
)

data class UserProfileResponse(
    @SerializedName("data") val data: LyraUser,
)

// --- Memberships DTOs ---

data class MembershipPlanDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("priceKurus") val priceKurus: Long,
    @SerializedName("price") val price: Double,
    @SerializedName("currency") val currency: String,
    @SerializedName("durationDays") val durationDays: Int,
    @SerializedName("autoRenew") val autoRenew: Boolean,
)

data class MembershipPlansResponse(
    @SerializedName("data") val data: List<MembershipPlanDto>
)

data class CardDetailsDto(
    @SerializedName("number") val number: String,
    @SerializedName("expMonth") val expMonth: Int,
    @SerializedName("expYear") val expYear: Int,
    @SerializedName("cvc") val cvc: String,
    @SerializedName("holderName") val holderName: String,
)

data class CheckoutRequest(
    @SerializedName("plan") val plan: String,
    @SerializedName("card") val card: CardDetailsDto,
)

data class PaymentDto(
    @SerializedName("transactionId") val transactionId: String,
    @SerializedName("amountKurus") val amountKurus: Long,
    @SerializedName("currency") val currency: String,
)

data class MembershipDto(
    @SerializedName("planId") val planId: String,
    @SerializedName("type") val type: String,
    @SerializedName("status") val status: String,
    @SerializedName("autoRenew") val autoRenew: Boolean,
    @SerializedName("startedAt") val startedAt: String,
    @SerializedName("expiresAt") val expiresAt: String,
)

data class CheckoutDataDto(
    @SerializedName("payment") val payment: PaymentDto,
    @SerializedName("membership") val membership: MembershipDto,
)

data class CheckoutResponse(
    @SerializedName("data") val data: CheckoutDataDto
)

// --- Playback DTOs ---

data class PlaybackNextRequest(
    @SerializedName("songId") val songId: String
)

data class PlaybackNextDataDto(
    @SerializedName("type") val type: String, // "song" or "ad"
    @SerializedName("song") val song: SongDto?,
    @SerializedName("stream") val stream: StreamUrlDto?,
    @SerializedName("impressionId") val impressionId: String?,
)

data class PlaybackNextResponse(
    @SerializedName("data") val data: PlaybackNextDataDto
)

data class AdCompleteRequest(
    @SerializedName("impressionId") val impressionId: String
)

data class AdCompleteDataDto(
    @SerializedName("completed") val completed: Boolean
)

data class AdCompleteResponse(
    @SerializedName("data") val data: AdCompleteDataDto
)

// --- Delete Playlist DTOs ---

data class DeletePlaylistDataDto(
    @SerializedName("deleted") val deleted: Boolean
)

data class DeletePlaylistResponse(
    @SerializedName("data") val data: DeletePlaylistDataDto
)
