package com.turkcell.lyraapp.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.turkcell.lyraapp.data.library.Playlist
import com.turkcell.lyraapp.data.player.NowPlayingTrack
import com.turkcell.lyraapp.ui.icons.LyraIcons

@Composable
fun NowPlayingRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                NowPlayingEffect.NavigateBack -> onNavigateBack()
                NowPlayingEffect.ShowDownloadSuccess -> snackbarHostState.showSnackbar("Sarki indirildi")
                NowPlayingEffect.ShowDownloadError -> snackbarHostState.showSnackbar("Indirme basarisiz oldu")
                is NowPlayingEffect.ShowAddToPlaylistSuccess -> snackbarHostState.showSnackbar("\"${effect.playlistName}\" listesine eklendi")
                NowPlayingEffect.ShowAddToPlaylistError -> snackbarHostState.showSnackbar("Sarki zaten bu listede")
            }
        }
    }

    NowPlayingScreen(
        state = uiState,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: NowPlayingUiState,
    onIntent: (NowPlayingIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
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
                onAddToPlaylistClick = { onIntent(NowPlayingIntent.AddToPlaylistClick) },
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
                isDownloaded = state.isDownloaded,
                isDownloading = state.isDownloading,
                onToggleFavorite = { onIntent(NowPlayingIntent.ToggleFavorite) },
                onDownloadClick = { onIntent(NowPlayingIntent.DownloadClick) },
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color(0xFF2A2A2A),
                contentColor = Color.White,
            )
        }
    }

    if (state.showPlaylistPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onIntent(NowPlayingIntent.DismissPlaylistPicker) },
            sheetState = sheetState,
            containerColor = Color(0xFF1A1A1A),
        ) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                Text(
                    text = "Playlist'e Ekle",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                if (state.playlists.isEmpty()) {
                    Text(
                        text = "Henuz playlist olusturmadiniz",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(24.dp),
                    )
                } else {
                    LazyColumn {
                        items(state.playlists, key = { it.id }) { playlist ->
                            PlaylistPickerItem(
                                playlist = playlist,
                                onClick = { onIntent(NowPlayingIntent.AddToPlaylist(playlist.id)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    subtitle: String,
    title: String,
    onDismiss: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

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
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = LyraIcons.MoreVert,
                    contentDescription = "Diger secenekler",
                    tint = Color.White,
                )
            }
            androidx.compose.material3.DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = Color(0xFF2A2A2A),
            ) {
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            text = "Playlist'e Ekle",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = LyraIcons.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onAddToPlaylistClick()
                    },
                )
            }
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
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onToggleFavorite: () -> Unit,
    onDownloadClick: () -> Unit,
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
        
        // Indirme Butonu
        IconButton(onClick = onDownloadClick) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFFF48FB1)
                )
            } else {
                Icon(
                    imageVector = if (isDownloaded) LyraIcons.Check else LyraIcons.Download,
                    contentDescription = if (isDownloaded) "Indirildi" else "Cevrimdisi indir",
                    tint = if (isDownloaded) Color(0xFF4AC2A8) else Color(0xFF9E9E9E),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))

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
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }

    val currentProgress = if (isDragging) dragProgress else progress
    val displayPositionMs = if (isDragging) (durationMs * dragProgress).toLong() else currentPositionMs

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = currentProgress,
            onValueChange = {
                isDragging = true
                dragProgress = it
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek(dragProgress)
            },
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
                text = formatMs(displayPositionMs),
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

@Composable
private fun PlaylistPickerItem(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(playlist.artworkStartColor), Color(playlist.artworkEndColor))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = LyraIcons.LibraryMusic,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
            )
            Text(
                text = "${playlist.tracks.size} sarki",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
        Icon(
            imageVector = LyraIcons.Add,
            contentDescription = null,
            tint = Color(0xFFF48FB1),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
