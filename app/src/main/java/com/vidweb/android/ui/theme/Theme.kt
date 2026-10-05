package com.vidweb.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF14171C)
val Surface = Color(0xFF1C2127)
val Surface2 = Color(0xFF232A33)
val Border = Color(0xFF313A45)
val TextMain = Color(0xFFE8EAED)
val TextDim = Color(0xFF9AA4B2)
val Accent = Color(0xFF00A4DC)
val AccentHover = Color(0xFF4FC3F7)
val Danger = Color(0xFFE5534B)

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0B3D52),
    onPrimaryContainer = AccentHover,
    secondary = AccentHover,
    background = Bg,
    onBackground = TextMain,
    surface = Surface,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    outline = Border,
    error = Danger,
)

@Composable
fun VidWebTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
