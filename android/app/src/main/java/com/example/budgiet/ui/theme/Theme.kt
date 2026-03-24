package com.example.budgiet.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private var darkColorScheme: ColorScheme? = null
val DarkColorScheme: ColorScheme
    @Composable get() {
        if (darkColorScheme != null) {
            return darkColorScheme!!
        }
        darkColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Dynamic color is available on Android 12+
            dynamicDarkColorScheme(LocalContext.current)
        } else {
            darkColorScheme(
                primary = Purple80,
                secondary = PurpleGrey80,
                tertiary = Pink80
            )
        }
        return darkColorScheme!!
    }

private var lightColorScheme: ColorScheme? = null
val LightColorScheme: ColorScheme
    @Composable get() {
        if (lightColorScheme != null) {
            return lightColorScheme!!
        }
        lightColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Dynamic color is available on Android 12+
            dynamicLightColorScheme(LocalContext.current)
        } else {
            lightColorScheme(
                primary = Purple40,
                secondary = PurpleGrey40,
                tertiary = Pink40

                /* Other default colors to override
                background = Color(0xFFFFFBFE),
                surface = Color(0xFFFFFBFE),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = Color(0xFF1C1B1F),
                onSurface = Color(0xFF1C1B1F),
                */
            )
        }
        return lightColorScheme!!
    }

@Composable
fun BudgietTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        },
        typography = Typography,
        content = content
    )
}