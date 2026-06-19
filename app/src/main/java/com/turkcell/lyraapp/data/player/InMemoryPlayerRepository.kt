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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var queue: List<NowPlayingTrack> = emptyList()
    private var currentIndex: Int = -1

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private fun releasePlayer() {
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
                // Fetch the signed streaming URL from the API
                val response = withContext(Dispatchers.IO) {
                    apiService.getStreamUrl(track.id)
                }
                val streamUrl = response.data.url

                // Initialize MediaPlayer
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(streamUrl)
                    setOnPreparedListener {
                        start()
                        _isPlaying.value = true
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
            }
        } else {
            val track = _currentTrack.value
            if (track != null) {
                playUrl(track)
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
}
