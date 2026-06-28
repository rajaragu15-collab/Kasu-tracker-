package com.nanba.financetracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nanba.financetracker.data.AppTheme

private val LightColors = lightColorScheme(
    primary = GreenPrimaryLight,
    secondary = AmberAccent,
    error = ErrorRed,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    secondary = AmberAccent,
    error = ErrorRed,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface
)

@Composable
fun KasuTrackerTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> systemDark
    }

    val colorScheme = if (useDarkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            val window = (context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                        !useDarkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
