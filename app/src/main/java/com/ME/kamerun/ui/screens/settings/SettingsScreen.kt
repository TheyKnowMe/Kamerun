package com.ME.kamerun.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ME.kamerun.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val config by viewModel.serverConfig.collectAsState()
    var host by remember(config) { mutableStateOf(config.host) }
    var port by remember(config) { mutableStateOf(config.port.toString()) }
    var saved by remember { mutableStateOf(false) }

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
                text = "SETTINGS",
                color = WinampTextBright,
                style = MaterialTheme.typography.labelMedium,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WinampDarkBg)
                .border(1.dp, WinampBorderDark)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "SERVER CONNECTION",
                color = WinampGreen,
                style = MaterialTheme.typography.labelLarge,
            )

            OutlinedTextField(
                value = host,
                onValueChange = { host = it; saved = false },
                label = { Text("IP ADDRESS", color = WinampTextDim) },
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

            OutlinedTextField(
                value = port,
                onValueChange = { port = it; saved = false },
                label = { Text("PORT", color = WinampTextDim) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WinampGreen,
                    unfocusedBorderColor = WinampBorderLight,
                    cursorColor = WinampGreen,
                    focusedTextColor = WinampGreen,
                    unfocusedTextColor = WinampGreenDim,
                ),
            )

            Surface(
                onClick = {
                    val portNum = port.toIntOrNull() ?: 9550
                    viewModel.saveServerConfig(host, portNum)
                    saved = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                color = WinampButtonBg,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.verticalGradient(listOf(WinampBorderLight, WinampBorderDark))
                ),
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (saved) {
                        Text("✓ SAVED", color = WinampGreen, style = MaterialTheme.typography.labelLarge)
                    } else {
                        Text("SAVE", color = WinampYellow, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
