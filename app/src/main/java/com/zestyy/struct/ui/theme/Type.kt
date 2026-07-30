package com.zestyy.struct.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zestyy.struct.R

/**
 * Real Inter, loaded from a single variable font file (res/font/inter_variable.ttf — the OFL
 * `Inter[opsz,wght].ttf` from google/fonts, verified as valid sfnt data before being committed).
 * Google's repo dropped separate static weight files a while back in favor of one variable font,
 * so each weight below is the same file with a different "wght" axis setting rather than a
 * separate Font() entry pointing at a different resource. Requires minSdk 26 (we're at 26+), which
 * is when Android added variable-font axis support. FontVariation is still an experimental Compose
 * API, hence the opt-in below — it's stable enough in practice, just not API-frozen yet.
 */
@OptIn(ExperimentalTextApi::class)
private fun interWeight(weight: Int, fontWeight: FontWeight) = Font(
    resId = R.font.inter_variable,
    weight = fontWeight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val Inter = FontFamily(
    interWeight(400, FontWeight.Normal),
    interWeight(500, FontWeight.Medium),
    interWeight(600, FontWeight.SemiBold),
    interWeight(700, FontWeight.Bold),
)

val StructTypography = Typography(
    displayLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.3).sp),
    headlineLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.2).sp),
    headlineMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.2.sp),
)
