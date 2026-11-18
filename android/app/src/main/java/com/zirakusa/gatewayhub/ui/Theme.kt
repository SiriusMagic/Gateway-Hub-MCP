package com.zirakusa.gatewayhub.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.zirakusa.gatewayhub.R
import com.zirakusa.gatewayhub.data.ThemeChoice

/** The colours of one theme. "accent" is the gold of the original night theme. */
data class HubPalette(
    val label: Int, val dark: Boolean, val background: Color, val card: Color, val text: Color, val muted: Color,
    val line: Color, val accent: Color, val onAccent: Color, val good: Color, val bad: Color,
)

val PALETTES = mapOf(
    ThemeChoice.NIGHT to HubPalette(R.string.theme_night, true, Color(0xFF111015), Color(0xFF1D1B24), Color(0xFFF3F0FA),
        Color(0xFFA49EB3), Color(0xFF3A3646), Color(0xFFE8B931), Color(0xFF1B1724), Color(0xFF7DDC9A), Color(0xFFFF7A7A)),
    ThemeChoice.OCEAN to HubPalette(R.string.theme_ocean, true, Color(0xFF0B1320), Color(0xFF14223A), Color(0xFFEAF2FF),
        Color(0xFF93A6C4), Color(0xFF2A3B57), Color(0xFF4FC3F7), Color(0xFF06223A), Color(0xFF7DDCB4), Color(0xFFFF8A80)),
    ThemeChoice.FOREST to HubPalette(R.string.theme_forest, true, Color(0xFF0E1511), Color(0xFF18241C), Color(0xFFEAF5EC),
        Color(0xFF9BB3A1), Color(0xFF2E4235), Color(0xFF9CCC65), Color(0xFF13240F), Color(0xFF81E6A0), Color(0xFFFF8A80)),
    ThemeChoice.RUBY to HubPalette(R.string.theme_ruby, true, Color(0xFF160D12), Color(0xFF26161F), Color(0xFFFBEFF4),
        Color(0xFFB79AA8), Color(0xFF45293A), Color(0xFFF06292), Color(0xFF2B0A18), Color(0xFF7DDC9A), Color(0xFFFFAB91)),
    ThemeChoice.LIGHT to HubPalette(R.string.theme_light, false, Color(0xFFF6F4F9), Color(0xFFFFFFFF), Color(0xFF1B1724),
        Color(0xFF625B71), Color(0xFFD9D4E3), Color(0xFF8A5A00), Color(0xFFFFFFFF), Color(0xFF1E7B3C), Color(0xFFB3261E)),
)

val LocalPalette = staticCompositionLocalOf { PALETTES.getValue(ThemeChoice.NIGHT) }

/** The current theme's colours, read where they are drawn. */
object HubColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.background
    val Card: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.card
    val Text: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.text
    val Muted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.muted
    val Line: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.line
    val Gold: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.accent
    val OnGold: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.onAccent
    val Good: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.good
    val Bad: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bad
}

@Composable fun HubTheme(theme: ThemeChoice = ThemeChoice.NIGHT, content: @Composable () -> Unit) {
    val p = PALETTES.getValue(theme)
    val scheme = if (p.dark) darkColorScheme(
        primary = p.accent, onPrimary = p.onAccent, background = p.background, surface = p.card, onSurface = p.text,
        onBackground = p.text, surfaceVariant = p.card, onSurfaceVariant = p.muted, outline = p.line, error = p.bad,
    ) else lightColorScheme(
        primary = p.accent, onPrimary = p.onAccent, background = p.background, surface = p.card, onSurface = p.text,
        onBackground = p.text, surfaceVariant = p.card, onSurfaceVariant = p.muted, outline = p.line, error = p.bad,
    )
    CompositionLocalProvider(LocalPalette provides p) { MaterialTheme(colorScheme = scheme, content = content) }
}
