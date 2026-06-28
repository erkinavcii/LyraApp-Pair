package com.turkcell.lyraapp.data.library

import android.content.Context
import com.turkcell.lyraapp.data.network.LyraApiService
import com.turkcell.lyraapp.data.network.PlaylistDto
import com.turkcell.lyraapp.data.network.PlaylistDetailDto
import com.turkcell.lyraapp.data.network.SongDto
import com.turkcell.lyraapp.data.network.CreatePlaylistRequest
import com.turkcell.lyraapp.data.network.AddTrackRequest
import com.turkcell.lyraapp.data.player.NowPlayingTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkLibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: LyraApiService
) : LibraryRepository {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    override val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        fetchPlaylists()
    }

    private fun fetchPlaylists() {
        repositoryScope.launch {
            try {
                val publicPlaylists = try {
                    apiService.getPlaylists().data
                } catch (e: Exception) {
                    emptyList()
                }

                val userPlaylists = try {
                    apiService.getUserPlaylists().data
                } catch (e: Exception) {
                    emptyList()
                }

                val userPlaylistIds = userPlaylists.map { it.id }.toSet()
                val allPlaylistsDto = (publicPlaylists + userPlaylists).distinctBy { it.id }

                val mapped = allPlaylistsDto.map { playlistDto ->
                    val isOwnedByUser = playlistDto.id in userPlaylistIds
                    val detail = try {
                        apiService.getPlaylistDetail(playlistDto.id).data
                    } catch (e: Exception) {
                        null
                    }
                    val tracks = detail?.songs?.map { it.toDomain() } ?: emptyList()
                    playlistDto.toDomain(tracks, isOwnedByUser)
                }

                _playlists.value = mapped
            } catch (e: Exception) {
                android.util.Log.e("NetworkLibraryRepository", "fetchPlaylists error", e)
            }
        }
    }

    override suspend fun getPlaylistById(id: String): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val isOwnedByUser = _playlists.value.find { it.id == id }?.isOwnedByUser ?: false
            val response = apiService.getPlaylistDetail(id)
            val detail = response.data
            val playlist = detail.toDomain(isOwnedByUser)
            _playlists.update { current ->
                val exists = current.any { it.id == id }
                if (exists) {
                    current.map { if (it.id == id) playlist else it }
                } else {
                    current + playlist
                }
            }
            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createPlaylist(
        name: String,
        description: String,
        isPublic: Boolean,
        tracks: List<NowPlayingTrack>
    ): Result<Playlist> = withContext(Dispatchers.IO) {
        if (name.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Çalma listesi adı boş olamaz."))
        }
        try {
            val createResponse = apiService.createPlaylist(
                CreatePlaylistRequest(name = name.trim(), description = description.trim())
            )
            val createdPlaylistId = createResponse.data.id

            tracks.forEach { track ->
                try {
                    apiService.addTrackToPlaylist(createdPlaylistId, AddTrackRequest(track.id))
                } catch (e: Exception) {
                    android.util.Log.e("NetworkLibraryRepository", "addTrack error: ${track.id}", e)
                }
            }

            val finalResponse = apiService.getPlaylistDetail(createdPlaylistId)
            val finalPlaylist = finalResponse.data.toDomain(isOwnedByUser = true)

            _playlists.update { current ->
                listOf(finalPlaylist) + current
            }

            Result.success(finalPlaylist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePlaylist(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deletePlaylist(id)
            if (response.data.deleted) {
                _playlists.update { current ->
                    current.filter { it.id != id }
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Çalma listesi silinemedi."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailableTracks(): Result<List<NowPlayingTrack>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSongs(limit = 100)
            Result.success(response.data.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addTrackToPlaylist(playlistId: String, track: NowPlayingTrack): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.addTrackToPlaylist(playlistId, AddTrackRequest(track.id))
            if (!response.isSuccessful) {
                val errorMessage = when (response.code()) {
                    409 -> "Bu şarkı zaten çalma listesinde bulunuyor."
                    403 -> "Bu çalma listesine şarkı ekleyemezsiniz."
                    404 -> "Çalma listesi veya şarkı bulunamadı."
                    else -> "Şarkı eklenemedi."
                }
                return@withContext Result.failure(Exception(errorMessage))
            }
            _playlists.update { current ->
                current.map { playlist ->
                    if (playlist.id == playlistId) {
                        playlist.copy(tracks = playlist.tracks + track)
                    } else {
                        playlist
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.removeTrackFromPlaylist(playlistId, songId)
            _playlists.update { current ->
                current.map { playlist ->
                    if (playlist.id == playlistId) {
                        playlist.copy(tracks = playlist.tracks.filter { it.id != songId })
                    } else {
                        playlist
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun SongDto.toDomain(): NowPlayingTrack {
        val colors = NowPlayingTrack.getColorsForId(id)
        return NowPlayingTrack(
            id = id,
            title = title,
            subtitle = artist,
            startColor = colors.first,
            endColor = colors.second,
            durationMs = durationMs
        )
    }

    private fun PlaylistDto.toDomain(
        tracks: List<NowPlayingTrack> = emptyList(),
        isOwnedByUser: Boolean = false
    ): Playlist {
        val colors = NowPlayingTrack.getColorsForId(id)
        return Playlist(
            id = id,
            name = name,
            description = description ?: "",
            isPublic = true,
            artworkStartColor = colors.first,
            artworkEndColor = colors.second,
            tracks = tracks,
            isOwnedByUser = isOwnedByUser
        )
    }

    private fun PlaylistDetailDto.toDomain(isOwnedByUser: Boolean = false): Playlist {
        val colors = NowPlayingTrack.getColorsForId(id)
        return Playlist(
            id = id,
            name = name,
            description = description ?: "",
            isPublic = true,
            artworkStartColor = colors.first,
            artworkEndColor = colors.second,
            tracks = songs.map { it.toDomain() },
            isOwnedByUser = isOwnedByUser
        )
    }
}
