package com.turkcell.lyraapp.data.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import com.turkcell.lyraapp.data.local.DownloadedSongDao
import com.turkcell.lyraapp.data.local.DownloadedSongEntity
import com.turkcell.lyraapp.data.network.LyraApiService
import com.turkcell.lyraapp.service.MediaPlayerService
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
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryPlayerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: LyraApiService,
    private val downloadedSongDao: DownloadedSongDao,
) : PlayerRepository {

    private val _currentTrack = MutableStateFlow<NowPlayingTrack?>(null)
    override val currentTrack: StateFlow<NowPlayingTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    override val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    private val _downloadedTracks = MutableStateFlow<List<NowPlayingTrack>>(emptyList())
    override val downloadedTracks: StateFlow<List<NowPlayingTrack>> = _downloadedTracks.asStateFlow()

    private var queue: List<NowPlayingTrack> = emptyList()
    private var currentIndex: Int = -1

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionJob: kotlinx.coroutines.Job? = null

    init {
        scope.launch {
            migrateJsonToRoomIfNeeded()
            loadDownloadedTracksFromRoom()
        }
    }

    private suspend fun migrateJsonToRoomIfNeeded() = withContext(Dispatchers.IO) {
        val jsonFile = File(context.filesDir, "offline_songs/metadata.json")
        if (!jsonFile.exists()) return@withContext
        try {
            val jsonStr = jsonFile.readText()
            val jsonArray = org.json.JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val mp3File = File(context.filesDir, "offline_songs/$id.mp3")
                if (mp3File.exists() && mp3File.length() > 0) {
                    downloadedSongDao.insert(
                        DownloadedSongEntity(
                            id = id,
                            title = obj.getString("title"),
                            subtitle = obj.getString("subtitle"),
                            startColor = obj.getLong("startColor"),
                            endColor = obj.getLong("endColor"),
                            durationMs = obj.optLong("durationMs", 223_000L),
                        )
                    )
                }
            }
            jsonFile.delete()
        } catch (e: Exception) {
            android.util.Log.e("PlayerRepository", "JSON migration failed", e)
        }
    }

    private suspend fun loadDownloadedTracksFromRoom() = withContext(Dispatchers.IO) {
        val entities = downloadedSongDao.getAll()
        val tracks = entities
            .filter { File(context.filesDir, "offline_songs/${it.id}.mp3").let { f -> f.exists() && f.length() > 0 } }
            .map { it.toNowPlayingTrack() }
        _downloadedTracks.value = tracks
    }

    private fun releasePlayer() {
        positionJob?.cancel()
        _currentPositionMs.value = 0L
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    private fun playUrl(track: NowPlayingTrack) {
        scope.launch {
            _isPlaying.value = false
            releasePlayer()
            _currentTrack.value = track

            startMediaService()

            try {
                val localFile = File(context.filesDir, "offline_songs/${track.id}.mp3")
                val dataSource = if (localFile.exists() && localFile.length() > 0) {
                    localFile.absolutePath
                } else {
                    val response = withContext(Dispatchers.IO) { apiService.getStreamUrl(track.id) }
                    response.data.url
                }

                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(dataSource)
                    setOnPreparedListener {
                        start()
                        _isPlaying.value = true
                        startPositionTracker()
                    }
                    setOnCompletionListener { skipNext() }
                    setOnErrorListener { _, _, _ ->
                        _isPlaying.value = false
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                _isPlaying.value = false
            }
        }
    }

    private fun startMediaService() {
        val intent = Intent(context, MediaPlayerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun play(track: NowPlayingTrack) {
        queue = listOf(track)
        currentIndex = 0
        playUrl(track)
    }

    override fun playQueue(tracks: List<NowPlayingTrack>, startIndex: Int) {
        queue = tracks
        currentIndex = startIndex.coerceIn(0, tracks.lastIndex)
        if (queue.isNotEmpty()) playUrl(queue[currentIndex])
    }

    override fun togglePlayPause() {
        val mp = mediaPlayer
        if (mp != null) {
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
            } else {
                mp.start()
                _isPlaying.value = true
                startPositionTracker()
            }
        } else {
            val track = _currentTrack.value
            if (track != null) playUrl(track)
        }
    }

    override fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun startPositionTracker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                val mp = mediaPlayer
                if (mp != null && _isPlaying.value) {
                    try {
                        _currentPositionMs.value = mp.currentPosition.toLong()
                    } catch (e: Exception) {
                        android.util.Log.e("PlayerTracker", "Error reading position", e)
                    }
                }
                kotlinx.coroutines.delay(250)
            }
        }
    }

    override fun skipNext() {
        if (queue.isEmpty()) return
        currentIndex = (currentIndex + 1) % queue.size
        playUrl(queue[currentIndex])
    }

    override fun skipPrevious() {
        if (queue.isEmpty()) return
        currentIndex = (currentIndex - 1 + queue.size) % queue.size
        playUrl(queue[currentIndex])
    }

    override fun isTrackDownloaded(trackId: String): Boolean {
        val file = File(context.filesDir, "offline_songs/$trackId.mp3")
        return file.exists() && file.length() > 0
    }

    override suspend fun downloadTrack(track: NowPlayingTrack): Result<Unit> = withContext(Dispatchers.IO) {
        val trackId = track.id
        if (isTrackDownloaded(trackId)) {
            if (!_downloadedTracks.value.any { it.id == trackId }) {
                downloadedSongDao.insert(track.toEntity())
                _downloadedTracks.update { it + track }
            }
            return@withContext Result.success(Unit)
        }

        _downloadingTrackIds.update { it + trackId }
        try {
            val streamUrl = apiService.getStreamUrl(trackId).data.url

            val url = URL(streamUrl)
            val connection = url.openConnection()
            connection.connect()

            val outputFile = File(context.filesDir, "offline_songs/$trackId.mp3")
            outputFile.parentFile?.mkdirs()

            connection.getInputStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(4096)
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        output.write(buffer, 0, count)
                    }
                }
            }

            downloadedSongDao.insert(track.toEntity())
            _downloadedTracks.update { current ->
                if (!current.any { it.id == trackId }) current + track else current
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PlayerRepository", "Download failed for $trackId", e)
            try { File(context.filesDir, "offline_songs/$trackId.mp3").delete() } catch (ignored: Exception) {}
            Result.failure(e)
        } finally {
            _downloadingTrackIds.update { it - trackId }
        }
    }

    override fun deleteDownloadedTrack(trackId: String): Boolean {
        val file = File(context.filesDir, "offline_songs/$trackId.mp3")
        val deleted = file.exists() && file.delete()
        scope.launch(Dispatchers.IO) { downloadedSongDao.deleteById(trackId) }
        _downloadedTracks.update { it.filter { t -> t.id != trackId } }
        return deleted
    }
}

private fun DownloadedSongEntity.toNowPlayingTrack() = NowPlayingTrack(
    id = id,
    title = title,
    subtitle = subtitle,
    startColor = startColor,
    endColor = endColor,
    durationMs = durationMs,
)

private fun NowPlayingTrack.toEntity() = DownloadedSongEntity(
    id = id,
    title = title,
    subtitle = subtitle,
    startColor = startColor,
    endColor = endColor,
    durationMs = durationMs,
)
