package com.example.perutours.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PeruGold80,
    onPrimary = Color(0xFF451A03),
    primaryContainer = PeruGoldContainerDark,
    onPrimaryContainer = Color(0xFFFEF3C7),

    secondary = PeruTerracotta80,
    onSecondary = Color(0xFF431407),
    secondaryContainer = PeruTerracottaContainerDark,
    onSecondaryContainer = Color(0xFFFFEDD5),

    tertiary = PeruEmerald80,
    onTertiary = Color(0xFF022C22),

    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = PeruGold40,
    onPrimary = Color.White,
    primaryContainer = PeruGoldContainerLight,
    onPrimaryContainer = OnPeruGoldContainerLight,

    secondary = PeruTerracotta40,
    onSecondary = Color.White,
    secondaryContainer = PeruTerracottaContainerLight,

    tertiary = PeruEmerald40,
    onTertiary = Color.White,

    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

@Composable
fun PeruToursTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Para mantener la identidad de marca de la agencia turística,
    // se recomienda dejar dynamicColor en false (evita que Android 12+ sobreescriba con el fondo de pantalla del usuario)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Asegúrate de vincular tu Typography existente
        content = content
    )
}