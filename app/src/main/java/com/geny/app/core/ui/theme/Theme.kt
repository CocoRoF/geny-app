package com.geny.app.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.geny.app.core.storage.SettingsDataStore

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Purple20,
    secondary = Teal80,
    onSecondary = Teal20,
    tertiary = Amber80,
    onTertiary = Amber20,
    background = DarkSurface,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = LightSurface,
    secondary = Teal40,
    onSecondary = LightSurface,
    tertiary = Amber40,
    onTertiary = LightSurface,
    background = LightSurface,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant
)

@Composable
fun GenyTheme(
    settingsDataStore: SettingsDataStore? = null,
    content: @Composable () -> Unit
) {
    val settings = settingsDataStore?.settingsFlow?.collectAsState(initial = null)?.value
    val themeMode = settings?.themeMode ?: "system"

    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GenyTypography,
        content = content
    )
}
