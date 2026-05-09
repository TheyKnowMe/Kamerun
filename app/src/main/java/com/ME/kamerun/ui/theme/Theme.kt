package com.ME.kamerun.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WinampColorScheme = darkColorScheme(
    primary = WinampGreen,
    onPrimary = WinampBlack,
    primaryContainer = WinampGreenDark,
    onPrimaryContainer = WinampGreen,

    secondary = WinampYellow,
    onSecondary = WinampBlack,
    secondaryContainer = WinampPanelBg,
    onSecondaryContainer = WinampYellow,

    tertiary = WinampCyan,
    onTertiary = WinampBlack,
    tertiaryContainer = WinampPanelBg,
    onTertiaryContainer = WinampCyan,

    error = WinampRed,
    onError = WinampBlack,
    errorContainer = Color(0xFF3D0000),
    onErrorContainer = WinampRed,

    background = WinampBlack,
    onBackground = WinampGreen,

    surface = WinampDarkBg,
    onSurface = WinampGreen,
    surfaceVariant = WinampPanelBg,
    onSurfaceVariant = WinampTextDim,

    surfaceContainerHighest = WinampMidBg,
    surfaceContainerHigh = WinampDarkBg,
    surfaceContainer = WinampDarkBg,
    surfaceContainerLow = WinampBlack,
    surfaceContainerLowest = WinampBlack,

    outline = WinampBorderLight,
    outlineVariant = WinampBorderDark,

    inverseSurface = WinampGreen,
    inverseOnSurface = WinampBlack,
    inversePrimary = WinampGreenDark,
)

@Composable
fun KamerunTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = WinampBlack.toArgb()
            window.navigationBarColor = WinampBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = WinampColorScheme,
        typography = Typography,
        content = content,
    )
}
