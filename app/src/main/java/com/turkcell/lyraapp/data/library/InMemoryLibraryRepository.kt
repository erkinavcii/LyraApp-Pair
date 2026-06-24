package com.turkcell.lyraapp.data.library

import com.turkcell.lyraapp.data.player.NowPlayingTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class InMemoryLibraryRepository @Inject constructor() : LibraryRepository {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    override val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init {
        // Mock çalma listelerini oluşturup yükleyelim
        _playlists.value = listOf(
            Playlist(
                id = "pl-gece-surusu",
                name = "Gece Sürüşü",
                description = "Karanlık yollar için synth-pop",
                isPublic = true,
                artworkStartColor = 0xFF8B6FB8,
                artworkEndColor = 0xFF4A3D6B,
                tracks = listOf(TRACK_1, TRACK_2, TRACK_6, TRACK_7, TRACK_3, TRACK_10),
                isPinned = false
            ),
            Playlist(
                id = "pl-sabah-kahvesi",
                name = "Sabah Kahvesi",
                description = "Güne enerjik ve taze bir başlangıç",
                isPublic = true,
                artworkStartColor = 0xFF7C83D9,
                artworkEndColor = 0xFF3E4486,
                tracks = listOf(TRACK_7, TRACK_4, TRACK_5, TRACK_9, TRACK_2),
                isPinned = false
            ),
            Playlist(
                id = "pl-odaklan",
                name = "Odaklan",
                description = "Zihninizi toplamanıza yardımcı olacak tınılar",
                isPublic = true,
                artworkStartColor = 0xFF4AC2A8,
                artworkEndColor = 0xFF1F6E5C,
                tracks = listOf(TRACK_6, TRACK_4, TRACK_8, TRACK_3, TRACK_1),
                isPinned = false
            ),
            Playlist(
                id = "pl-yaz-anilari",
                name = "Yaz Anıları",
                description = "Sıcak yaz günlerini hatırlatan ritimler",
                isPublic = true,
                artworkStartColor = 0xFF5AAFC9,
                artworkEndColor = 0xFF2A5F73,
                tracks = listOf(TRACK_5, TRACK_7, TRACK_1, TRACK_9, TRACK_10),
                isPinned = false
            ),
            Playlist(
                id = "pl-akustik-aksam",
                name = "Akustik Akşam",
                description = "Akustik gitarlar ve dingin vokaller",
                isPublic = true,
                artworkStartColor = 0xFF9B5CC4,
                artworkEndColor = 0xFF6B3494,
                tracks = listOf(TRACK_10, TRACK_5, TRACK_7, TRACK_2),
                isPinned = false
            )
        )
    }

    override suspend fun getPlaylistById(id: String): Result<Playlist> {
        delay(NETWORK_DELAY_MS)
        val playlist = _playlists.value.find { it.id == id }
        return if (playlist != null) {
            Result.success(playlist)
        } else {
            Result.failure(NoSuchElementException("Çalma listesi bulunamadı."))
        }
    }

    override suspend fun createPlaylist(
        name: String,
        description: String,
        isPublic: Boolean,
        tracks: List<NowPlayingTrack>
    ): Result<Playlist> {
        delay(NETWORK_DELAY_MS)
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Çalma listesi adı boş olamaz."))
        }

        // Rastgele kapak renkleri
        val colors = ARTWORK_COLORS.random()

        val newPlaylist = Playlist(
            id = "pl-${System.currentTimeMillis()}",
            name = name.trim(),
            description = description.trim(),
            isPublic = isPublic,
            artworkStartColor = colors.first,
            artworkEndColor = colors.second,
            tracks = tracks,
            isPinned = false
        )

        _playlists.update { current ->
            // Yeni oluşturulan liste listenin en üstüne (Beğenilen Şarkılar'dan hemen sonra) eklenir
            listOf(newPlaylist) + current
        }

        return Result.success(newPlaylist)
    }

    override suspend fun deletePlaylist(id: String): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        var found = false
        _playlists.update { current ->
            val updated = current.filter { it.id != id }
            if (updated.size < current.size) {
                found = true
            }
            updated
        }
        return if (found) {
            Result.success(Unit)
        } else {
            Result.failure(NoSuchElementException("Çalma listesi bulunamadı."))
        }
    }

    override suspend fun getAvailableTracks(): Result<List<NowPlayingTrack>> {
        delay(NETWORK_DELAY_MS / 2)
        return Result.success(ALL_TRACKS)
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, songId: String): Result<Unit> {
        delay(NETWORK_DELAY_MS)
        _playlists.update { current ->
            current.map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(tracks = playlist.tracks.filter { it.id != songId })
                } else {
                    playlist
                }
            }
        }
        return Result.success(Unit)
    }

    private companion object {
        const val NETWORK_DELAY_MS = 400L

        // PDF tasarımlarındaki şarkılarla birebir örtüşen şarkı listesi
        val TRACK_1 = NowPlayingTrack("tr-1", "Neon Sokaklar", "Şehir Işıkları", 0xFFD98E4A, 0xFF8A5526, 223000L)
        val TRACK_2 = NowPlayingTrack("tr-2", "Gece Yarısı", "Mavi Deniz", 0xFF6FBF5A, 0xFF356B2A, 214000L)
        val TRACK_3 = NowPlayingTrack("tr-3", "Yıldız Tozu", "Polaris", 0xFF3D5A80, 0xFF1B2A45, 247000L)
        val TRACK_4 = NowPlayingTrack("tr-4", "Sessiz Şehir", "Ela Tuna", 0xFF7C83D9, 0xFF3E4486, 195000L)
        val TRACK_5 = NowPlayingTrack("tr-5", "Sahil Yolu", "Kumsal", 0xFFE8956D, 0xFFC46A3E, 260000L)
        val TRACK_6 = NowPlayingTrack("tr-6", "Mor Bulutlar", "Derin Kaya", 0xFF4AC2A8, 0xFF1F6E5C, 232000L)
        val TRACK_7 = NowPlayingTrack("tr-7", "İlk Işık", "Sabah Ezgisi", 0xFF5AAFC9, 0xFF2A5F73, 220000L)
        val TRACK_8 = NowPlayingTrack("tr-8", "Kayıp Anlar", "Eko", 0xFF9B8FD9, 0xFF5B52A0, 238000L)
        val TRACK_9 = NowPlayingTrack("tr-9", "Son Tren", "Peron", 0xFF5AAFC9, 0xFF2A5F73, 217000L)
        val TRACK_10 = NowPlayingTrack("tr-10", "Akustik Akşam", "Grup Melodi", 0xFF9B5CC4, 0xFF6B3494, 252000L)

        val ALL_TRACKS = listOf(
            TRACK_1, TRACK_2, TRACK_3, TRACK_4, TRACK_5,
            TRACK_6, TRACK_7, TRACK_8, TRACK_9, TRACK_10
        )

        val ARTWORK_COLORS = listOf(
            Pair(0xFFD98E4A, 0xFF8A5526), // Orange
            Pair(0xFF6FBF5A, 0xFF356B2A), // Green
            Pair(0xFF3D5A80, 0xFF1B2A45), // Dark Blue
            Pair(0xFF7C83D9, 0xFF3E4486), // Purple
            Pair(0xFF4AC2A8, 0xFF1F6E5C), // Teal
            Pair(0xFF5AAFC9, 0xFF2A5F73), // Light Blue
            Pair(0xFF9B5CC4, 0xFF6B3494)  // Pink/Violet
        )
    }
}
