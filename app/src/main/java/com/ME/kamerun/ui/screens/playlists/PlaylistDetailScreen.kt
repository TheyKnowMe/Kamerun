package com.ME.kamerun.ui.screens.playlists

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ME.kamerun.data.local.entities.PlaylistEntity
import com.ME.kamerun.data.local.entities.SongEntity
import com.ME.kamerun.player.MusicPlayer
import com.ME.kamerun.ui.theme.*
import java.io.File

private const val TAG = "PlaylistDetail"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity?,
    songs: List<SongEntity>,
    allSongs: List<SongEntity>,
    onBack: () -> Unit,
    onRemoveSong: (Long) -> Unit,
    onAddSong: (SongEntity) -> Unit,
    musicPlayer: MusicPlayer,
) {
    val playerState by musicPlayer.state.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(songs) {
        Log.d(TAG, "Songs count: ${songs.size}")
        songs.forEachIndexed { i, s ->
            Log.d(TAG, "  [$i] '${s.title}' audioPath=${s.audioPath} thumbPath=${s.thumbnailPath}")
        }
    }

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
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück",
                        tint = WinampTextBright,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = (playlist?.name ?: "PLAYLIST").uppercase(),
                    color = WinampTextBright,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showAddSheet = true }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add songs",
                        tint = WinampTextBright,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Play All Button
        if (songs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WinampPanelBg)
                    .border(1.dp, WinampBorderDark)
                    .clickable {
                        val playable = songs.filter { it.audioPath != null }
                        Log.d(TAG, "PLAY ALL clicked: ${playable.size} playable of ${songs.size}")
                        if (playable.isNotEmpty()) {
                            musicPlayer.play(playable.first(), playable)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = WinampGreen,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "PLAY ALL (${songs.size} TRACKS)",
                    color = WinampGreen,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
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
                        Text(
                            "EMPTY PLAYLIST",
                            color = WinampGreenDim,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            } else {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                    val isCurrent = playerState.currentSong?.id == song.id
                    WinampSongRow(
                        index = index + 1,
                        song = song,
                        isPlaying = isCurrent && playerState.isPlaying,
                        isCurrent = isCurrent,
                        onClick = {
                            Log.d(TAG, "Song clicked: '${song.title}' audioPath=${song.audioPath}")
                            if (isCurrent) {
                                musicPlayer.togglePlayPause()
                            } else {
                                musicPlayer.play(song, songs)
                            }
                        },
                        onRemove = { onRemoveSong(song.id) },
                    )
                }
            }
        }

        // Bottom info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WinampPanelBg)
                .border(1.dp, WinampBorderLight)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            val totalDuration = songs.mapNotNull { it.duration }.sum()
            val min = totalDuration / 60
            val sec = totalDuration % 60
            Text(
                text = "${songs.size} tracks / ${min}:${String.format("%02d", sec)}",
                color = WinampTextDim,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }

    if (showAddSheet) {
        val songsNotInPlaylist = remember(allSongs, songs) {
            val inPlaylistIds = songs.map { it.id }.toSet()
            allSongs.filter { it.id !in inPlaylistIds }
        }
        AddSongSheet(
            songs = songsNotInPlaylist,
            onAdd = { song ->
                onAddSong(song)
            },
            onDismiss = { showAddSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSongSheet(
    songs: List<SongEntity>,
    onAdd: (SongEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(songs, query) {
        if (query.isBlank()) songs
        else songs.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = WinampDarkBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WinampGreenDark)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    "ADD SONGS TO PLAYLIST",
                    color = WinampTextBright,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("SEARCH...", color = WinampGreenDim, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WinampGreen,
                    unfocusedTextColor = WinampGreenDim,
                    focusedBorderColor = WinampGreen,
                    unfocusedBorderColor = WinampBorderDark,
                    cursorColor = WinampGreen,
                ),
                textStyle = MaterialTheme.typography.bodySmall,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = WinampGreenDim, modifier = Modifier.size(16.dp))
                },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = WinampGreenDim, modifier = Modifier.size(14.dp))
                    }}
                } else null,
            )

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (songs.isEmpty()) "ALL SONGS ALREADY IN PLAYLIST" else "NO RESULTS",
                        color = WinampGreenDim,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(filtered, key = { it.id }) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAdd(song) }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val imageModel: Any? = when {
                                song.thumbnailPath != null && File(song.thumbnailPath).exists() -> File(song.thumbnailPath)
                                song.thumbnailUrl != null -> song.thumbnailUrl
                                else -> null
                            }
                            if (imageModel != null) {
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .border(1.dp, WinampBorderDark, RoundedCornerShape(2.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    song.title.uppercase(),
                                    color = WinampGreenDim,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    song.artist.uppercase(),
                                    color = WinampTextDim,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                tint = WinampGreen,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        HorizontalDivider(color = WinampBorderDark.copy(alpha = 0.4f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WinampSongRow(
    index: Int,
    song: SongEntity,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val bgColor = when {
        isCurrent -> WinampGreenDark.copy(alpha = 0.2f)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail
        val imageModel: Any? = when {
            song.thumbnailPath != null && File(song.thumbnailPath).exists() -> File(song.thumbnailPath)
            song.thumbnailUrl != null -> song.thumbnailUrl
            else -> null
        }
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = song.title,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .border(
                        1.dp,
                        if (isCurrent) WinampGreen else WinampBorderDark,
                        RoundedCornerShape(2.dp),
                    ),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Index
        Text(
            text = String.format("%02d", index),
            color = if (isCurrent) WinampGreen else WinampGreenDark,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(20.dp),
        )
        Text(
            text = ". ",
            color = WinampGreenDark,
            style = MaterialTheme.typography.bodySmall,
        )

        // Playing indicator
        if (isPlaying) {
            Text(
                text = "► ",
                color = WinampGreen,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Artist - Title
        Text(
            text = "${song.artist} - ${song.title}".uppercase(),
            color = if (isCurrent) WinampGreen else WinampGreenDim,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        // Duration
        song.duration?.let { dur ->
            val min = dur / 60
            val sec = dur % 60
            Text(
                text = "$min:${String.format("%02d", sec)}",
                color = WinampTextDim,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        // Remove button
        IconButton(onClick = onRemove, modifier = Modifier.size(22.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Entfernen",
                tint = WinampTextDim,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}
