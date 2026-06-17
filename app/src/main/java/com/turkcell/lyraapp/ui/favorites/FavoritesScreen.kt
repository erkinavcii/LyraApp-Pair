package com.turkcell.lyraapp.ui.favorites

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
fun FavoritesRoute(
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                FavoritesEffect.NavigateBack -> onNavigateBack()
                FavoritesEffect.NavigateToNowPlaying -> onNavigateToNowPlaying()
            }
        }
    }

    FavoritesScreen(
        state = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    onIntent: (FavoritesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF121212),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            item {
                FavoritesHeader(onNavigateBack = { onIntent(FavoritesIntent.NavigateBack) })
            }
            item {
                FavoritesCoverSection(
                    trackCountText = state.trackCountText,
                    totalDurationText = state.totalDurationText,
                )
            }
            item {
                FavoritesActions(
                    hasItems = state.tracks.isNotEmpty(),
                    onPlayAll = { onIntent(FavoritesIntent.PlayAll) },
                    onShuffle = { onIntent(FavoritesIntent.ShufflePlay) },
                    onDownload = { onIntent(FavoritesIntent.Download) },
                )
            }
            if (state.tracks.isEmpty()) {
                item {
                    EmptyFavoritesHint()
                }
            } else {
                items(state.tracks, key = { it.id }) { track ->
                    FavoriteTrackRow(
                        track = track,
                        isPlaying = state.currentTrack?.id == track.id,
                        onClick = { onIntent(FavoritesIntent.PlayTrack(track)) },
                        onToggleFavorite = { onIntent(FavoritesIntent.ToggleFavorite(track)) },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun FavoritesHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = LyraIcons.ArrowBack,
                contentDescription = "Geri",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun FavoritesCoverSection(
    trackCountText: String,
    totalDurationText: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFB1C8), Color(0xFFFF8FA8)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = LyraIcons.Favorite,
                contentDescription = null,
                tint = Color(0xFF7B2949),
                modifier = Modifier.size(56.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Begenilen Sarkılar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            if (trackCountText.isNotEmpty()) {
                Text(
                    text = "$trackCountText · $totalDurationText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun FavoritesActions(
    hasItems: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onDownload: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (hasItems) Color(0xFFFFB1C8) else Color(0xFF3A2A30))
                .clickable(enabled = hasItems, onClick = onPlayAll),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = LyraIcons.Play,
                    contentDescription = null,
                    tint = if (hasItems) Color(0xFF5E1133) else Color.Gray,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Cal",
                    color = if (hasItems) Color(0xFF5E1133) else Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A))
                .clickable(enabled = hasItems, onClick = onShuffle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = LyraIcons.Shuffle,
                contentDescription = "Karistir",
                tint = if (hasItems) Color.White else Color.Gray,
                modifier = Modifier.size(22.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A))
                .clickable(onClick = onDownload),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = LyraIcons.Download,
                contentDescription = "Indir",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun FavoriteTrackRow(
    track: NowPlayingTrack,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPlaying) Color(0xFF2A1F23) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(track.startColor), Color(track.endColor)),
                    ),
                ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isPlaying) Color(0xFFFFB1C8) else Color.White,
                fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
                maxLines = 1,
            )
            Text(
                text = track.subtitle,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Text(
            text = formatMs(track.durationMs),
            color = Color.Gray,
            fontSize = 12.sp,
        )
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = LyraIcons.Favorite,
                contentDescription = "Favoriden cikar",
                tint = Color(0xFFFFB1C8),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = LyraIcons.MoreVert,
                contentDescription = "Diger secenekler",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun EmptyFavoritesHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = LyraIcons.FavoriteOutlined,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = "Henuz hic sarki begenmediniz",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Sarkilardaki kalp ikonuna tiklayin",
                color = Color(0xFF555555),
                style = MaterialTheme.typography.bodySmall,
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
