package com.ME.kamerun.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ME.kamerun.data.local.entities.PlaylistEntity
import com.ME.kamerun.player.MusicPlayer
import com.ME.kamerun.player.PlayerState
import com.ME.kamerun.ui.screens.add.WinampConfirmDialog
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
    var playlistToDelete by remember { mutableStateOf<PlaylistEntity?>(null) }

    var currentPosition by remember { mutableStateOf(0L) }
    LaunchedEffect(playerState.isPlaying) {
        while (playerState.isPlaying) {
            currentPosition = musicPlayer.getCurrentPosition()
            delay(200)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(WinampBlack)) {
        WinampPlayerPanel(
            playerState = playerState.copy(currentPosition = currentPosition),
            onPlayPause = { musicPlayer.togglePlayPause() },
            onNext = { musicPlayer.next() },
            onPrevious = { musicPlayer.previous() },
            onSeek = { musicPlayer.seekTo(it) },
            onToggleShuffle = { musicPlayer.toggleShuffle() },
            onToggleRepeat = { musicPlayer.toggleRepeat() },
            onToggleEq = { musicPlayer.toggleEq() },
            onSetEqBand = { band, value -> musicPlayer.setEqBand(band, value) },
        )
        WinampPlaylistPanel(
            playlists = playlists,
            onPlaylistClick = onPlaylistClick,
            onDeleteRequest = { playlistToDelete = it },
            modifier = Modifier.weight(1f),
        )
    }

    playlistToDelete?.let { playlist ->
        WinampConfirmDialog(
            title = "DELETE PLAYLIST",
            message = "\"${playlist.name}\" löschen?",
            confirmText = "DELETE",
            confirmColor = WinampRed,
            onConfirm = { viewModel.deletePlaylist(playlist); playlistToDelete = null },
            onDismiss = { playlistToDelete = null },
        )
    }
}

@Composable
private fun WinampLed(on: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (on) WinampGreen else WinampGreenDark)
    )
}

