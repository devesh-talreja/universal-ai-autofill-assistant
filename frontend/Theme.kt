package com.example.smartautofiller.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary            = Color(0xFF7F77DD),
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFEEEDFE),
    onPrimaryContainer = Color(0xFF3C3489),
    secondary          = Color(0xFF378ADD),
    onSecondary        = Color.White,
    background         = Color(0xFFF3F2FF),
    onBackground       = Color(0xFF1A1A2E),
    surface            = Color(0xFFFFFFFF),
    onSurface          = Color(0xFF1A1A2E),
    surfaceVariant     = Color(0xFFEEEDFE),
    onSurfaceVariant   = Color(0xFF534AB7),
    error              = Color(0xFFE24B4A),
    onError            = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFFAFA9EC),
    onPrimary          = Color(0xFF26215C),
    primaryContainer   = Color(0xFF3C3489),
    onPrimaryContainer = Color(0xFFCECBF6),
    secondary          = Color(0xFF85B7EB),
    onSecondary        = Color(0xFF042C53),
    background         = Color(0xFF121212),
    onBackground       = Color(0xFFE8E6FF),
    surface            = Color(0xFF1E1E2E),
    onSurface          = Color(0xFFE8E6FF),
    surfaceVariant     = Color(0xFF2A2845),
    onSurfaceVariant   = Color(0xFFAFA9EC),
    error              = Color(0xFFFF6B6B),
    onError            = Color(0xFF500000),
)

@Composable
fun SmartautofillerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}