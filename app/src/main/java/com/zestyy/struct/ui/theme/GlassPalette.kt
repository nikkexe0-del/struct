package com.zestyy.struct.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GlassPalette(
    val surfaceDefault: Color,
    val surfaceOpaque: Color,
    val border: Color,
    val highlight: Color,
    val accent: Color
)

val DarkGlassPalette = GlassPalette(
    surfaceDefault = GlassSurfaceLight,
    surfaceOpaque = GlassSurfaceOpaque,
    border = GlassBorder,
    highlight = GlassHighlight,
    accent = ByteOrange
)

val LightGlassPalette = GlassPalette(
    surfaceDefault = LightGlassSurface,
    surfaceOpaque = LightGlassSurfaceOpaque,
    border = LightGlassBorder,
    highlight = Color(0xCCFFFFFF),
    accent = ByteOrange
)

val GreenGlassPalette = GlassPalette(
    surfaceDefault = GreenGlassSurface,
    surfaceOpaque = GreenGlassSurfaceOpaque,
    border = GreenGlassBorder,
    highlight = Color(0x6634D058),
    accent = GreenAccent
)

val LocalGlassPalette = staticCompositionLocalOf { DarkGlassPalette }
