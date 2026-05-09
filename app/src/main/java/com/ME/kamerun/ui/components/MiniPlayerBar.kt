package com.ME.kamerun.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.ME.kamerun.player.PlayerState
import com.ME.kamerun.ui.theme.*
import java.io.File

@Composable
fun MiniPlayerBar(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = playerState.currentSong ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WinampDarkBg)
            .border(1.dp, WinampBorderLight),
    ) {
        // Progress bar
        if (playerState.duration > 0) {
            LinearProgressIndicator(
                progress = {
                    playerState.currentPosition.toFloat() / playerState.duration.toFloat()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = WinampGreen,
                trackColor = WinampSliderBg,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            val imageModel: Any? = when {
                song.thumbnailPath != null && File(song.thumbnailPath).exists() -> File(song.thumbnailPath)
                song.thumbnailUrl != null -> song.thumbnailUrl
                else -> null
            }
            AsyncImage(
                model = imageModel,
                contentDescription = song.title,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .border(1.dp, WinampGreenDark, RoundedCornerShape(2.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title.uppercase(),
                    color = WinampGreen,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist.uppercase(),
                    color = WinampTextDim,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Controls
            IconButton(onClick = onPrevious, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Zurück",
                    tint = WinampTextBright, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onPlayPause, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = WinampGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Weiter",
                    tint = WinampTextBright, modifier = Modifier.size(16.dp))
            }
        }
    }
}