@Composable
private fun WinampToggleButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(2.dp),
        color = if (active) WinampGreenDark.copy(alpha = 0.4f) else WinampButtonBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, if (active) WinampGreen else WinampBorderLight,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            WinampLed(on = active)
            Text(
                text = label,
                color = if (active) WinampGreen else WinampTextDim,
                fontSize = 10.sp, fontFamily = WinampFont, fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun WinampPlayerPanel(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleEq: () -> Unit,
    onSetEqBand: (Int, Int) -> Unit,
) {
    val song = playerState.currentSong

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(WinampPanelBg, WinampDarkBg, WinampBorderDark)))
            .border(1.dp, WinampBorderLight, RoundedCornerShape(0.dp))
            .padding(4.dp),
    ) {
        // Title Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(WinampGreenDark, WinampDarkBg, WinampGreenDark)))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("KAMERUN v1.0", color = WinampTextBright, fontSize = 13.sp, fontFamily = WinampFont, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .background(WinampDisplayBg, RoundedCornerShape(2.dp))
                .border(1.dp, WinampDisplayBorder, RoundedCornerShape(2.dp))
                .padding(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                            .size(90.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, WinampGreenDark, RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    val posSeconds = (playerState.currentPosition / 1000).toInt()
                    Text(
                        text = String.format("%02d:%02d", posSeconds / 60, posSeconds % 60),
                        color = WinampGreen, fontSize = 40.sp, fontFamily = WinampFont,
                        fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (song != null) {
                        Text(song.artist, color = WinampGreenDim, fontSize = 13.sp, fontFamily = WinampFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.title, color = WinampGreen, fontSize = 14.sp, fontFamily = WinampFont, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text("- KAMERUN PLAYER -", color = WinampGreenDim, fontSize = 14.sp, fontFamily = WinampFont)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WinampInfoBadge("320", "kbps")
                        WinampInfoBadge("44", "kHz")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Seek Bar
        if (playerState.duration > 0) {
            WinampSeekBar(
                position = playerState.currentPosition,
                duration = playerState.duration,
                onSeek = onSeek,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WinampButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Zurück", tint = WinampTextBright, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            WinampButton(onClick = onPlayPause, size = 56) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null, tint = WinampGreen, modifier = Modifier.size(32.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            WinampButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Weiter", tint = WinampTextBright, modifier = Modifier.size(28.dp))
            }
        }

        // Shuffle / Repeat / EQ Toggles
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            WinampToggleButton("SHUFFLE", playerState.isShuffle, onToggleShuffle, Modifier.weight(1f))
            WinampToggleButton("REPEAT", playerState.isRepeat, onToggleRepeat, Modifier.weight(1f))
            WinampToggleButton("EQ", playerState.isEqEnabled, onToggleEq, Modifier.weight(1f))
        }

        // EQ Panel
        if (playerState.isEqEnabled) {
            Spacer(modifier = Modifier.height(4.dp))
            WinampEqualizerPanel(bands = playerState.eqBands, onBandChange = onSetEqBand)
        }
    }
}

@Composable
private fun WinampEqualizerPanel(
    bands: List<Int>,
    onBandChange: (Int, Int) -> Unit,
) {
    val bandLabels = listOf("70 Hz", "180 Hz", "320 Hz", "1 kHz", "3 kHz")
    val sliderHeight = 140.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .background(WinampDisplayBg, RoundedCornerShape(2.dp))
            .border(1.dp, WinampDisplayBorder, RoundedCornerShape(2.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        // EQ Title Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(WinampGreenDark, WinampDarkBg, WinampGreenDark)))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                "WINAMP EQUALIZER",
                color = WinampTextBright,
                fontSize = 13.sp,
                fontFamily = WinampFont,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // dB Skala
            Column(
                modifier = Modifier.width(36.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("+12", color = WinampGreenDim, fontSize = 10.sp, fontFamily = WinampFont)
                Spacer(modifier = Modifier.height(42.dp))
                Text("0", color = WinampGreenDim, fontSize = 10.sp, fontFamily = WinampFont)
                Spacer(modifier = Modifier.height(42.dp))
                Text("-12", color = WinampGreenDim, fontSize = 10.sp, fontFamily = WinampFont)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 5 Bänder
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                bands.forEachIndexed { index, value ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .height(sliderHeight)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Slider(
                                value = value.toFloat(),
                                onValueChange = { onBandChange(index, it.toInt()) },
                                valueRange = -100f..100f,
                                modifier = Modifier
                                    .width(sliderHeight)
                                    .rotate(-90f),
                                colors = SliderDefaults.colors(
                                    thumbColor = WinampYellow,
                                    activeTrackColor = WinampGreen,
                                    inactiveTrackColor = WinampSliderBg,
                                ),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = bandLabels[index],
                            color = WinampGreenDim,
                            fontSize = 10.sp,
                            fontFamily = WinampFont,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WinampInfoBadge(value: String, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, color = WinampGreen, fontSize = 13.sp, fontFamily = WinampFont, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(3.dp))
        Text(unit, color = WinampGreenDark, fontSize = 10.sp, fontFamily = WinampFont)
    }
}

@Composable
private fun WinampSeekBar(position: Long, duration: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    var sliderPosition by remember(position) { mutableFloatStateOf(position.toFloat() / duration.toFloat()) }
    Slider(
        value = sliderPosition,
        onValueChange = { sliderPosition = it },
        onValueChangeFinished = { onSeek((sliderPosition * duration).toLong()) },
        modifier = modifier.fillMaxWidth().height(24.dp),
        colors = SliderDefaults.colors(thumbColor = WinampGreen, activeTrackColor = WinampGreen, inactiveTrackColor = WinampSliderBg),
    )
}

@Composable
private fun WinampButton(onClick: () -> Unit, size: Int = 48, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size.dp),
        shape = RoundedCornerShape(2.dp),
        color = WinampButtonBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, Brush.verticalGradient(listOf(WinampBorderLight, WinampBorderDark))),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun WinampPlaylistPanel(
    playlists: List<PlaylistEntity>,
    onPlaylistClick: (Long) -> Unit,
    onDeleteRequest: (PlaylistEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .background(Brush.verticalGradient(listOf(WinampPanelBg, WinampDarkBg)))
            .border(1.dp, WinampBorderLight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(WinampGreenDark, WinampDarkBg, WinampGreenDark)))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("PLAYLISTS", color = WinampTextBright, fontSize = 13.sp, fontFamily = WinampFont, fontWeight = FontWeight.Bold)
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).background(WinampDisplayBg),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NO PLAYLISTS", color = WinampGreenDim, fontSize = 16.sp, fontFamily = WinampFont)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Gehe zu \"Hinzufügen\" Tab", color = WinampTextDim, fontSize = 12.sp, fontFamily = WinampFont)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).background(WinampDisplayBg).padding(4.dp),
            ) {
                itemsIndexed(playlists, key = { _, p -> p.id }) { index, playlist ->
                    WinampPlaylistRow(
                        index = index + 1,
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) },
                        onDeleteRequest = { onDeleteRequest(playlist) },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WinampPanelBg)
                .border(1.dp, WinampBorderLight)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text("${playlists.size} playlists", color = WinampTextDim, fontSize = 11.sp, fontFamily = WinampFont)
        }
    }
}

@Composable
private fun WinampPlaylistRow(
    index: Int,
    playlist: PlaylistEntity,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(String.format("%02d", index), color = WinampGreenDark, fontSize = 13.sp, fontFamily = WinampFont, modifier = Modifier.width(28.dp))
        Text(". ", color = WinampGreenDark, fontSize = 13.sp, fontFamily = WinampFont)
        if (playlist.isAiGenerated) {
            Text("★ ", color = WinampYellow, fontSize = 14.sp, fontFamily = WinampFont)
        }
        Text(
            text = playlist.name.uppercase(),
            color = WinampGreen, fontSize = 15.sp, fontFamily = WinampFont,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (playlist.isAiGenerated && playlist.vibe != null) {
            Text(playlist.vibe.uppercase(), color = WinampTextDim, fontSize = 10.sp, fontFamily = WinampFont, maxLines = 1, modifier = Modifier.padding(start = 6.dp))
        }
        IconButton(onClick = onDeleteRequest, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Löschen", tint = WinampTextDim, modifier = Modifier.size(18.dp))
        }
    }
}
