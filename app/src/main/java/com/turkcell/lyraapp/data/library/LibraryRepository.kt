package com.turkcell.lyraapp.data.library

import com.turkcell.lyraapp.data.player.NowPlayingTrack
import kotlinx.coroutines.flow.StateFlow

interface LibraryRepository {
    val playlists: StateFlow<List<Playlist>>
    suspend fun getPlaylistById(id: String): Result<Playlist>
    suspend fun createPlaylist(
        name: String,
        description: String,
        isPublic: Boolean,
        tracks: List<NowPlayingTrack>
    ): Result<Playlist>
    suspend fun deletePlaylist(id: String): Result<Unit>
    suspend fun getAvailableTracks(): Result<List<NowPlayingTrack>>
}
