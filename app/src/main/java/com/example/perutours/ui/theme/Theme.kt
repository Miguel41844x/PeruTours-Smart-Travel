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
    onPrimary = Color(0xFF56110C),
    primaryContainer = PeruGoldContainerDark,
    onPrimaryContainer = Color(0xFFFFDAD5),

    secondary = PeruTerracotta80,
    onSecondary = Color(0xFF482900),
    secondaryContainer = PeruTerracottaContainerDark,
    onSecondaryContainer = Color(0xFFFFDEA4),

    tertiary = PeruEmerald80,
    onTertiary = Color(0xFF00382E),
    tertiaryContainer = PeruEmeraldContainerDark,
    onTertiaryContainer = Color(0xFFA8F2DD),

    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = PeruBrick,
    onPrimary = Color.White,
    primaryContainer = PeruBrickContainer,
    onPrimaryContainer = OnPeruBrickContainer,

    secondary = PeruMaizeDark,
    onSecondary = Color.White,
    secondaryContainer = PeruMaizeContainer,
    onSecondaryContainer = OnPeruMaizeContainer,

    tertiary = PeruValley,
    onTertiary = Color.White,
    tertiaryContainer = PeruValleyContainer,
    onTertiaryContainer = OnPeruValleyContainer,

    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
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
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
