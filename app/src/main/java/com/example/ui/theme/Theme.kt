package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisPrimary,
    onPrimary = JarvisOnPrimary,
    primaryContainer = JarvisSurfaceVariant,
    onPrimaryContainer = JarvisCyanLight,
    secondary = JarvisSecondary,
    onSecondary = JarvisOnSecondary,
    secondaryContainer = Color(0xFF132A4A),
    onSecondaryContainer = JarvisElectricBlue,
    tertiary = JarvisAmber,
    onTertiary = Color(0xFF2E1700),
    background = JarvisBackground,
    onBackground = JarvisTextPrimary,
    surface = JarvisSurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisSurfaceBorder,
    outlineVariant = Color(0xFF1E293B),
    error = JarvisRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // JARVIS is fundamentally an immersive futuristic dark HUD interface
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
