package com.pavanpej.kompass.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Kompass design system — the single source of truth for app colors.
 * Background is true black (#000000) for per-pixel-off AMOLED display.
 */
object KompassColors {
    val TrueBlack = Color(0xFF000000)
    val Surface = Color(0xFF000000)
    /** A touch above true black, for small chrome (pills, banners) that needs to read as a distinct layer. */
    val SurfaceElevated = Color(0xFF1C1C1E)

    val Accent = Color(0xFF00E5D1)
    val PrincetonOrange = Color(0xFFFF8600)

    val OnSurface = Color(0xFFECECEE)
    val OnSurfaceFaint = OnSurface.copy(alpha = 0.35f)
    val OnSurfaceMuted = OnSurface.copy(alpha = 0.6f)
}
