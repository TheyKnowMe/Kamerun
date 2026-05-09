package com.ME.kamerun.ui.screens.playlists

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistEntity?,
    songs: List<SongEntity>,
    onBack: () -> Unit,
    onRemoveSong: (Long) -> Unit,
    musicPlayer: MusicPlayer,
) {
    val playerState by musicPlayer.state.collectAsState()

    // Debug logging
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
