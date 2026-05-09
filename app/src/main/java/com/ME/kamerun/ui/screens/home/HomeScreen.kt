package com.ME.kamerun.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ME.kamerun.data.local.entities.PlaylistEntity
import com.ME.kamerun.data.local.entities.SongEntity
import com.ME.kamerun.player.MusicPlayer
import com.ME.kamerun.player.PlayerState
import com.ME.kamerun.ui.screens.playlists.PlaylistsViewModel
import com.ME.kamerun.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun HomeScreen(
    onPlaylistClick: (Long) -> Unit,
    musicPlayer: MusicPlayer,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsState()
    val playerState by musicPlayer.state.collectAsState()

    // Position updater
    var currentPosition by remember { mutableStateOf(0L) }
    LaunchedEffect(playerState.isPlaying) {
        while (playerState.isPlaying) {
            currentPosition = musicPlayer.getCurrentPosition()
            delay(200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WinampBlack),
    ) {
        // ── Winamp Player Window ──
        WinampPlayerPanel(
            playerState = playerState.copy(currentPosition = currentPosition),
            onPlayPause = { musicPlayer.togglePlayPause() },
            onNext = { musicPlayer.next() },
            onPrevious = { musicPlayer.previous() },
            onSeek = { musicPlayer.seekTo(it) },
        )

        // ── Playlist List (wie Winamp Playlist Editor) ──
        WinampPlaylistPanel(
            playlists = playlists,
            onPlaylistClick = onPlaylistClick,
            onDelete = { viewModel.deletePlaylist(it) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WinampPlayerPanel(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    val song = playerState.currentSong

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(WinampPanelBg, WinampDarkBg, WinampBorderDark)
                )
            )
            .border(1.dp, WinampBorderLight, RoundedCornerShape(0.dp))
            .padding(2.dp),
    ) {
        // ── Title Bar ──
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
                text = "KAMERUN v1.0",
                color = WinampTextBright,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        // ── Display ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .background(WinampDisplayBg, RoundedCornerShape(2.dp))
                .border(1.dp, WinampDisplayBorder, RoundedCornerShape(2.dp))
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Thumbnail
                if (song != null) {
                    val imageModel: Any? = when {
                        song.thumbnailPath != null && File(song.thumbnailPath).exists() -> File(song.thumbnailPath)
                        song.thumbnailUrl != null -> song.thumbnailUrl
                        else -> null
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .border(1.dp, WinampGreenDark, RoundedCornerShape(2.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Timer
                    val posSeconds = (playerState.currentPosition / 1000).toInt()
                    val posMin = posSeconds / 60
                    val posSec = posSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", posMin, posSec),
                        color = WinampGreen,
                        fontSize = 28.sp,
                        fontFamily = WinampFont,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Song title – scrolling marquee effect
                    if (song != null) {
                        Text(
                            text = "${song.artist} - ${song.title}",
                            color = WinampGreen,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Text(
                            text = "- KAMERUN PLAYER -",
                            color = WinampGreenDim,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Bitrate / kHz info
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WinampInfoBadge("320", "kbps")
                        WinampInfoBadge("44", "kHz")
                    }
                }
            }
        }

        // ── Seek Bar ──
        if (playerState.duration > 0) {
            WinampSeekBar(
                position = playerState.currentPosition,
                duration = playerState.duration,
                onSeek = onSeek,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // ── Transport Controls ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WinampButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Zurück",
                    tint = WinampTextBright, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            WinampButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                    tint = WinampTextBright,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(2.dp))
            WinampButton(onClick = {}) {
                Icon(Icons.Default.Stop, contentDescription = "Stop",
                    tint = WinampTextBright, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            WinampButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Weiter",
                    tint = WinampTextBright, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun WinampInfoBadge(value: String, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            color = WinampGreen,
            fontSize = 10.sp,
            fontFamily = WinampFont,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = unit,
            color = WinampGreenDark,
            fontSize = 8.sp,
            fontFamily = WinampFont,
        )
    }
}

@Composable
private fun WinampSeekBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderPosition by remember(position) { mutableFloatStateOf(position.toFloat() / duration.toFloat()) }

    Slider(
        value = sliderPosition,
        onValueChange = { sliderPosition = it },
        onValueChangeFinished = { onSeek((sliderPosition * duration).toLong()) },
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        colors = SliderDefaults.colors(
            thumbColor = WinampGreen,
            activeTrackColor = WinampGreen,
            inactiveTrackColor = WinampSliderBg,
        ),
    )
}

@Composable
private fun WinampButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(2.dp),
        color = WinampButtonBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(
                colors = listOf(WinampBorderLight, WinampBorderDark)
            )
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

// ── Playlist Panel (untere Hälfte) ──

@Composable
private fun WinampPlaylistPanel(
    playlists: List<PlaylistEntity>,
    onPlaylistClick: (Long) -> Unit,
    onDelete: (PlaylistEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(WinampPanelBg, WinampDarkBg)
                )
            )
            .border(1.dp, WinampBorderLight),
    ) {
        // Panel Title
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
                text = "PLAYLISTS",
                color = WinampTextBright,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(WinampDisplayBg),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "NO PLAYLISTS",
                        color = WinampGreenDim,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Gehe zu \"Hinzufügen\" Tab",
                        color = WinampTextDim,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(WinampDisplayBg)
                    .padding(2.dp),
            ) {
                itemsIndexed(playlists, key = { _, p -> p.id }) { index, playlist ->
                    WinampPlaylistRow(
                        index = index + 1,
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) },
                        onDelete = { onDelete(playlist) },
                    )
                }
            }
        }

        // Bottom info bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WinampPanelBg)
                .border(
                    width = 1.dp,
                    color = WinampBorderLight,
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = "${playlists.size} playlists",
                color = WinampTextDim,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun WinampPlaylistRow(
    index: Int,
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Index
        Text(
            text = String.format("%02d", index),
            color = WinampGreenDark,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(22.dp),
        )
        Text(
            text = ". ",
            color = WinampGreenDark,
            style = MaterialTheme.typography.bodySmall,
        )

        // Vibe Icon
        if (playlist.isAiGenerated) {
            Text(
                text = "★ ",
                color = WinampYellow,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Playlist Name
        Text(
            text = playlist.name.uppercase(),
            color = WinampGreen,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        // Vibe subtitle
        if (playlist.isAiGenerated && playlist.vibe != null) {
            Text(
                text = playlist.vibe.uppercase(),
                color = WinampTextDim,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        // Delete
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Löschen",
                tint = WinampTextDim,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
