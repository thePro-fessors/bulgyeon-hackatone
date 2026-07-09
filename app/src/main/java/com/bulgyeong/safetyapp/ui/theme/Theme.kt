package com.bulgyeong.safetyapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = FigmaBlack,
    secondary = FigmaYellow,
    background = FigmaWhite,
    surface = FigmaWhite,
    onPrimary = FigmaWhite,
    onSecondary = FigmaBlack,
    onBackground = FigmaBlack,
    onSurface = FigmaBlack,
    error = AlertRed
)

@Composable
fun BulgyeongSafetyAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = FigmaYellow.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
