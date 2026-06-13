package com.turkcell.lyraapp.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.lyraapp.data.player.NowPlayingTrack
import com.turkcell.lyraapp.ui.icons.LyraIcons

@Composable
fun NowPlayingRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                NowPlayingEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    NowPlayingScreen(
        state = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun NowPlayingScreen(
    state: NowPlayingUiState,
    onIntent: (NowPlayingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = state.track
    val startColor = Color(track?.startColor ?: 0xFF3A2A1A)
    val endColor = Color(track?.endColor ?: 0xFF1A1008)
    val bgEnd = endColor.copy(alpha = 1f).let {
        Color(
            red = it.red * 0.35f,
            green = it.green * 0.35f,
            blue = it.blue * 0.35f,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(startColor.copy(alpha = 0.6f), bgEnd))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))

            TopBar(
                subtitle = track?.subtitle ?: "",
                title = track?.title ?: "",
                onDismiss = { onIntent(NowPlayingIntent.Dismiss) },
            )

            Spacer(Modifier.height(32.dp))

            AlbumArt(
                startColor = startColor,
                endColor = endColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp)),
            )

            Spacer(Modifier.height(28.dp))

            TrackInfo(
                title = track?.title ?: "",
                subtitle = track?.subtitle ?: "",
                isFavorited = state.isFavorited,
                onToggleFavorite = { onIntent(NowPlayingIntent.ToggleFavorite) },
            )

            Spacer(Modifier.height(16.dp))

            ProgressSection(
                progress = state.progress,
                currentPositionMs = state.currentPositionMs,
                durationMs = track?.durationMs ?: 0L,
                onSeek = { onIntent(NowPlayingIntent.SeekTo(it)) },
            )

            Spacer(Modifier.height(16.dp))

            PlaybackControls(
                isPlaying = state.isPlaying,
                isShuffling = state.isShuffling,
                isRepeating = state.isRepeating,
                onTogglePlayPause = { onIntent(NowPlayingIntent.TogglePlayPause) },
                onSkipPrevious = { onIntent(NowPlayingIntent.SkipPrevious) },
                onSkipNext = { onIntent(NowPlayingIntent.SkipNext) },
                onToggleShuffle = { onIntent(NowPlayingIntent.ToggleShuffle) },
                onToggleRepeat = { onIntent(NowPlayingIntent.ToggleRepeat) },
            )

            Spacer(Modifier.height(24.dp))

            BottomActions()
        }
    }
}

@Composable
private fun TopBar(
    subtitle: String,
    title: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = LyraIcons.ChevronDown,
                contentDescription = "Kapat",
                tint = Color.White,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SIMDI CALIYOR",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 1.5.sp,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        IconButton(onClick = {}) {
            Icon(
                imageVector = LyraIcons.MoreVert,
                contentDescription = "Diger secenekler",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun AlbumArt(
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            Brush.radialGradient(listOf(startColor, endColor)),
        ),
    ) {
        // Dekoratif ic ice daireler
        for (i in 1..5) {
            Box(
                modifier = Modifier
                    .fillMaxSize(fraction = 0.18f * i + 0.1f)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f)),
            )
        }
    }
}

@Composable
private fun TrackInfo(
    title: String,
    subtitle: String,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorited) LyraIcons.Favorite else LyraIcons.FavoriteOutlined,
                contentDescription = "Favori",
                tint = if (isFavorited) Color(0xFFF48FB1) else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun ProgressSection(
    progress: Float,
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFF48FB1),
                activeTrackColor = Color(0xFFF48FB1),
                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatMs(currentPositionMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
            Text(
                text = formatMs(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPlaying: Boolean,
    isShuffling: Boolean,
    isRepeating: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlIcon(
            icon = LyraIcons.Shuffle,
            tint = if (isShuffling) Color(0xFFF48FB1) else Color.White.copy(alpha = 0.7f),
            size = 24,
            onClick = onToggleShuffle,
        )
        ControlIcon(
            icon = LyraIcons.SkipPrevious,
            tint = Color.White,
            size = 32,
            onClick = onSkipPrevious,
        )

        // Buyuk oynat/durdur butonu
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFF48FB1)),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onTogglePlayPause, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isPlaying) LyraIcons.Pause else LyraIcons.Play,
                    contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        ControlIcon(
            icon = LyraIcons.SkipNext,
            tint = Color.White,
            size = 32,
            onClick = onSkipNext,
        )
        ControlIcon(
            icon = LyraIcons.Repeat,
            tint = if (isRepeating) Color(0xFFF48FB1) else Color.White.copy(alpha = 0.7f),
            size = 24,
            onClick = onToggleRepeat,
        )
    }
}

@Composable
private fun ControlIcon(
    icon: ImageVector,
    tint: Color,
    size: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size.dp),
        )
    }
}

@Composable
private fun BottomActions() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = {}) {
            Icon(
                imageVector = LyraIcons.Cast,
                contentDescription = "Yayinla",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = LyraIcons.Notifications,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Arkaplan",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
        IconButton(onClick = {}) {
            Icon(
                imageVector = LyraIcons.QueueMusic,
                contentDescription = "Kuyruk",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
