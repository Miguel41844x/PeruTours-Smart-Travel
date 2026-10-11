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
    weight: FontWeight = FontWeight.Normal
) = TextStyle(
    fontFamily = PeruToursFont,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    displaySmall = peruTextStyle(36, 44, FontWeight.Bold),
    headlineLarge = peruTextStyle(30, 38, FontWeight.Bold),
    headlineMedium = peruTextStyle(26, 34, FontWeight.Bold),
    headlineSmall = peruTextStyle(22, 28, FontWeight.SemiBold),
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
