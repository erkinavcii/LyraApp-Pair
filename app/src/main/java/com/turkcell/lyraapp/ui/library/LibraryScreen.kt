package com.turkcell.lyraapp.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.lyraapp.data.library.Playlist
import com.turkcell.lyraapp.ui.icons.LyraIcons

@Composable
fun LibraryRoute(
    onNavigateToPlaylistDetail: (String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToNewPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LibraryEffect.NavigateToPlaylistDetail -> onNavigateToPlaylistDetail(effect.playlistId)
                LibraryEffect.NavigateToFavorites -> onNavigateToFavorites()
                LibraryEffect.NavigateToNewPlaylist -> onNavigateToNewPlaylist()
                is LibraryEffect.ShowError -> { /* Hata gösterme işlevi gerekirse buraya bağlanabilir */ }
            }
        }
    }

    LibraryScreen(
        state = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onIntent: (LibraryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 1. Üst Başlık & Arama Çubuğu Row
            LibraryHeaderRow(
                isSearchActive = state.isSearchActive,
                searchQuery = state.searchQuery,
                onSearchQueryChanged = { onIntent(LibraryIntent.SearchQueryChanged(it)) },
                onToggleSearch = { onIntent(LibraryIntent.ToggleSearchActive(it)) },
                onAddPlaylistClick = { onIntent(LibraryIntent.CreatePlaylistClick) },
                focusManager = focusManager
            )

            // 2. Sekmeler (Chips)
            LibraryTabRow(
                selectedTab = state.selectedTab,
                onTabSelected = { onIntent(LibraryIntent.SelectTab(it)) }
            )

            // 3. Sıralama ve Görünüm Kontrolü
            LibrarySortRow(
                isGridView = state.isGridView,
                onToggleViewMode = { onIntent(LibraryIntent.ToggleViewMode) }
            )

            // 4. İçerik Alanı
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (state.selectedTab) {
                    LibraryTab.Playlists -> {
                        PlaylistsContent(
                            isGridView = state.isGridView,
                            favoritesCount = state.favoritesCount,
                            downloadedCount = state.downloadedCount,
                            playlists = state.filteredPlaylists,
                            onPlaylistClick = { onIntent(LibraryIntent.ClickPlaylist(it)) },
                            onFavoritesClick = { onIntent(LibraryIntent.ClickFavorites) },
                            onDownloadsClick = { onIntent(LibraryIntent.ClickPlaylist("downloads")) },
                            onDeletePlaylistClick = { onIntent(LibraryIntent.DeletePlaylist(it)) }
                        )
                    }
                    LibraryTab.Artists -> {
                        EmptyStatePlaceholder(
                            title = "Sanatçılar",
                            subtitle = "Kütüphanene sanatçı eklediğinde burada görünecek."
                        )
                    }
                    LibraryTab.Albums -> {
                        EmptyStatePlaceholder(
                            title = "Albümler",
                            subtitle = "Kütüphanene albüm eklediğinde burada görünecek."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeaderRow(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onAddPlaylistClick: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isSearchActive) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = LyraIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Kütüphanende ara...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                )
                IconButton(
                    onClick = { onToggleSearch(false) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = LyraIcons.Close,
                        contentDescription = "Aramayı Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            Text(
                text = "Kütüphane",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onToggleSearch(true) }) {
                    Icon(
                        imageVector = LyraIcons.Search,
                        contentDescription = "Ara",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onAddPlaylistClick) {
                    Icon(
                        imageVector = LyraIcons.Add,
                        contentDescription = "Yeni Çalma Listesi",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryTabRow(
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LibraryTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val tabText = when (tab) {
                LibraryTab.Playlists -> "Çalma listeleri"
                LibraryTab.Artists -> "Sanatçılar"
                LibraryTab.Albums -> "Albümler"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSelected && tab == LibraryTab.Playlists) {
                        Icon(
                            imageVector = LyraIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = tabText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrarySortRow(
    isGridView: Boolean,
    onToggleViewMode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = LyraIcons.Sort,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Son eklenenler",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        }

        IconButton(
            onClick = onToggleViewMode,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isGridView) LyraIcons.List else LyraIcons.Grid,
                contentDescription = "Görünüm Değiştir",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PlaylistsContent(
    isGridView: Boolean,
    favoritesCount: Int,
    downloadedCount: Int,
    playlists: List<Playlist>,
    onPlaylistClick: (String) -> Unit,
    onFavoritesClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onDeletePlaylistClick: (String) -> Unit
) {
    if (isGridView) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GridFavoritesItem(
                    favoritesCount = favoritesCount,
                    onClick = onFavoritesClick
                )
            }
            item {
                GridDownloadsItem(
                    downloadedCount = downloadedCount,
                    onClick = onDownloadsClick
                )
            }
            items(playlists, key = { it.id }) { playlist ->
                GridPlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                ListFavoritesItem(
                    favoritesCount = favoritesCount,
                    onClick = onFavoritesClick
                )
            }
            item {
                ListDownloadsItem(
                    downloadedCount = downloadedCount,
                    onClick = onDownloadsClick
                )
            }
            items(playlists, key = { it.id }) { playlist ->
                ListPlaylistItem(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) },
                    onDeleteClick = { onDeletePlaylistClick(playlist.id) }
                )
            }
        }
    }
}

@Composable
private fun ListDownloadsItem(
    downloadedCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF34D399), Color(0xFF10B981))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LyraIcons.Download,
                contentDescription = null,
                tint = Color(0xFF064E3B),
                modifier = Modifier.size(28.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "İndirilen Şarkılar",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Çevrimdışı çalma listesi · $downloadedCount şarkı",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = LyraIcons.Pin,
            contentDescription = "Sabitlendi",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun GridDownloadsItem(
    downloadedCount: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF34D399), Color(0xFF10B981))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LyraIcons.Download,
                contentDescription = null,
                tint = Color(0xFF064E3B),
                modifier = Modifier.size(44.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LyraIcons.Pin,
                    contentDescription = "Sabitlendi",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "İndirilen Şarkılar",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
        Text(
            text = "$downloadedCount şarkı",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// ── LİSTE GÖRÜNÜMÜ BİLEŞENLERİ ──

@Composable
private fun ListFavoritesItem(
    favoritesCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFB1C8), Color(0xFFFF8FA8))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LyraIcons.Favorite,
                contentDescription = null,
                tint = Color(0xFF7B2949),
                modifier = Modifier.size(28.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Beğenilen Şarkılar",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Çalma listesi · $favoritesCount şarkı",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = LyraIcons.Pin,
            contentDescription = "Sabitlendi",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ListPlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(playlist.artworkStartColor), Color(playlist.artworkEndColor))
                    )
                )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Çalma listesi · ${playlist.tracks.size} şarkı",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (playlist.isOwnedByUser) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = LyraIcons.MoreVert,
                        contentDescription = "Diğer seçenekler",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sil", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = LyraIcons.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

// ── GRİD GÖRÜNÜMÜ BİLEŞENLERİ ──

@Composable
private fun GridFavoritesItem(
    favoritesCount: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFB1C8), Color(0xFFFF8FA8))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = LyraIcons.Favorite,
                contentDescription = null,
                tint = Color(0xFF7B2949),
                modifier = Modifier.size(44.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = LyraIcons.Pin,
                    contentDescription = "Sabitlendi",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Beğenilen Şarkılar",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
        Text(
            text = "$favoritesCount şarkı",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun GridPlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(playlist.artworkStartColor), Color(playlist.artworkEndColor))
                    )
                )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
        Text(
            text = "${playlist.tracks.size} şarkı",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

// ── BOŞ DURUM GÖRÜNÜMÜ ──

@Composable
private fun EmptyStatePlaceholder(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
