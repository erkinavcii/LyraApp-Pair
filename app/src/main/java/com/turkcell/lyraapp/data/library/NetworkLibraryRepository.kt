package com.turkcell.lyraapp.data.library

import com.turkcell.lyraapp.data.network.LyraApiService
import com.turkcell.lyraapp.data.network.PlaylistDto
import com.turkcell.lyraapp.data.network.SongDto
import com.turkcell.lyraapp.data.player.NowPlayingTrack
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
                val response = apiService.getPlaylists()
                val mapped = response.data.map { playlistDto ->
                    val detail = try {
                        apiService.getPlaylistDetail(playlistDto.id).data
                    } catch (e: Exception) {
                        null
                    }
                    val tracks = detail?.songs?.map { it.toDomain() } ?: emptyList()
                    playlistDto.toDomain(tracks)
                }
                _playlists.value = mapped
            } catch (e: Exception) {
                // Fallback / log
            }
        }
    }

    override suspend fun getPlaylistById(id: String): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val local = _playlists.value.find { it.id == id }
            if (local != null && local.tracks.isNotEmpty()) {
                return@withContext Result.success(local)
            }

            val response = apiService.getPlaylistDetail(id)
            val detail = response.data
            val tracks = detail.songs.map { it.toDomain() }
            val colors = NowPlayingTrack.getColorsForId(detail.id)
            val playlist = Playlist(
                id = detail.id,
                name = detail.name,
                description = detail.description ?: "",
                isPublic = true,
                artworkStartColor = colors.first,
                artworkEndColor = colors.second,
                tracks = tracks
            )
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
    ): Result<Playlist> = withContext(Dispatchers.Default) {
        if (name.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Çalma listesi adı boş olamaz."))
        }
        val id = "pl-${System.currentTimeMillis()}"
        val colors = NowPlayingTrack.getColorsForId(id)
        val newPlaylist = Playlist(
            id = id,
            name = name.trim(),
            description = description.trim(),
            isPublic = isPublic,
            artworkStartColor = colors.first,
            artworkEndColor = colors.second,
            tracks = tracks,
            isPinned = false
        )
        _playlists.update { current ->
            listOf(newPlaylist) + current
        }
        Result.success(newPlaylist)
    }

    override suspend fun deletePlaylist(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        var found = false
        _playlists.update { current ->
            val updated = current.filter { it.id != id }
            if (updated.size < current.size) {
                found = true
            }
            updated
        }
        if (found) {
            Result.success(Unit)
        } else {
            Result.failure(NoSuchElementException("Çalma listesi bulunamadı."))
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

    private fun PlaylistDto.toDomain(tracks: List<NowPlayingTrack> = emptyList()): Playlist {
        val colors = NowPlayingTrack.getColorsForId(id)
        return Playlist(
            id = id,
            name = name,
            description = description ?: "",
            isPublic = true,
            artworkStartColor = colors.first,
            artworkEndColor = colors.second,
            tracks = tracks
        )
    }
}
