package com.turkcell.lyraapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.turkcell.lyraapp.MainActivity
import com.turkcell.lyraapp.R
import com.turkcell.lyraapp.data.favorites.FavoritesRepository
import com.turkcell.lyraapp.data.player.NowPlayingTrack
import com.turkcell.lyraapp.data.player.PlayerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MediaPlayerService : Service() {

    @Inject
    lateinit var playerRepository: PlayerRepository

    @Inject
    lateinit var favoritesRepository: FavoritesRepository

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Position tracking — no real audio engine, so we track elapsed time manually
    private var trackStartTimeMs = 0L
    private var pausedPositionMs = 0L
    private var progressJob: Job? = null

    private fun currentPositionMs(): Long {
        return if (playerRepository.isPlaying.value) {
            pausedPositionMs + (System.currentTimeMillis() - trackStartTimeMs)
        } else {
            pausedPositionMs
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "LyraMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { playerRepository.togglePlayPause() }
                override fun onPause() { playerRepository.togglePlayPause() }
                override fun onSkipToNext() { playerRepository.skipNext() }
                override fun onSkipToPrevious() { playerRepository.skipPrevious() }
                override fun onSeekTo(pos: Long) {
                    pausedPositionMs = pos
                    if (playerRepository.isPlaying.value) {
                        trackStartTimeMs = System.currentTimeMillis() - pausedPositionMs
                    }
                }
                override fun onCustomAction(action: String, extras: Bundle?) {
                    if (action == ACTION_FAVORITE) {
                        val track = playerRepository.currentTrack.value ?: return
                        favoritesRepository.toggleFavorite(track)
                    }
                }
            })
            isActive = true
        }

        observePlayerState()
    }

    private fun observePlayerState() {
        serviceScope.launch {
            playerRepository.currentTrack.collect { track ->
                // Reset position on track change
                pausedPositionMs = 0L
                trackStartTimeMs = System.currentTimeMillis()
                val isFav = track?.let { favoritesRepository.isFavorite(it.id) } ?: false
                updateNotification(track, playerRepository.isPlaying.value, isFav)
            }
        }
        serviceScope.launch {
            playerRepository.isPlaying.collect { playing ->
                if (playing) {
                    trackStartTimeMs = System.currentTimeMillis() - pausedPositionMs
                    startProgressUpdates()
                } else {
                    pausedPositionMs = currentPositionMs()
                    stopProgressUpdates()
                }
                val track = playerRepository.currentTrack.value
                val isFav = track?.let { favoritesRepository.isFavorite(it.id) } ?: false
                updateNotification(track, playing, isFav)
            }
        }
        serviceScope.launch {
            favoritesRepository.favorites.collect {
                val track = playerRepository.currentTrack.value
                val isFav = track?.let { favoritesRepository.isFavorite(it.id) } ?: false
                updateNotification(track, playerRepository.isPlaying.value, isFav)
            }
        }
    }

    private fun updateNotification(track: NowPlayingTrack?, isPlaying: Boolean, isFavorited: Boolean) {
        track ?: return
        updateMediaSession(track, isPlaying, isFavorited)
        val notification = buildNotification(track, isPlaying, isFavorited)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateMediaSession(track: NowPlayingTrack, isPlaying: Boolean, isFavorited: Boolean) {
        val artwork = createArtworkBitmap(track.startColor, track.endColor, track.title)
        val displaySubtitle = track.subtitle.ifEmpty { "LyraApp" }
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, track.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.subtitle)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, displaySubtitle)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artwork)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)
                .build()
        )
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val speed = if (isPlaying) 1f else 0f
        val heartIcon = if (isFavorited) R.drawable.ic_notif_heart else R.drawable.ic_notif_heart_outline
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, currentPositionMs(), speed)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
                )
                .addCustomAction(
                    PlaybackStateCompat.CustomAction.Builder(
                        ACTION_FAVORITE,
                        "Favori",
                        heartIcon,
                    ).build()
                )
                .build()
        )
    }

    private fun buildNotification(track: NowPlayingTrack, isPlaying: Boolean, isFavorited: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val artwork = createArtworkBitmap(track.startColor, track.endColor, track.title)
        val heartIcon = if (isFavorited) R.drawable.ic_notif_heart else R.drawable.ic_notif_heart_outline
        val playPauseIcon = if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(track.title)
            .setContentText(track.subtitle)
            .setLargeIcon(artwork)
            .setContentIntent(openAppIntent)
            .addAction(heartIcon, "Favori", buildActionIntent(ACTION_TOGGLE_FAVORITE, 0))
            .addAction(R.drawable.ic_notif_skip_previous, "Önceki", buildActionIntent(ACTION_SKIP_PREVIOUS, 1))
            .addAction(playPauseIcon, "Oynat/Duraklat", buildActionIntent(ACTION_PLAY_PAUSE, 2))
            .addAction(R.drawable.ic_notif_skip_next, "Sonraki", buildActionIntent(ACTION_SKIP_NEXT, 3))
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(1, 2, 3),
            )
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (playerRepository.isPlaying.value) {
                delay(500)
                val track = playerRepository.currentTrack.value ?: continue
                val isFav = favoritesRepository.isFavorite(track.id)
                val heartIcon = if (isFav) R.drawable.ic_notif_heart else R.drawable.ic_notif_heart_outline
                mediaSession.setPlaybackState(
                    PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, currentPositionMs(), 1f)
                        .setActions(
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackStateCompat.ACTION_SEEK_TO,
                        )
                        .addCustomAction(
                            PlaybackStateCompat.CustomAction.Builder(
                                ACTION_FAVORITE, "Favori", heartIcon,
                            ).build()
                        )
                        .build()
                )
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun buildActionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MediaPlayerService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, requestCode, intent, flags)
        } else {
            PendingIntent.getService(this, requestCode, intent, flags)
        }
    }

    private fun createArtworkBitmap(startColor: Long, endColor: Long, title: String = ""): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dikey gradient — app'teki verticalGradient(startColor, endColor) ile aynı yön
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, size.toFloat(),
                startColor.toInt(), endColor.toInt(),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // App'teki DynamicMusicCardBackground ile aynı desenler (Stroke stili, aynı merkez)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val cx = size * 0.2f
        val cy = size * 0.3f

        when {
            title.contains("Gece") || title.contains("Neon") ||
                title.contains("Derin") || title.contains("Yıldız") -> {
                for (i in 1..6) {
                    strokePaint.color = Color.argb((0.05f * i * 255).toInt(), 255, 255, 255)
                    canvas.drawCircle(cx, cy, (size / 5f) * i, strokePaint)
                }
            }
            title.contains("Sabah") || title.contains("Odaklan") || title.contains("Sakin") -> {
                strokePaint.color = Color.argb(25, 255, 255, 255)
                val path = android.graphics.Path().apply {
                    moveTo(size * 0.2f, size * 0.2f)
                    lineTo(size * 0.5f, size * 0.1f)
                    lineTo(size * 0.4f, size * 0.4f)
                    close()
                }
                canvas.drawPath(path, strokePaint)
            }
            title.contains("Yaz") || title.contains("Enerji") -> {
                strokePaint.color = Color.argb(25, 255, 255, 255)
                strokePaint.style = Paint.Style.FILL
                for (x in 0..5) {
                    for (y in 0..5) {
                        canvas.drawCircle(size * 0.15f * x, size * 0.15f * y, 4f, strokePaint)
                    }
                }
            }
            else -> {
                for (i in 1..4) {
                    strokePaint.color = Color.argb((0.06f * i * 255).toInt(), 255, 255, 255)
                    canvas.drawCircle(cx, cy, (size / 5f) * i, strokePaint)
                }
            }
        }

        return bitmap
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medya Oynatıcı",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "LyraApp medya kontrolü"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> playerRepository.togglePlayPause()
            ACTION_SKIP_NEXT -> playerRepository.skipNext()
            ACTION_SKIP_PREVIOUS -> playerRepository.skipPrevious()
            ACTION_TOGGLE_FAVORITE -> {
                val track = playerRepository.currentTrack.value ?: return START_STICKY
                favoritesRepository.toggleFavorite(track)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopProgressUpdates()
        serviceScope.cancel()
        mediaSession.release()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "lyra_media_channel"
        const val ACTION_PLAY_PAUSE = "com.turkcell.lyraapp.ACTION_PLAY_PAUSE"
        const val ACTION_SKIP_NEXT = "com.turkcell.lyraapp.ACTION_SKIP_NEXT"
        const val ACTION_SKIP_PREVIOUS = "com.turkcell.lyraapp.ACTION_SKIP_PREVIOUS"
        const val ACTION_TOGGLE_FAVORITE = "com.turkcell.lyraapp.ACTION_TOGGLE_FAVORITE"
        const val ACTION_FAVORITE = "com.turkcell.lyraapp.ACTION_FAVORITE"
    }
}
