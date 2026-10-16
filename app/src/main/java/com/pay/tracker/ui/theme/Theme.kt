package com.pay.tracker.ui.theme

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

// Emerald Reserve & Deep Onyx - Light Theme
private val LightColorScheme = lightColorScheme(
     primary = EmeraldPrimaryLight,
     onPrimary = Color.White,
     primaryContainer = EmeraldLightPrimaryContainer,
     onPrimaryContainer = EmeraldLightOnPrimaryContainer,
     secondary = ChampagneGoldDark,
     onSecondary = Color.White,
     secondaryContainer = Color(0xFFF6EEDB),
     onSecondaryContainer = Color(0xFF322408),
     tertiary = TransferColor,
     onTertiary = Color.White,
     tertiaryContainer = Color(0xFFD0F6E6),
     onTertiaryContainer = Color(0xFF003822),
     background = PearlBackground,
     onBackground = PearlOnSurface,
     surface = PearlSurface,
     onSurface = PearlOnSurface,
     surfaceVariant = PearlSurfaceLow,
     onSurfaceVariant = PearlOnSurfaceVariant,
     surfaceContainer = PearlSurfaceContainer,
     surfaceContainerLow = PearlSurfaceLow,
     surfaceContainerHigh = PearlSurfaceContainerHigh,
     surfaceContainerHighest = PearlSurfaceContainerHighest,
     outline = PearlOutline,
     outlineVariant = PearlOutlineVariant,
     error = ExpenseRed,
     onError = Color.White,
     errorContainer = ExpenseRedContainer,
     onErrorContainer = ExpenseRedOnContainer
)

// Botanical Glass - Dark Theme
private val DarkColorScheme = darkColorScheme(
     primary = BotanicalEmerald,
     onPrimary = Color(0xFF062211),
     primaryContainer = Color(0xFF163823),
     onPrimaryContainer = Color(0xFFA5F4C5),
     secondary = BotanicalCreamPill,
     onSecondary = BotanicalCreamPillText,
     secondaryContainer = Color(0xFF263329),
     onSecondaryContainer = Color(0xFFE8E5D8),
     tertiary = TransferColor,
     onTertiary = Color(0xFF003822),
     tertiaryContainer = Color(0xFF0B3322),
     onTertiaryContainer = Color(0xFF8CF3C7),
     background = BotanicalObsidian,
     onBackground = BotanicalTextPrimary,
     surface = BotanicalGlassSurface,
     onSurface = BotanicalTextPrimary,
     surfaceVariant = BotanicalDarkSurface,
     onSurfaceVariant = BotanicalTextSecondary,
     surfaceContainer = BotanicalGlassSurface,
     surfaceContainerLow = BotanicalGlassSurfaceLow,
     surfaceContainerHigh = Color(0xE615261D),
     surfaceContainerHighest = Color(0xF21A2E23),
     outline = BotanicalGlassBorder,
     outlineVariant = BotanicalGlassBorderSubtle,
     error = ExpenseRed,
     onError = Color(0xFF690005),
     errorContainer = ExpenseRedContainer,
     onErrorContainer = ExpenseRedOnContainer
)

@Composable
fun PayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false by default to showcase Emerald Reserve & Deep Onyx
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
