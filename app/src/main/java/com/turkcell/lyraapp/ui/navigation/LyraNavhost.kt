package com.turkcell.lyraapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.turkcell.lyraapp.ui.auth.login.LoginRoute
import com.turkcell.lyraapp.ui.auth.register.RegisterRoute
import com.turkcell.lyraapp.ui.favorites.FavoritesRoute
import com.turkcell.lyraapp.ui.home.HomeRoute
import com.turkcell.lyraapp.ui.library.LibraryRoute
import com.turkcell.lyraapp.ui.library.create.NewPlaylistRoute
import com.turkcell.lyraapp.ui.library.detail.PlaylistDetailRoute
import com.turkcell.lyraapp.ui.profile.ProfileRoute
import com.turkcell.lyraapp.ui.nowplaying.NowPlayingRoute
import com.turkcell.lyraapp.ui.search.SearchRoute

@Composable
fun LyraNavHost(
    isLoggedIn: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val startRoute = if (isLoggedIn) LyraDestination.Home.route else LyraDestination.Login.route

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isTopLevelRoute(currentRoute)) {
                LyraBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = navController::navigateToTab,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(LyraDestination.Login.route) {
                LoginRoute(
                    onNavigateToHome = { navController.navigateToHomeClearingAuth() },
                    onNavigateToRegister = {
                        navController.navigate(LyraDestination.Register.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(LyraDestination.Register.route) {
                RegisterRoute(
                    onNavigateToLogin = {
                        navController.navigate(LyraDestination.Login.route) {
                            popUpTo(LyraDestination.Login.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(LyraDestination.Home.route) {
                HomeRoute(
                    onNavigateToLogin = {
                        navController.navigate(LyraDestination.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToNowPlaying = {
                        navController.navigate(LyraDestination.NowPlaying.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToPlaylistDetail = { playlistId ->
                        navController.navigate("playlist_detail/$playlistId") {
                            launchSingleTop = true
                        }
                    },
                    onToggleTheme = onThemeToggle,
                )
            }

            composable(LyraDestination.NowPlaying.route) {
                NowPlayingRoute(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(LyraDestination.Search.route) { SearchRoute() }
            composable(LyraDestination.Library.route) {
                LibraryRoute(
                    onNavigateToPlaylistDetail = { playlistId ->
                        navController.navigate("playlist_detail/$playlistId") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToFavorites = {
                        navController.navigate(LyraDestination.Favorites.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToNewPlaylist = {
                        navController.navigate(LyraDestination.NewPlaylist.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(LyraDestination.PlaylistDetail.route) {
                PlaylistDetailRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNowPlaying = {
                        navController.navigate(LyraDestination.NowPlaying.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(LyraDestination.NewPlaylist.route) {
                NewPlaylistRoute(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(LyraDestination.Favorites.route) {
                FavoritesRoute(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNowPlaying = {
                        navController.navigate(LyraDestination.NowPlaying.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(LyraDestination.Profile.route) { ProfileRoute() }
        }
    }
}

private fun NavHostController.navigateToTab(destination: LyraDestination) {
    navigate(destination.route) {
        popUpTo(LyraDestination.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateToHomeClearingAuth() {
    navigate(LyraDestination.Home.route) {
        popUpTo(LyraDestination.Login.route) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}