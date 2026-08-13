package com.sage502.budgettracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Gray20,
    onPrimary = Gray99,
    primaryContainer = Gray90,
    onPrimaryContainer = Gray10,
    secondary = Gray40,
    onSecondary = Gray99,
    secondaryContainer = Gray95,
    onSecondaryContainer = Gray10,
    tertiary = Gray30,
    onTertiary = Gray99,
    tertiaryContainer = Gray90,
    onTertiaryContainer = Gray10,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkGray90,
    onPrimary = DarkGray10,
    primaryContainer = DarkGray30,
    onPrimaryContainer = DarkGray90,
    secondary = DarkGray80,
    onSecondary = DarkGray10,
    secondaryContainer = DarkGray20,
    onSecondaryContainer = DarkGray90,
    tertiary = DarkGray80,
    onTertiary = DarkGray10,
    tertiaryContainer = DarkGray30,
    onTertiaryContainer = DarkGray90,
)

@Composable
fun BudgetTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
