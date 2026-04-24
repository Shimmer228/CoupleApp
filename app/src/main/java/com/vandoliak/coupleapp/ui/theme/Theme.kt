package com.vandoliak.coupleapp.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue,
    secondary = CalmTeal,
    tertiary = WarmOrange,
    background = DarkSurface,
    surface = DarkCard,
    surfaceVariant = DarkCard,
    onPrimary = SurfaceLight,
    onSecondary = SurfaceLight,
    onTertiary = SlateText,
    onBackground = SurfaceLight,
    onSurface = SurfaceLight
)

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    secondary = CalmTeal,
    tertiary = WarmOrange,
    background = SurfaceLight,
    surface = SurfaceCard,
    surfaceVariant = CloudBlue,
    onPrimary = SurfaceCard,
    onSecondary = SurfaceCard,
    onTertiary = SlateText,
    onBackground = SlateText,
    onSurface = SlateText,
    onSurfaceVariant = MutedText,
    outline = DividerLight
)

@Composable
fun CoupleAppTheme(
    darkTheme: Boolean = false,
    // Dynamic color is available on Android 12+
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
        content = content
    )
}
