package com.github.rodrigotimoteo.kboyemu.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KBoyColorScheme = darkColorScheme(
    primary = GBGreen,
    onPrimary = GBGreenDark,
    primaryContainer = GBGreenMid,
    onPrimaryContainer = GBGreenLight,
    secondary = GBShellAccent,
    onSecondary = GBOnSurface,
    secondaryContainer = GBShellLight,
    onSecondaryContainer = GBOnSurface,
    background = GBBackground,
    onBackground = GBOnSurface,
    surface = GBSurface,
    onSurface = GBOnSurface,
    surfaceVariant = GBShell,
    onSurfaceVariant = GBOnSurfaceVariant,
    error = GBButtonA,
)

/**
 * KBoy Emulator theme — a Game Boy-inspired dark theme with classic green accents
 */
@Composable
fun KBoyEmuTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = KBoyColorScheme,
        typography = Typography,
        content = content,
    )
}
