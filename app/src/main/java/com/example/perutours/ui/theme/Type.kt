package com.example.perutours.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val PeruToursFont = FontFamily.SansSerif

private fun peruTextStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    family: FontFamily = PeruToursFont
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    displayLarge = peruTextStyle(48, 56, FontWeight.Medium, FontFamily.Serif),
    displayMedium = peruTextStyle(40, 48, FontWeight.Medium, FontFamily.Serif),
    displaySmall = peruTextStyle(34, 42, FontWeight.Medium, FontFamily.Serif),
    headlineLarge = peruTextStyle(30, 38, FontWeight.Medium, FontFamily.Serif),
    headlineMedium = peruTextStyle(27, 35, FontWeight.Medium, FontFamily.Serif),
    headlineSmall = peruTextStyle(24, 32, FontWeight.Medium, FontFamily.Serif),
    titleLarge = peruTextStyle(20, 26, FontWeight.SemiBold),
    titleMedium = peruTextStyle(16, 22, FontWeight.SemiBold),
    titleSmall = peruTextStyle(14, 20, FontWeight.SemiBold),
    bodyLarge = peruTextStyle(16, 24),
    bodyMedium = peruTextStyle(14, 20),
    bodySmall = peruTextStyle(12, 18),
    labelLarge = peruTextStyle(14, 20, FontWeight.SemiBold),
    labelMedium = peruTextStyle(12, 16, FontWeight.Medium),
    labelSmall = peruTextStyle(11, 16, FontWeight.Medium)
)
