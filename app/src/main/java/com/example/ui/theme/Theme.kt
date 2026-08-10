package com.example.ui.theme

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
    primary = GoldAccent,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF334155),
    onPrimaryContainer = Color.White,
    secondary = GoldLight,
    onSecondary = Color.Black,
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceVariantDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = SurfaceBorderDark,
    error = RoseError
)

private val LightColorScheme = lightColorScheme(
    primary = Amber700,
    onPrimary = Color.White,
    primaryContainer = Amber100,
    onPrimaryContainer = SurfaceDark,
    secondary = Amber500,
    onSecondary = Color.White,
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceVariantLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = Color(0xFFCBD5E1),
    error = RoseError
)

@Composable
fun LiquorTheme(
    darkTheme: Boolean = true, // Default to rich amber dark mode for premium liquor dashboard look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    LiquorTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
