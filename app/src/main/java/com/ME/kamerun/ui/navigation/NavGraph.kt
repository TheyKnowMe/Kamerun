package com.ME.kamerun.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.ME.kamerun.player.MusicPlayer
import com.ME.kamerun.ui.components.MiniPlayerBar
import com.ME.kamerun.ui.screens.add.AddScreen
import com.ME.kamerun.ui.screens.home.HomeScreen
import com.ME.kamerun.ui.screens.import_songs.ImportScreen
import com.ME.kamerun.ui.screens.playlists.PlaylistDetailScreen
import com.ME.kamerun.ui.screens.playlists.PlaylistsViewModel
import com.ME.kamerun.ui.screens.settings.SettingsScreen
import com.ME.kamerun.ui.screens.vibe.VibeGeneratorScreen
import com.ME.kamerun.ui.theme.*
import kotlinx.coroutines.delay

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Add : Screen("add")
    object Import : Screen("import")
    object Vibe : Screen("vibe")
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    object Settings : Screen("settings")
}

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "PLAYER", Icons.Default.Home),
    BottomNavItem(Screen.Add, "ADD", Icons.Default.Add),
    BottomNavItem(Screen.Settings, "CONFIG", Icons.Default.Settings),
)

@Composable
fun AppNavGraph(
    musicPlayer: MusicPlayer,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }
    val playerState by musicPlayer.state.collectAsState()

    // Position updater
    var currentPosition by remember { mutableStateOf(0L) }
    LaunchedEffect(playerState.isPlaying) {
        while (playerState.isPlaying) {
            currentPosition = musicPlayer.getCurrentPosition()
            delay(500)
        }
    }
    val displayState = playerState.copy(currentPosition = currentPosition)

    Scaffold(
        containerColor = WinampBlack,
        bottomBar = {
            if (showBottomBar) {
                Column(modifier = Modifier.background(WinampBlack)) {
                    // Mini player on non-home screens
                    if (currentRoute != Screen.Home.route && playerState.currentSong != null) {
                        MiniPlayerBar(
                            playerState = displayState,
                            onPlayPause = { musicPlayer.togglePlayPause() },
                            onNext = { musicPlayer.next() },
                            onPrevious = { musicPlayer.previous() },
                        )
                    }

                    NavigationBar(
                        containerColor = WinampDarkBg,
                        contentColor = WinampGreen,
                    ) {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                icon = {
                                    Icon(item.icon, contentDescription = item.label)
                                },
                                label = {
                                    Text(
                                        item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                selected = currentRoute == item.screen.route,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = WinampGreen,
                                    selectedTextColor = WinampGreen,
                                    unselectedIconColor = WinampTextDim,
                                    unselectedTextColor = WinampTextDim,
                                    indicatorColor = WinampGreenDark.copy(alpha = 0.3f),
                                ),
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onPlaylistClick = { id ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(id))
                    },
                    musicPlayer = musicPlayer,
                )
            }

            composable(Screen.Add.route) {
                AddScreen(
                    onImportClick = { navController.navigate(Screen.Import.route) },
                    onVibeClick = { navController.navigate(Screen.Vibe.route) },
                )
            }

            composable(Screen.Import.route) {
                ImportScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Vibe.route) {
                VibeGeneratorScreen(onSaved = { navController.popBackStack() })
            }

            composable(
                Screen.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                val viewModel: PlaylistsViewModel = hiltViewModel()

                var playlist by remember {
                    mutableStateOf<com.ME.kamerun.data.local.entities.PlaylistEntity?>(null)
                }
                val songs by viewModel.getSongsForPlaylist(playlistId)
                    .collectAsState(initial = emptyList())
                val allSongs by viewModel.allSongs.collectAsState()

                LaunchedEffect(playlistId) {
                    playlist = viewModel.getPlaylistById(playlistId)
                }

                PlaylistDetailScreen(
                    playlist = playlist,
                    songs = songs,
                    allSongs = allSongs,
                    onBack = { navController.popBackStack() },
                    onRemoveSong = { songId ->
                        viewModel.removeSongFromPlaylist(playlistId, songId)
                    },
                    onAddSong = { song ->
                        viewModel.addSongToPlaylist(playlistId, song.id)
                    },
                    musicPlayer = musicPlayer,
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
