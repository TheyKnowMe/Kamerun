package com.ME.kamerun.ui.screens.vibe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ME.kamerun.ui.screens.settings.SettingsViewModel
import com.ME.kamerun.ui.theme.*

@Composable
fun VibeGeneratorScreen(
    onSaved: () -> Unit,
    vibeViewModel: VibeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val allSongs by vibeViewModel.allSongs.collectAsState()
    val uiState by vibeViewModel.uiState.collectAsState()
    val serverConfig by settingsViewModel.serverConfig.collectAsState()

    var vibeText by remember { mutableStateOf("") }

    val vibeChips = listOf(
        "Party", "Chill", "Workout", "Focus",
        "Road Trip", "Romantic", "Gaming", "Jazz",
    )

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
                text = "AI VIBE GENERATOR",
                color = WinampTextBright,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WinampDarkBg)
                .border(1.dp, WinampBorderDark)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Vibe Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(vibeChips) { chip ->
                    Surface(
                        onClick = { vibeText = chip },
                        shape = RoundedCornerShape(2.dp),
                        color = if (vibeText == chip) WinampGreenDark else WinampButtonBg,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (vibeText == chip) WinampGreen else WinampBorderLight,
                        ),
                    ) {
                        Text(
                            text = chip.uppercase(),
                            color = if (vibeText == chip) WinampGreen else WinampTextDim,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            // Vibe Text
            OutlinedTextField(
                value = vibeText,
                onValueChange = { vibeText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe your vibe...", color = WinampTextDim) },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WinampCyan,
                    unfocusedBorderColor = WinampBorderLight,
                    cursorColor = WinampCyan,
                    focusedTextColor = WinampCyan,
                    unfocusedTextColor = WinampGreenDim,
                ),
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WinampDisplayBg, RoundedCornerShape(2.dp))
                            .border(1.dp, WinampDisplayBorder, RoundedCornerShape(2.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = WinampCyan,
                                trackColor = WinampSliderBg,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "AI IS GENERATING...",
                                color = WinampCyan,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WinampRed.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                            .border(1.dp, WinampRed, RoundedCornerShape(2.dp))
                            .padding(12.dp),
                    ) {
                        Text(
                            uiState.error!!.uppercase(),
                            color = WinampRed,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Surface(
                        onClick = { vibeViewModel.resetState() },
                        shape = RoundedCornerShape(2.dp),
                        color = WinampButtonBg,
                    ) {
                        Text(
                            "RETRY",
                            color = WinampYellow,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                uiState.resultSongs != null -> {
                    val resultSongs = uiState.resultSongs!!
                    Text(
                        "${resultSongs.size} TRACKS SELECTED",
                        color = WinampCyan,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = {
                                vibeViewModel.saveGeneratedPlaylist(
                                    name = "Vibe: $vibeText",
                                    vibe = vibeText,
                                    songs = resultSongs,
                                )
                                onSaved()
                            },
                            shape = RoundedCornerShape(2.dp),
                            color = WinampButtonBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, WinampGreen),
                        ) {
                            Text(
                                "✓ SAVE PLAYLIST",
                                color = WinampGreen,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                        Surface(
                            onClick = { vibeViewModel.resetState() },
                            shape = RoundedCornerShape(2.dp),
                            color = WinampButtonBg,
                        ) {
                            Text(
                                "DISCARD",
                                color = WinampTextDim,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(resultSongs, key = { it.id }) { song ->
                            Text(
                                text = "♪ ${song.artist} - ${song.title}".uppercase(),
                                color = WinampGreenDim,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 3.dp),
                            )
                        }
                    }
                }

                else -> {
                    Surface(
                        onClick = {
                            vibeViewModel.generatePlaylist(
                                vibe = vibeText,
                                songs = allSongs,
                                host = serverConfig.host,
                                port = serverConfig.port,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = vibeText.isNotBlank() && allSongs.isNotEmpty(),
                        shape = RoundedCornerShape(2.dp),
                        color = if (vibeText.isNotBlank() && allSongs.isNotEmpty()) WinampButtonBg else WinampBorderDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (vibeText.isNotBlank()) WinampCyan else WinampBorderDark,
                        ),
                    ) {
                        Box(
                            modifier = Modifier.padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "★ GENERATE (${allSongs.size} TRACKS)",
                                color = if (vibeText.isNotBlank() && allSongs.isNotEmpty()) WinampCyan else WinampTextDim,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    if (allSongs.isEmpty()) {
                        Text(
                            "Import songs first",
                            color = WinampTextDim,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
