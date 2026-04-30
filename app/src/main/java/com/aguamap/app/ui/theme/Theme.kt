package com.aguamap.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlue,
    secondary = OceanBlue,
    tertiary = IceBlue,
    background = DeepOcean,
    surface = DeepOcean,
    onPrimary = HighContrastWhite,
    onSecondary = HighContrastWhite,
    onBackground = IceBlue,
    onSurface = IceBlue
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    secondary = SkyBlue,
    tertiary = DeepOcean,
    background = IceBlue,
    surface = HighContrastWhite,
    onPrimary = HighContrastWhite,
    onSecondary = HighContrastWhite,
    onBackground = OceanBlue,
    onSurface = OceanBlue
)

private val HighContrastColorScheme = lightColorScheme(
    primary = HighContrastBlack,
    onPrimary = HighContrastWhite,
    primaryContainer = HighContrastYellow,
    onPrimaryContainer = HighContrastBlack,
    background = HighContrastWhite,
    onBackground = HighContrastBlack,
    surface = HighContrastWhite,
    onSurface = HighContrastBlack,
    secondary = HighContrastBlack,
    onSecondary = HighContrastWhite
)

@Composable
fun AguaMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    dynamicColor: Boolean = false, // Desactivado por defecto para mantener estética Ocean & Cloud
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast -> HighContrastColorScheme
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
