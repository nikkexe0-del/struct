package com.zestyy.struct.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkScheme = darkColorScheme(
    primary = ByteOrange,
    onPrimary = Color.White,
    secondary = ByteOrangeBright,
    background = ByteBlack,
    onBackground = TextPrimary,
    surface = ByteCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurfaceDark,
    outline = GlassBorder,
    error = DangerRed,
)

private val LightScheme = lightColorScheme(
    primary = ByteOrange,
    onPrimary = Color.White,
    secondary = Color(0xFFCC5F0F),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightGlassSurface,
    outline = LightGlassBorder,
    error = DangerRed,
)

private val GreenScheme = darkColorScheme(
    primary = GreenAccent,
    onPrimary = Color.Black,
    secondary = GreenAccentBright,
    background = GreenBackground,
    onBackground = TextPrimary,
    surface = GreenSurface,
    onSurface = TextPrimary,
    surfaceVariant = GreenGlassSurface,
    outline = GreenGlassBorder,
    error = DangerRed,
)

val StructShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),   // default card radius — iOS-style continuous corner feel
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun StructTheme(content: @Composable () -> Unit) {
    val mode by ThemeManager.mode.collectAsState()

    val (colorScheme, glassPalette) = when (mode) {
        AppThemeMode.DARK -> DarkScheme to DarkGlassPalette
        AppThemeMode.LIGHT -> LightScheme to LightGlassPalette
        AppThemeMode.GREEN -> GreenScheme to GreenGlassPalette
    }

    CompositionLocalProvider(LocalGlassPalette provides glassPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StructTypography,
            shapes = StructShapes,
            content = content
        )
    }
}
