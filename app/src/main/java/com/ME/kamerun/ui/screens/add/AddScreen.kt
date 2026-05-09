package com.ME.kamerun.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ME.kamerun.ui.screens.library.LibraryViewModel
import com.ME.kamerun.ui.screens.playlists.PlaylistsViewModel
import com.ME.kamerun.ui.theme.*

@Composable
fun AddScreen(
    onImportClick: () -> Unit,
    onVibeClick: () -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    playlistsViewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val songs by libraryViewModel.songs.collectAsState()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WinampBlack),
    ) {
        // Title bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(WinampGreenDark, WinampDarkBg, WinampGreenDark)
                    )
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = "ACTIONS",
                color = WinampTextBright,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        // Action buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WinampDarkBg)
                .border(1.dp, WinampBorderDark)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            WinampActionButton(
                text = "IMPORT YOUTUBE PLAYLIST",
                icon = Icons.Default.Download,
                color = WinampGreen,
                onClick = onImportClick,
            )
            WinampActionButton(
                text = "NEW PLAYLIST",
                icon = Icons.Default.PlaylistAdd,
                color = WinampYellow,
                onClick = { showCreatePlaylistDialog = true },
            )
            WinampActionButton(
                text = "AI VIBE PLAYLIST",
                icon = Icons.Default.AutoAwesome,
                color = WinampCyan,
                onClick = onVibeClick,
            )
        }

        // Song library title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(WinampGreenDark, WinampDarkBg, WinampGreenDark)
                    )
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = "LIBRARY (${songs.size} TRACKS)",
                color = WinampTextBright,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        // Song List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(WinampDisplayBg)
                .padding(2.dp),
        ) {
            if (songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "NO TRACKS LOADED",
                                color = WinampGreenDim,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Import a YouTube playlist to start",
                                color = WinampTextDim,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            } else {
                items(songs, key = { it.id }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "♪ ",
                            color = WinampGreenDark,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "${song.artist} - ${song.title}".uppercase(),
                            color = WinampGreenDim,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { libraryViewModel.deleteSong(song) },
                            modifier = Modifier.size(22.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Löschen",
                                tint = WinampTextDim,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        WinampCreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name ->
                playlistsViewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            },
        )
    }
}

@Composable
private fun WinampActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(2.dp),
        color = WinampButtonBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(listOf(WinampBorderLight, WinampBorderDark))
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun WinampCreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("NEW PLAYLIST", color = WinampGreen)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = WinampTextDim) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WinampGreen,
                    unfocusedBorderColor = WinampBorderLight,
                    cursorColor = WinampGreen,
                    focusedTextColor = WinampGreen,
                    unfocusedTextColor = WinampGreenDim,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank(),
            ) {
                Text("CREATE", color = WinampGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = WinampTextDim)
            }
        },
        containerColor = WinampDarkBg,
    )
}
