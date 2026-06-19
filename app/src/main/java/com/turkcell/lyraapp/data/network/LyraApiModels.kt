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
