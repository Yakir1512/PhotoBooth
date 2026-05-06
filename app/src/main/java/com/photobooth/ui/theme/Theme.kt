package com.photobooth.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.photobooth.data.model.AppTheme

// ─────────────────────────────────────────────────────────────────────────────
// Color Palettes
// ─────────────────────────────────────────────────────────────────────────────

object PhotoBoothColors {
    // DARK GOLD – Elegant event theme
    val DarkGoldBackground   = Color(0xFF0A0A0A)
    val DarkGoldSurface      = Color(0xFF1A1A1A)
    val DarkGoldPrimary      = Color(0xFFD4AF37)   // Rich gold
    val DarkGoldSecondary    = Color(0xFFB8860B)
    val DarkGoldAccent       = Color(0xFFFFF8DC)   // Cream white
    val DarkGoldOnPrimary    = Color(0xFF0A0A0A)
    val DarkGoldOnBackground = Color(0xFFF5F5F5)

    // NEON PARTY
    val NeonBackground       = Color(0xFF050510)
    val NeonSurface          = Color(0xFF0D0D25)
    val NeonPrimary          = Color(0xFF00FFFF)   // Cyan
    val NeonSecondary        = Color(0xFFFF00FF)   // Magenta
    val NeonAccent           = Color(0xFF00FF88)   // Green
    val NeonOnPrimary        = Color(0xFF050510)
    val NeonOnBackground     = Color(0xFFFFFFFF)

    // MINIMAL WHITE
    val MinimalBackground    = Color(0xFFFAFAFA)
    val MinimalSurface       = Color(0xFFFFFFFF)
    val MinimalPrimary       = Color(0xFF1A1A1A)
    val MinimalSecondary     = Color(0xFF555555)
    val MinimalAccent        = Color(0xFFE0E0E0)
    val MinimalOnPrimary     = Color(0xFFFFFFFF)
    val MinimalOnBackground  = Color(0xFF1A1A1A)

    // VINTAGE RETRO
    val VintageBackground    = Color(0xFF2C1810)
    val VintageSurface       = Color(0xFF3D2318)
    val VintagePrimary       = Color(0xFFE8C547)   // Warm yellow
    val VintageSecondary     = Color(0xFFC4A44A)
    val VintageAccent        = Color(0xFFF2E8D0)   // Parchment
    val VintageOnPrimary     = Color(0xFF1A0F08)
    val VintageOnBackground  = Color(0xFFF2E8D0)
}

// ─────────────────────────────────────────────────────────────────────────────
// Color Schemes
// ─────────────────────────────────────────────────────────────────────────────

fun darkGoldColorScheme() = darkColorScheme(
    background       = PhotoBoothColors.DarkGoldBackground,
    surface          = PhotoBoothColors.DarkGoldSurface,
    primary          = PhotoBoothColors.DarkGoldPrimary,
    secondary        = PhotoBoothColors.DarkGoldSecondary,
    onPrimary        = PhotoBoothColors.DarkGoldOnPrimary,
    onBackground     = PhotoBoothColors.DarkGoldOnBackground,
    onSurface        = PhotoBoothColors.DarkGoldOnBackground,
    tertiary         = PhotoBoothColors.DarkGoldAccent,
)

fun neonPartyColorScheme() = darkColorScheme(
    background       = PhotoBoothColors.NeonBackground,
    surface          = PhotoBoothColors.NeonSurface,
    primary          = PhotoBoothColors.NeonPrimary,
    secondary        = PhotoBoothColors.NeonSecondary,
    onPrimary        = PhotoBoothColors.NeonOnPrimary,
    onBackground     = PhotoBoothColors.NeonOnBackground,
    onSurface        = PhotoBoothColors.NeonOnBackground,
    tertiary         = PhotoBoothColors.NeonAccent,
)

fun minimalWhiteColorScheme() = lightColorScheme(
    background       = PhotoBoothColors.MinimalBackground,
    surface          = PhotoBoothColors.MinimalSurface,
    primary          = PhotoBoothColors.MinimalPrimary,
    secondary        = PhotoBoothColors.MinimalSecondary,
    onPrimary        = PhotoBoothColors.MinimalOnPrimary,
    onBackground     = PhotoBoothColors.MinimalOnBackground,
    onSurface        = PhotoBoothColors.MinimalOnBackground,
    tertiary         = PhotoBoothColors.MinimalAccent,
)

fun vintageRetroColorScheme() = darkColorScheme(
    background       = PhotoBoothColors.VintageBackground,
    surface          = PhotoBoothColors.VintageSurface,
    primary          = PhotoBoothColors.VintagePrimary,
    secondary        = PhotoBoothColors.VintageSecondary,
    onPrimary        = PhotoBoothColors.VintageOnPrimary,
    onBackground     = PhotoBoothColors.VintageOnBackground,
    onSurface        = PhotoBoothColors.VintageOnBackground,
    tertiary         = PhotoBoothColors.VintageAccent,
)

// ─────────────────────────────────────────────────────────────────────────────
// Typography
// ─────────────────────────────────────────────────────────────────────────────

val PhotoBoothTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize   = 64.sp,
        letterSpacing = (-2).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 48.sp,
        letterSpacing = (-1).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 28.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 18.sp,
        lineHeight  = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 14.sp,
        letterSpacing = 1.sp,
    ),
)

// ─────────────────────────────────────────────────────────────────────────────
// Theme entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhotoBoothTheme(
    appTheme: AppTheme = AppTheme.DARK_GOLD,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (appTheme) {
        AppTheme.DARK_GOLD    -> darkGoldColorScheme()
        AppTheme.NEON_PARTY   -> neonPartyColorScheme()
        AppTheme.MINIMAL_WHITE -> minimalWhiteColorScheme()
        AppTheme.VINTAGE_RETRO -> vintageRetroColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = PhotoBoothTypography,
        content     = content,
    )
}
