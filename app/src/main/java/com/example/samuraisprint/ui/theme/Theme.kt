package com.example.samuraisprint.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SamuraiBg = Color(0xFF101017)
val SamuraiSurface = Color(0xFF1A1A24)
val SamuraiCard = Color(0xFF222232)
val SamuraiCrimson = Color(0xFFE52538)
val SamuraiCrimsonGlow = Color(0xFFFF4D5E)
val SamuraiGold = Color(0xFFFFB800)
val SamuraiGoldGlow = Color(0xFFFFD56B)
val SamuraiCyan = Color(0xFF00E5FF)
val SamuraiPurple = Color(0xFF9D4EDD)
val SamuraiJade = Color(0xFF00E676)
val TextPrimary = Color(0xFFF0F0F5)
val TextSecondary = Color(0xFFA0A0B5)
val TextMuted = Color(0xFF6B6B80)

private val DarkColorScheme = darkColorScheme(
    primary = SamuraiCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0A10),
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = SamuraiGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF4A3700),
    onSecondaryContainer = Color(0xFFFFE088),
    tertiary = SamuraiCyan,
    background = SamuraiBg,
    onBackground = TextPrimary,
    surface = SamuraiSurface,
    onSurface = TextPrimary,
    surfaceVariant = SamuraiCard,
    onSurfaceVariant = TextSecondary
)

@Composable
fun SamuraiSprintTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
