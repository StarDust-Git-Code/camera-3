package dev.camera3.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF000000)
val Panel = Color(0xFF171917)
val Foreground = Color(0xFFF5F5F2)
val Muted = Color(0xFFA5AAA4)
val Accent = Color(0xFF8BC34A)

@Composable
fun Camera3Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Foreground, onPrimary = Ink,
        background = Ink, onBackground = Foreground,
        surface = Panel, onSurface = Foreground,
        secondary = Accent, outline = Color(0xFF343734),
    ), content = content)
}
