package com.turkcell.lyraapp.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.lyraapp.data.home.PlaylistForYou
import com.turkcell.lyraapp.data.home.QuickPick
import com.turkcell.lyraapp.data.home.RecentlyPlayed
import com.turkcell.lyraapp.data.home.Recommendation
import com.turkcell.lyraapp.data.player.NowPlayingTrack
import com.turkcell.lyraapp.ui.icons.LyraIcons

@Composable
fun HomeRoute(
    onNavigateToLogin: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToPlaylistDetail: (String) -> Unit,
    onNavigateToPremiumPlans: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onIntent(HomeIntent.Retry)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeEffect.NavigateToLogin -> onNavigateToLogin()
                HomeEffect.NavigateToNowPlaying -> onNavigateToNowPlaying()
                is HomeEffect.NavigateToPlaylistDetail -> onNavigateToPlaylistDetail(effect.playlistId)
                is HomeEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                HomeEffect.NavigateToPremiumPlans -> onNavigateToPremiumPlans()
            }
        }
    }

    HomeScreen(
        state = uiState,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (state.isDarkMode) Color(0xFF121212) else Color(0xFFF8F8F8),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(24.dp))
                    HomeHeader(
                        greeting = state.greeting,
                        userInitials = state.userInitials,
                        isDarkMode = state.isDarkMode,
                        onLogout = { onIntent(HomeIntent.Logout) },
                        onToggleTheme = { onIntent(HomeIntent.ToggleTheme) }
                    )

                    val quickPickTracks = state.quickPicks.map { qp ->
                        NowPlayingTrack(id = qp.id, title = qp.title, subtitle = "", startColor = qp.artworkStartColor, endColor = qp.artworkEndColor)
                    }
                    val recentlyPlayedTracks = state.recentlyPlayed.map { rp ->
                        NowPlayingTrack(id = rp.id, title = rp.title, subtitle = rp.subtitle, startColor = rp.artworkStartColor, endColor = rp.artworkEndColor)
                    }

                    val recommendationsTracks = state.recommendations.map { rec ->
                        NowPlayingTrack(id = rec.id, title = rec.title, subtitle = rec.subtitle, startColor = rec.artworkStartColor, endColor = rec.artworkEndColor)
                    }

                    if (state.isOffline) {
                        OfflineBanner(isDarkMode = state.isDarkMode)

                        if (state.offlineRecentlyPlayed.isNotEmpty()) {
                            SectionHeader(title = "Son çalınanlar", isDarkMode = state.isDarkMode)
                            val offlineRecentlyTracks = state.offlineRecentlyPlayed.map { rp ->
                                NowPlayingTrack(id = rp.id, title = rp.title, subtitle = rp.subtitle, startColor = rp.artworkStartColor, endColor = rp.artworkEndColor)
                            }
                            RecentlyPlayedList(
                                items = state.offlineRecentlyPlayed,
                                isDarkMode = state.isDarkMode,
                                onTrackClick = { track -> onIntent(HomeIntent.PlayTrack(track, offlineRecentlyTracks)) },
                            )
                        }

                        if (state.offlineDownloadedSongs.isNotEmpty()) {
                            SectionHeader(title = "İndirilenler", isDarkMode = state.isDarkMode)
                            val offlineDownloadedTracks = state.offlineDownloadedSongs.map { qp ->
                                NowPlayingTrack(id = qp.id, title = qp.title, subtitle = "", startColor = qp.artworkStartColor, endColor = qp.artworkEndColor)
                            }
                            QuickPicksGrid(
                                items = state.offlineDownloadedSongs,
                                isDarkMode = state.isDarkMode,
                                onTrackClick = { track -> onIntent(HomeIntent.PlayTrack(track, offlineDownloadedTracks)) },
                            )
                        }

                        if (state.offlineRecentlyPlayed.isEmpty() && state.offlineDownloadedSongs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Çevrimdisi içerik bulunamadi. Önce internet bağlantısı ile şarkı indirin.",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        QuickPicksGrid(
                            items = state.quickPicks,
                            isDarkMode = state.isDarkMode,
                            onTrackClick = { track -> onIntent(HomeIntent.PlayTrack(track, quickPickTracks)) },
                        )

                        SectionHeader(title = "Son çalınanlar", isDarkMode = state.isDarkMode)
                        RecentlyPlayedList(
                            items = state.recentlyPlayed,
                            isDarkMode = state.isDarkMode,
                            onTrackClick = { track -> onIntent(HomeIntent.PlayTrack(track, recentlyPlayedTracks)) },
                        )

                        SectionHeader(title = "Önerilenler", isDarkMode = state.isDarkMode)
                        RecommendationsList(
                            items = state.recommendations,
                            isDarkMode = state.isDarkMode,
                            onTrackClick = { track -> onIntent(HomeIntent.PlayTrack(track, recommendationsTracks)) },
                        )

                        SectionHeader(title = "Senin için çalma listeleri", isDarkMode = state.isDarkMode)
                        PlaylistsList(
                            items = state.playlistsForYou,
                            isDarkMode = state.isDarkMode,
                            onPlaylistClick = { playlistId -> onIntent(HomeIntent.PlaylistClicked(playlistId)) },
                        )
                    }

                    Spacer(Modifier.height(100.dp))
                }

                state.currentTrack?.let { track ->
                    MiniPlayer(
                        title = track.title,
                        artist = track.subtitle,
                        startColor = track.startColor,
                        endColor = track.endColor,
                        isDarkMode = state.isDarkMode,
                        isFavorited = state.isCurrentTrackFavorited,
                        isPlaying = state.isPlaying,
                        onClick = { onIntent(HomeIntent.OpenNowPlaying) },
                        onPlayPause = { onIntent(HomeIntent.TogglePlayPause) },
                        onSkipNext = { onIntent(HomeIntent.SkipNext) },
                        onFavoriteToggle = { onIntent(HomeIntent.ToggleFavorite) },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }

                if (state.showPremiumExpiryDialog) {
                    PremiumExpiryDialog(
                        daysLeft = state.premiumDaysLeft,
                        onSubscribeMonthly = { onIntent(HomeIntent.NavigateToPremiumFromDialog) },
                        onRenewOneTime = { onIntent(HomeIntent.NavigateToPremiumFromDialog) },
                        onDismiss = { onIntent(HomeIntent.DismissPremiumExpiryDialog) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumExpiryDialog(
    daysLeft: Int,
    onSubscribeMonthly: () -> Unit,
    onRenewOneTime: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                .background(Color(0xFF2A2020))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = LyraIcons.Clock,
                contentDescription = null,
                tint = Color(0xFFF4A0B5),
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = "Premium'un $daysLeft gun sonra bitiyor",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                text = "Tek seferlik erisiniz sona ermek uzere. Kesintisiz dinlemeye devam etmek icin yenile ya da aylik abonelige gec.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onSubscribeMonthly,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF4A0B5),
                ),
            ) {
                Icon(
                    imageVector = LyraIcons.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF5A1E35),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Aylik abonelige gec",
                    color = Color(0xFF5A1E35),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            Button(
                onClick = onRenewOneTime,
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
            ) {
                Icon(
                    imageVector = LyraIcons.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "30 gun yenile",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Daha sonra",
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun OfflineBanner(isDarkMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFE0E0E0))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = LyraIcons.Download,
            contentDescription = null,
            tint = Color(0xFFF48FB1),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "Çevrimdisi mod — yalnizca indirilen içerikler gösteriliyor",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    userInitials: String,
    isDarkMode: Boolean,
    onLogout: () -> Unit,
    onToggleTheme: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = greeting,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isDarkMode) LyraIcons.LightMode else LyraIcons.DarkMode,
                        contentDescription = "Tema Değiştir",
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF48FB1))
                        .clickable { onLogout() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitials,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        Text(
            text = "Ne dinlemek istersin?",
            style = MaterialTheme.typography.headlineMedium,
            color = if (isDarkMode) Color.White else Color.Black,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun QuickPicksGrid(
    items: List<QuickPick>,
    isDarkMode: Boolean,
    onTrackClick: (NowPlayingTrack) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in items.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickPickCard(item = items[i], isDarkMode = isDarkMode, onTrackClick = onTrackClick, modifier = Modifier.weight(1f))
                if (i + 1 < items.size) {
                    QuickPickCard(item = items[i + 1], isDarkMode = isDarkMode, onTrackClick = onTrackClick, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickPickCard(
    item: QuickPick,
    isDarkMode: Boolean,
    onTrackClick: (NowPlayingTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFE0E0E0))
            .clickable {
                onTrackClick(
                    NowPlayingTrack(
                        id = item.id,
                        title = item.title,
                        subtitle = "",
                        startColor = item.artworkStartColor,
                        endColor = item.artworkEndColor,
                    )
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        DynamicMusicCardBackground(
            title = item.title,
            startColor = Color(item.artworkStartColor),
            endColor = Color(item.artworkEndColor),
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = item.title,
            color = if (isDarkMode) Color.White else Color.Black,
            modifier = Modifier.padding(horizontal = 12.dp),
            maxLines = 1,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SectionHeader(title: String, isDarkMode: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = if (isDarkMode) Color.White else Color.Black,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tümü",
            color = Color(0xFFF48FB1),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun RecentlyPlayedList(
    items: List<RecentlyPlayed>,
    isDarkMode: Boolean,
    onTrackClick: (NowPlayingTrack) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(items) { item ->
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .clickable {
                        onTrackClick(
                            NowPlayingTrack(
                                id = item.id,
                                title = item.title,
                                subtitle = item.subtitle,
                                startColor = item.artworkStartColor,
                                endColor = item.artworkEndColor,
                            )
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DynamicMusicCardBackground(
                        title = item.title,
                        startColor = Color(item.artworkStartColor),
                        endColor = Color(item.artworkEndColor),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.title,
                    color = if (isDarkMode) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = item.subtitle,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RecommendationsList(
    items: List<Recommendation>,
    isDarkMode: Boolean,
    onTrackClick: (NowPlayingTrack) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(items) { item ->
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .clickable {
                        onTrackClick(
                            NowPlayingTrack(
                                id = item.id,
                                title = item.title,
                                subtitle = item.subtitle,
                                startColor = item.artworkStartColor,
                                endColor = item.artworkEndColor,
                            )
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DynamicMusicCardBackground(
                        title = item.title,
                        startColor = Color(item.artworkStartColor),
                        endColor = Color(item.artworkEndColor),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.title,
                    color = if (isDarkMode) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = item.subtitle,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PlaylistsList(
    items: List<PlaylistForYou>,
    isDarkMode: Boolean,
    onPlaylistClick: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(items) { item ->
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .clickable {
                        onPlaylistClick(item.id)
                    },
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    DynamicMusicCardBackground(
                        title = item.title,
                        startColor = Color(item.artworkStartColor),
                        endColor = Color(item.artworkEndColor),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.title,
                    color = if (isDarkMode) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun DynamicMusicCardBackground(
    title: String,
    startColor: Color,
    endColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(startColor, endColor))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.dp.toPx()

            // Şarkı ismine göre sahte desenler
            when {
                title.contains("Gece") || title.contains("Neon") || title.contains("Derin") || title.contains("Yıldız") -> {
                    for (i in 1..6) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f * i),
                            radius = (size.minDimension / 5) * i,
                            center = Offset(size.width * 0.2f, size.height * 0.3f),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
                title.contains("Sabah") || title.contains("Odaklan") || title.contains("Sakin") -> {
                    val path = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.2f)
                        lineTo(size.width * 0.5f, size.height * 0.1f)
                        lineTo(size.width * 0.4f, size.height * 0.4f)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.1f),
                        style = Stroke(width = strokeWidth)
                    )
                }
                title.contains("Yaz") || title.contains("Enerji") -> {
                    for (x in 0..5) {
                        for (y in 0..5) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.1f),
                                radius = 2.dp.toPx(),
                                center = Offset(size.width * 0.15f * x, size.height * 0.15f * y)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    title: String,
    artist: String,
    startColor: Long,
    endColor: Long,
    isDarkMode: Boolean,
    isFavorited: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF2A2121) else Color(0xFFEEEEEE)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.verticalGradient(listOf(Color(startColor), Color(endColor)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LyraIcons.Waveform4,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isDarkMode) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = artist,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (isFavorited) LyraIcons.Favorite else LyraIcons.FavoriteOutlined,
                    contentDescription = "Favori",
                    tint = if (isFavorited) Color(0xFFF48FB1) else if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) LyraIcons.Pause else LyraIcons.Play,
                    contentDescription = if (isPlaying) "Duraklat" else "Oynat",
                    tint = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = onSkipNext) {
                Icon(
                    imageVector = LyraIcons.SkipNext,
                    contentDescription = "Sonraki",
                    tint = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}