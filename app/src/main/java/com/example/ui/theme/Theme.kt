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
    primary = ChessLensCyan,
    onPrimary = Slate950,
    primaryContainer = Slate800,
    onPrimaryContainer = ChessLensCyan,
    secondary = ChessLensTeal,
    onSecondary = Slate950,
    secondaryContainer = Slate800,
    onSecondaryContainer = ChessLensTeal,
    tertiary = ChessLensAmber,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    error = ChessLensRed,
    onError = Slate950,
    outline = Slate600
)

private val AmoledColorScheme = darkColorScheme(
    primary = ChessLensCyan,
    onPrimary = AmoledBlack,
    primaryContainer = AmoledCard,
    onPrimaryContainer = ChessLensCyan,
    secondary = ChessLensTeal,
    onSecondary = AmoledBlack,
    secondaryContainer = AmoledCard,
    onSecondaryContainer = ChessLensTeal,
    tertiary = ChessLensAmber,
    onTertiary = AmoledBlack,
    background = AmoledBlack,
    onBackground = Slate50,
    surface = AmoledBlack,
    onSurface = Slate50,
    surfaceVariant = AmoledCard,
    onSurfaceVariant = Slate200,
    error = ChessLensRed,
    onError = AmoledBlack,
    outline = Slate700
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF0F766E),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate200,
    onSurfaceVariant = Slate700,
    error = Color(0xFFDC2626),
    onError = Color.White,
    outline = Slate400
)

enum class ThemeMode {
    DARK,
    LIGHT,
    AMOLED,
    SYSTEM
}

@Composable
fun ChessLensTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemInDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeMode == ThemeMode.AMOLED -> AmoledColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ChessLensTheme(
        themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT,
        dynamicColor = dynamicColor,
        content = content
    )
}
