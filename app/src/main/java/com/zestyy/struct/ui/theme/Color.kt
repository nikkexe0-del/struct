package com.zestyy.struct.ui.theme

import androidx.compose.ui.graphics.Color

// Brand
val ByteOrange = Color(0xFFFF7A1A)
val ByteOrangeBright = Color(0xFFFF9E4D)
val ByteBlack = Color(0xFF0A0A0B)
val ByteCharcoal = Color(0xFF17171A)

// Glass surfaces — translucent, layered over ByteBlack backgrounds
val GlassSurfaceLight = Color(0x33FFFFFF) // 20% white
val GlassSurfaceDark = Color(0x1AFFFFFF)  // 10% white
val GlassSurfaceOpaque = Color(0xE6141416) // ~90% opaque charcoal — for stat bars sitting over map tiles, where a translucent tint reads as invisible against bright imagery
val GlassBorder = Color(0x40FFFFFF)       // 25% white hairline
val GlassHighlight = Color(0x66FFFFFF)    // top-edge specular highlight

// Text
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFFA1A1A6)
val TextTertiary = Color(0xFF6E6E73)

// Status
val SuccessGreen = Color(0xFF32D74B)
val WarnAmber = Color(0xFFFFB020)
val DangerRed = Color(0xFFFF453A)

// --- Light theme palette ---
val LightBackground = Color(0xFFF5F5F7)
val LightSurface = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF1C1C1E)
val LightTextSecondary = Color(0xFF6E6E73)
val LightGlassSurface = Color(0x99FFFFFF)
val LightGlassSurfaceOpaque = Color(0xF2FFFFFF)
val LightGlassBorder = Color(0x1F000000)

// --- Green theme palette (dark base, green accent) ---
val GreenAccent = Color(0xFF34D058)
val GreenAccentBright = Color(0xFF5CE577)
val GreenBackground = Color(0xFF07120A)
val GreenSurface = Color(0xFF102014)
val GreenGlassSurface = Color(0x3334D058)
val GreenGlassSurfaceOpaque = Color(0xE60F1B12)
val GreenGlassBorder = Color(0x4034D058)
