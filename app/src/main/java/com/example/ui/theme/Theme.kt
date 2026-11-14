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
    primary = WhatsAppDarkAccent,
    onPrimary = Color.Black,
    primaryContainer = WhatsAppTealDark,
    onPrimaryContainer = Color.White,
    secondary = WhatsAppGreen,
    onSecondary = Color.Black,
    background = WhatsAppDarkBackground,
    onBackground = WhatsAppDarkTextPrimary,
    surface = WhatsAppDarkSurface,
    onSurface = WhatsAppDarkTextPrimary,
    surfaceVariant = WhatsAppDarkCard,
    onSurfaceVariant = WhatsAppDarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppTealDark,
    onPrimary = Color.White,
    primaryContainer = WhatsAppTeal,
    onPrimaryContainer = Color.White,
    secondary = WhatsAppGreen,
    onSecondary = Color.White,
    background = WhatsAppBackground,
    onBackground = WhatsAppTextPrimary,
    surface = WhatsAppSurface,
    onSurface = WhatsAppTextPrimary,
    surfaceVariant = Color(0xFFF7F8FA),
    onSurfaceVariant = WhatsAppTextSecondary
)

@Composable
fun LokalPostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep brand WhatsApp colors by default
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LokalPostTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
