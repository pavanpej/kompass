package com.pavanpej.kompass.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KompassColorScheme = darkColorScheme(
    primary = KompassColors.Accent,
    secondary = KompassColors.Accent,
    tertiary = KompassColors.PrincetonOrange,
    background = KompassColors.TrueBlack,
    surface = KompassColors.Surface,
    onBackground = KompassColors.OnSurface,
    onSurface = KompassColors.OnSurface
)

@Composable
fun KompassTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KompassColorScheme,
        typography = Typography,
        content = content
    )
}
