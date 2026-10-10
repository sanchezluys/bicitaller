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
    primary = BiciOrangeDark,
    onPrimary = Color.Black,
    primaryContainer = BiciOrangeContainerDark,
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = BiciSteelSecondaryDark,
    onSecondary = Color.Black,
    tertiary = GarantiaActive,
    background = WorkshopDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = WorkshopDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = WorkshopDarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = WorkshopDarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = BiciOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = BiciOrangeContainerLight,
    onPrimaryContainer = Color(0xFF431B00),
    secondary = BiciSteelSecondary,
    onSecondary = Color.White,
    tertiary = Color(0xFF0891B2),
    background = WorkshopLightBg,
    onBackground = Color(0xFF0F172A),
    surface = WorkshopLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = WorkshopLightBorder
)

@Composable
fun BiciTallerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve distinctive workshop theme branding
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
