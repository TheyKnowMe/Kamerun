package com.ME.kamerun.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ME.kamerun.data.local.entities.SongEntity
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
    var songToDelete by remember { mutableStateOf<SongEntity?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WinampBlack),
    ) {
        // Title bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(WinampGreenDark, WinampDarkBg, WinampGreenDark)))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("ACTIONS", color = WinampTextBright, fontSize = 13.sp, fontFamily = WinampFont, fontWeight = FontWeight.Bold)
        }

        // Action buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WinampDarkBg)
                .border(1.dp, WinampBorderDark)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WinampActionButton("IMPORT YOUTUBE PLAYLIST", Icons.Default.Download, WinampGreen, onImportClick)
            WinampActionButton("NEW PLAYLIST", Icons.AutoMirrored.Filled.PlaylistAdd, WinampYellow) { showCreatePlaylistDialog = true }
            WinampActionButton("AI VIBE PLAYLIST", Icons.Default.AutoAwesome, WinampCyan, onVibeClick)
        }

        // Library title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .background(Brush.horizontalGradient(listOf(WinampGreenDark, WinampDarkBg, WinampGreenDark)))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "LIBRARY (${songs.size} TRACKS)",
                color = WinampTextBright,
                fontSize = 13.sp,
                fontFamily = WinampFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (songs.isNotEmpty()) {
                Row(
                    modifier = Modifier.clickable { showDeleteAllDialog = true }.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Alle löschen", tint = WinampRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ALL", color = WinampRed, fontSize = 12.sp, fontFamily = WinampFont, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Song list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(WinampDisplayBg)
                .padding(4.dp),
        ) {
            if (songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NO TRACKS LOADED", color = WinampGreenDim, fontSize = 16.sp, fontFamily = WinampFont)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Import a YouTube playlist to start", color = WinampTextDim, fontSize = 12.sp, fontFamily = WinampFont)
                        }
                    }
                }
            } else {
                items(songs, key = { it.id }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("♪ ", color = WinampGreenDark, fontSize = 15.sp, fontFamily = WinampFont)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title.uppercase(),
                                color = WinampGreen,
                                fontSize = 14.sp,
                                fontFamily = WinampFont,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artist.uppercase(),
                                color = WinampGreenDim,
                                fontSize = 11.sp,
                                fontFamily = WinampFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { songToDelete = song }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Löschen", tint = WinampTextDim, modifier = Modifier.size(18.dp))
                        }
                    }
                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(WinampBorderDark))
                }
            }
        }
    }

    // Dialogs
    songToDelete?.let { song ->
        WinampConfirmDialog(
            title = "DELETE TRACK",
            message = "${song.artist} - ${song.title}",
            confirmText = "DELETE",
            confirmColor = WinampRed,
            onConfirm = { libraryViewModel.deleteSong(song); songToDelete = null },
            onDismiss = { songToDelete = null },
        )
    }

    if (showDeleteAllDialog) {
        WinampConfirmDialog(
            title = "DELETE ALL TRACKS",
            message = "Alle ${songs.size} Songs aus der Library löschen?",
            confirmText = "DELETE ALL",
            confirmColor = WinampRed,
            onConfirm = { songs.forEach { libraryViewModel.deleteSong(it) }; showDeleteAllDialog = false },
            onDismiss = { showDeleteAllDialog = false },
        )
    }

    if (showCreatePlaylistDialog) {
        WinampCreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name -> playlistsViewModel.createPlaylist(name); showCreatePlaylistDialog = false },
        )
    }
}

@Composable
fun WinampConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = confirmColor, fontSize = 14.sp, fontFamily = WinampFont, fontWeight = FontWeight.Bold) },
        text = { Text(message, color = WinampTextBright, fontSize = 13.sp, fontFamily = WinampFont) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = confirmColor, fontFamily = WinampFont) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = WinampTextDim, fontFamily = WinampFont) } },
        containerColor = WinampDarkBg,
        shape = RoundedCornerShape(2.dp),
    )
}

@Composable
private fun WinampActionButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(2.dp),
        color = WinampButtonBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, Brush.verticalGradient(listOf(WinampBorderLight, WinampBorderDark))),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = text, color = color, fontSize = 15.sp, fontFamily = WinampFont, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WinampCreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NEW PLAYLIST", color = WinampGreen, fontFamily = WinampFont, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = WinampTextDim) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WinampGreen, unfocusedBorderColor = WinampBorderLight,
                    cursorColor = WinampGreen, focusedTextColor = WinampGreen, unfocusedTextColor = WinampGreenDim,
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("CREATE", color = WinampGreen, fontFamily = WinampFont) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = WinampTextDim, fontFamily = WinampFont) } },
        containerColor = WinampDarkBg,
        shape = RoundedCornerShape(2.dp),
    )
}
