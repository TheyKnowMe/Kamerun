package com.ME.kamerun.ui.screens.import_songs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ME.kamerun.ui.theme.*

@Composable
fun ImportScreen(
    onBack: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var playlistUrl by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

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
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "YOUTUBE IMPORT",
                    color = WinampTextBright,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WinampDarkBg)
                .border(1.dp, WinampBorderDark)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "PASTE YOUTUBE PLAYLIST URL:",
                color = WinampGreen,
                style = MaterialTheme.typography.labelLarge,
            )

            OutlinedTextField(
                value = playlistUrl,
                onValueChange = { playlistUrl = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "https://youtube.com/playlist?list=...",
                        color = WinampTextDim,
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WinampGreen,
                    unfocusedBorderColor = WinampBorderLight,
                    cursorColor = WinampGreen,
                    focusedTextColor = WinampGreen,
                    unfocusedTextColor = WinampGreenDim,
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardManager.getText()?.let {
                            playlistUrl = it.text
                        }
                    }) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "Einfügen",
                            tint = WinampYellow,
                        )
                    }
                },
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WinampDisplayBg, RoundedCornerShape(2.dp))
                            .border(1.dp, WinampDisplayBorder, RoundedCornerShape(2.dp))
                            .padding(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (uiState.total > 0) {
                                Text(
                                    "DOWNLOADING ${uiState.progress}/${uiState.total}",
                                    color = WinampGreen,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                LinearProgressIndicator(
                                    progress = {
                                        uiState.progress.toFloat() / uiState.total.toFloat()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = WinampGreen,
                                    trackColor = WinampSliderBg,
                                )
                                Text(
                                    uiState.currentSong.uppercase(),
                                    color = WinampGreenDim,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else {
                                Text(
                                    "ANALYZING PLAYLIST...",
                                    color = WinampYellow,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = WinampYellow,
                                    trackColor = WinampSliderBg,
                                )
                            }
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
                        Column {
                            Text(
                                "ERROR",
                                color = WinampRed,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                uiState.error!!,
                                color = WinampRed.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    WinampRetroButton(
                        text = "RETRY",
                        color = WinampYellow,
                        onClick = { viewModel.resetState() },
                    )
                }

                uiState.successCount != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WinampGreen.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                            .border(1.dp, WinampGreen, RoundedCornerShape(2.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "DOWNLOAD COMPLETE",
                                color = WinampGreen,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${uiState.successCount} TRACKS IMPORTED",
                                color = WinampGreenDim,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WinampRetroButton(
                            text = "IMPORT MORE",
                            color = WinampGreen,
                            onClick = {
                                viewModel.resetState()
                                playlistUrl = ""
                            },
                        )
                        WinampRetroButton(
                            text = "DONE",
                            color = WinampYellow,
                            onClick = onBack,
                        )
                    }
                }

                else -> {
                    WinampRetroButton(
                        text = "▶ DOWNLOAD",
                        color = WinampGreen,
                        onClick = { viewModel.importPlaylist(playlistUrl) },
                        enabled = playlistUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun WinampRetroButton(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(2.dp),
        color = if (enabled) WinampButtonBg else WinampBorderDark,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    if (enabled) WinampBorderLight else WinampBorderDark,
                    WinampBorderDark,
                )
            )
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (enabled) color else WinampTextDim,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
