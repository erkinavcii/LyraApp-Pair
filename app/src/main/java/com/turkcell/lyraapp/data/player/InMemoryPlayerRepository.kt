package com.turkcell.lyraapp.data.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
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
    private val apiService: LyraApiService
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

    init {
        loadDownloadedTracksMetadata()
    }

    private var queue: List<NowPlayingTrack> = emptyList()
    private var currentIndex: Int = -1

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionJob: kotlinx.coroutines.Job? = null

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

            // Start background service to show media notification
            startMediaService()

            try {
                // Check if the track has been downloaded offline
                val localFile = File(context.filesDir, "offline_songs/${track.id}.mp3")
                val dataSource = if (localFile.exists() && localFile.length() > 0) {
                    localFile.absolutePath
                } else {
                    // Fetch the signed streaming URL from the API
                    val response = withContext(Dispatchers.IO) {
                        apiService.getStreamUrl(track.id)
                    }
                    response.data.url
                }

                // Initialize MediaPlayer
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
                    setOnCompletionListener {
                        skipNext()
                    }
                    setOnErrorListener { _, _, _ ->
                        _isPlaying.value = false
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                // If API fetch or player preparation fails
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
        if (queue.isNotEmpty()) {
            playUrl(queue[currentIndex])
        }
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
            if (track != null) {
                playUrl(track)
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        val mp = mediaPlayer
        if (mp != null) {
            try {
                mp.seekTo(positionMs.toInt())
                _currentPositionMs.value = positionMs
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun startPositionTracker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                val mp = mediaPlayer
                android.util.Log.d("PlayerTracker", "Tracker loop: mp is null? ${mp == null}, isPlaying: ${_isPlaying.value}")
                if (mp != null && _isPlaying.value) {
                    try {
                        val pos = mp.currentPosition.toLong()
                        android.util.Log.d("PlayerTracker", "Position updated: $pos")
                        _currentPositionMs.value = pos
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
        val file = File(context.filesDir, "offline_songs/${trackId}.mp3")
        return file.exists() && file.length() > 0
    }

    private fun loadDownloadedTracksMetadata() {
        try {
            val file = File(context.filesDir, "offline_songs/metadata.json")
            if (!file.exists()) {
                _downloadedTracks.value = emptyList()
                return
            }
            val jsonStr = file.readText()
            val jsonArray = org.json.JSONArray(jsonStr)
            val tracks = mutableListOf<NowPlayingTrack>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                
                // Verify if the actual .mp3 file exists on disk
                val mp3File = File(context.filesDir, "offline_songs/$id.mp3")
                if (mp3File.exists() && mp3File.length() > 0) {
                    tracks.add(
                        NowPlayingTrack(
                            id = id,
                            title = obj.getString("title"),
                            subtitle = obj.getString("subtitle"),
                            startColor = obj.getLong("startColor"),
                            endColor = obj.getLong("endColor"),
                            durationMs = obj.optLong("durationMs", 223_000L)
                        )
                    )
                }
            }
            _downloadedTracks.value = tracks
            
            // If some tracks were missing their files, save updated list
            if (tracks.size < jsonArray.length()) {
                saveDownloadedTracksMetadata()
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerRepository", "Failed to load downloaded tracks metadata", e)
            _downloadedTracks.value = emptyList()
        }
    }

    private fun saveDownloadedTracksMetadata() {
        try {
            val file = File(context.filesDir, "offline_songs/metadata.json")
            file.parentFile?.mkdirs()
            val jsonArray = org.json.JSONArray()
            _downloadedTracks.value.forEach { track ->
                val obj = org.json.JSONObject().apply {
                    put("id", track.id)
                    put("title", track.title)
                    put("subtitle", track.subtitle)
                    put("startColor", track.startColor)
                    put("endColor", track.endColor)
                    put("durationMs", track.durationMs)
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            android.util.Log.e("PlayerRepository", "Failed to save downloaded tracks metadata", e)
        }
    }

    override suspend fun downloadTrack(track: NowPlayingTrack): Result<Unit> = withContext(Dispatchers.IO) {
        val trackId = track.id
        if (isTrackDownloaded(trackId)) {
            if (!_downloadedTracks.value.any { it.id == trackId }) {
                _downloadedTracks.update { it + track }
                saveDownloadedTracksMetadata()
            }
            return@withContext Result.success(Unit)
        }

        _downloadingTrackIds.update { it + trackId }
        try {
            // 1. Get stream URL
            val response = apiService.getStreamUrl(trackId)
            val streamUrl = response.data.url

            // 2. Download the bytes
            val url = URL(streamUrl)
            val connection = url.openConnection()
            connection.connect()

            val input = connection.getInputStream()
            val outputFile = File(context.filesDir, "offline_songs/${trackId}.mp3")
            outputFile.parentFile?.mkdirs()

            val output = FileOutputStream(outputFile)
            val data = ByteArray(4096)
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                output.write(data, 0, count)
            }

            output.flush()
            output.close()
            input.close()

            // 3. Add to downloaded tracks and save metadata
            _downloadedTracks.update { currentList ->
                if (!currentList.any { it.id == trackId }) {
                    currentList + track
                } else {
                    currentList
                }
            }
            saveDownloadedTracksMetadata()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PlayerRepository", "Download failed for track $trackId", e)
            // Cleanup failed file if exists
            try {
                val file = File(context.filesDir, "offline_songs/${trackId}.mp3")
                if (file.exists()) file.delete()
            } catch (ignored: Exception) {}
            Result.failure(e)
        } finally {
            _downloadingTrackIds.update { it - trackId }
        }
    }

    override fun deleteDownloadedTrack(trackId: String): Boolean {
        val file = File(context.filesDir, "offline_songs/${trackId}.mp3")
        val deleted = if (file.exists()) {
            file.delete()
        } else {
            false
        }
        
        _downloadedTracks.update { currentList ->
            currentList.filter { it.id != trackId }
        }
        saveDownloadedTracksMetadata()
        
        return deleted
    }
}
