package com.zirakusa.gatewayhub.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Same palette as the hub's web panel: night background, gold accent. */
object HubColors {
    val Background = Color(0xFF111015)
    val Card = Color(0xFF1D1B24)
    val Text = Color(0xFFF3F0FA)
    val Muted = Color(0xFFA49EB3)
    val Line = Color(0xFF3A3646)
    val Gold = Color(0xFFE8B931)
    val Good = Color(0xFF7DDC9A)
    val Bad = Color(0xFFFF7A7A)
}

@Composable fun HubTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = HubColors.Gold, onPrimary = Color(0xFF1B1724), background = HubColors.Background,
        surface = HubColors.Card, onSurface = HubColors.Text, onBackground = HubColors.Text,
        surfaceVariant = HubColors.Card, onSurfaceVariant = HubColors.Muted, outline = HubColors.Line, error = HubColors.Bad,
    ),
    content = content,
)
