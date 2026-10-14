package com.quran.halaqareport.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

val EmeraldPrimary = Color(0xFF0D4F3C)
val GoldAccent = Color(0xFFC69C45)
val IvoryBackground = Color(0xFFF9FBF9)
val SoftCardSurface = Color(0xFFFFFFFF)

private val LightIslamicPalette = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EFE6),
    onPrimaryContainer = Color(0xFF05291E),
    secondary = GoldAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF9ECCF),
    onSecondaryContainer = Color(0xFF423007),
    background = IvoryBackground,
    surface = SoftCardSurface
)

private val DarkIslamicPalette = darkColorScheme(
    primary = Color(0xFF66D4B0),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF0D4F3C),
    onPrimaryContainer = Color(0xFF83F1CC),
    secondary = Color(0xFFE5C168),
    background = Color(0xFF0F1714),
    surface = Color(0xFF182420)
)

@Composable
fun HalaqaReportTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkIslamicPalette else LightIslamicPalette
    // هذا السطر يضمن أن التطبيق بالكامل من اليمين لليسار (اللغة العربية)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
