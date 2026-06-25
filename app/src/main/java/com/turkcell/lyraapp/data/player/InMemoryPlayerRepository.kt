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

    override suspend fun downloadTrack(trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (isTrackDownloaded(trackId)) {
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
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
