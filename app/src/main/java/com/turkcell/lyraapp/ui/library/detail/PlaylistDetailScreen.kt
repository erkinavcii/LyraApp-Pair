package com.turkcell.lyraapp.ui.library.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.lyraapp.data.player.NowPlayingTrack
import com.turkcell.lyraapp.ui.icons.LyraIcons

@Composable
fun PlaylistDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PlaylistDetailEffect.NavigateBack -> onNavigateBack()
                PlaylistDetailEffect.NavigateToNowPlaying -> onNavigateToNowPlaying()
                is PlaylistDetailEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    PlaylistDetailScreen(
        state = uiState,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    state: PlaylistDetailUiState,
    onIntent: (PlaylistDetailIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val playlist = state.playlist
    val startColor = playlist?.artworkStartColor ?: 0xFF191114
    val endColor = playlist?.artworkEndColor ?: 0xFF191114

    if (state.isAddTrackSheetVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onIntent(PlaylistDetailIntent.DismissAddTrackSheet) },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            AddTrackSheetContent(
                tracks = state.availableTracks,
                isAddingTrack = state.isAddingTrack,
                onAddTrack = { onIntent(PlaylistDetailIntent.AddTrackToPlaylist(it)) },
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (playlist != null) {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // 1. Üst Aksiyon Barı (Geri & Seçenekler)
                item {
                    DetailHeaderRow(
                        onBackClick = { onIntent(PlaylistDetailIntent.NavigateBack) }
                    )
                }

                // 2. Playlist Kapak & Bilgileri Hero Bölümü
                item {
                    PlaylistHeroSection(
                        name = playlist.name,
                        description = playlist.description,
                        startColor = startColor,
                        endColor = endColor,
                        trackCount = playlist.tracks.size,
                        totalDurationMs = playlist.tracks.sumOf { it.durationMs },
                        ownerName = state.ownerName,
                    )
                }

                // 3. Playlist Aksiyon Tuşları (Karıştır, Çal, İndir vs.)
                item {
                    PlaylistActionsRow(
                        isDownloading = state.isDownloading,
                        isPlaying = state.isPlaying && playlist.tracks.any { it.id == state.currentTrack?.id },
                        isPlaylistFavorited = state.isPlaylistFavorited,
                        isOwnedByUser = playlist.isOwnedByUser && playlist.id != "downloads",
                        onPlayClick = { onIntent(PlaylistDetailIntent.PlayAll) },
                        onShuffleClick = { onIntent(PlaylistDetailIntent.ShufflePlay) },
                        onDownloadClick = { onIntent(PlaylistDetailIntent.DownloadClick) },
                        onFavoriteClick = { onIntent(PlaylistDetailIntent.TogglePlaylistFavorite) },
                        onAddTrackClick = { onIntent(PlaylistDetailIntent.ShowAddTrackSheet) },
                    )
                }

                // 4. Şarkı Listesi
                items(playlist.tracks, key = { it.id }) { track ->
                    val isTrackPlaying = state.currentTrack?.id == track.id
                    val isFavorited = state.favoritedTrackIds.contains(track.id)

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                onIntent(PlaylistDetailIntent.RemoveTrack(track.id))
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = false,
                        backgroundContent = {
                            val color = Color.Red.copy(alpha = 0.8f)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(
                                    imageVector = LyraIcons.Delete,
                                    contentDescription = "Sil",
                                    tint = Color.White
                                )
                            }
                        },
                        content = {
                            TrackRowItem(
                                track = track,
                                isPlaying = isTrackPlaying,
                                isFavorited = isFavorited,
                                onClick = { onIntent(PlaylistDetailIntent.PlayTrack(track)) },
                                onToggleFavorite = { onIntent(PlaylistDetailIntent.ToggleFavorite(track)) }
                            )
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailHeaderRow(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = LyraIcons.ArrowBack,
                contentDescription = "Geri",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = {}) {
            Icon(
                imageVector = LyraIcons.MoreVert,
                contentDescription = "Seçenekler",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun PlaylistHeroSection(
    name: String,
    description: String,
    startColor: Long,
    endColor: Long,
    trackCount: Int,
    totalDurationMs: Long,
    ownerName: String,
) {
    val totalMinutes = totalDurationMs / 60_000

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Kapak Resmi
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(startColor), Color(endColor))
                    )
                )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // İsim
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Açıklama
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Yazar ve Süre Metadataları
        Text(
            text = "${ownerName.ifBlank { "Bilinmiyor" }} · $trackCount şarkı · $totalMinutes dk",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PlaylistActionsRow(
    isDownloading: Boolean,
    isPlaying: Boolean,
    isPlaylistFavorited: Boolean,
    isOwnedByUser: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddTrackClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sol Kısım: Beğen, İndir, Ekle
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isPlaylistFavorited) LyraIcons.Favorite else LyraIcons.FavoriteOutlined,
                contentDescription = "Çalma Listesini Beğen",
                tint = if (isPlaylistFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = onDownloadClick) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = LyraIcons.Download,
                    contentDescription = "Çalma Listesini İndir",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        if (isOwnedByUser) {
            IconButton(onClick = onAddTrackClick) {
                Icon(
                    imageVector = LyraIcons.Add,
                    contentDescription = "Şarkı Ekle",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sağ Kısım: Karıştır ve Çal
        IconButton(onClick = onShuffleClick) {
            Icon(
                imageVector = LyraIcons.Shuffle,
                contentDescription = "Karıştırarak Çal",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onPlayClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) LyraIcons.Pause else LyraIcons.Play,
                contentDescription = "Çal",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun TrackRowItem(
    track: NowPlayingTrack,
    isPlaying: Boolean,
    isFavorited: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPlaying) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Şarkı Görseli veya Çalma Dalga Efekti
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(track.startColor), Color(track.endColor))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Icon(
                    imageVector = LyraIcons.Waveform,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Başlık & Sanatçı
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Text(
                text = track.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1
            )
        }

        // Süre
        Text(
            text = formatMs(track.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        // Beğen butonu
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isFavorited) LyraIcons.Favorite else LyraIcons.FavoriteOutlined,
                contentDescription = "Favori",
                tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // Diğer Seçenekler
        IconButton(
            onClick = {},
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = LyraIcons.MoreVert,
                contentDescription = "Daha fazla",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
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

@Composable
private fun AddTrackSheetContent(
    tracks: List<NowPlayingTrack>,
    isAddingTrack: Boolean,
    onAddTrack: (NowPlayingTrack) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Şarkı Ekle",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isAddingTrack) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (tracks.isEmpty() && !isAddingTrack) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Eklenecek şarkı bulunamadı.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                items(tracks, key = { it.id }) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAddingTrack) { onAddTrack(track) }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(track.startColor), Color(track.endColor))
                                    )
                                )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = track.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        Icon(
                            imageVector = LyraIcons.Add,
                            contentDescription = "Ekle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
