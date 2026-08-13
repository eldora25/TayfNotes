package com.eldora25.tayfnotes.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class TayfTheme {
    MIDNIGHT, SUNSET, FOREST, OCEAN, LAVENDER, ROSE, SLATE, EMERALD, ROYAL, CRIMSON
}

fun Color.contentColor(): Color {
    return if (this.luminance() > 0.45f) Color.Black else Color.White
}

@Composable
fun EditorNeonIcon(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val yellowNeon = Color(0xFFFFD700)
    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = yellowNeon, spotColor = yellowNeon)
            .background(Color.Black, CircleShape)
            .border(1.5.dp, yellowNeon.copy(alpha = 0.8f), CircleShape)
            .padding(10.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides yellowNeon) {
            content()
        }
    }
}

private fun getThemePrimary(theme: TayfTheme): Color {
    return when(theme) {
        TayfTheme.MIDNIGHT -> MidnightPrimary
        TayfTheme.SUNSET -> SunsetPrimary
        TayfTheme.FOREST -> ForestPrimary
        TayfTheme.OCEAN -> OceanPrimary
        TayfTheme.LAVENDER -> LavenderPrimary
        TayfTheme.ROSE -> RosePrimary
        TayfTheme.SLATE -> SlatePrimary
        TayfTheme.EMERALD -> EmeraldPrimary
        TayfTheme.ROYAL -> RoyalPrimary
        TayfTheme.CRIMSON -> CrimsonPrimary
    }
}

private fun getDarkColorScheme(theme: TayfTheme): ColorScheme {
    val primary = getThemePrimary(theme)
    return darkColorScheme(
        primary = primary,
        onPrimary = Color.Black,
        primaryContainer = primary.copy(alpha = 0.35f),
        onPrimaryContainer = Color.White,
        secondary = primary.copy(alpha = 0.8f),
        onSecondary = Color.Black,
        tertiary = Color(0xFFFFD700), // Gold as tertiary for default visibility
        onTertiary = Color.Black,
        background = Color(0xFF0F0F0F), // Slightly lighter than absolute black
        onBackground = Color.White,
        surface = Color(0xFF161616),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF222222), // More contrast for surfaces
        onSurfaceVariant = Color(0xFFD1D1D1),
        outline = primary.copy(alpha = 0.5f)
    )
}

private fun getLightColorScheme(theme: TayfTheme): ColorScheme {
    val primary = getThemePrimary(theme)
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.15f),
        onPrimaryContainer = primary,
        secondary = primary.copy(alpha = 0.6f),
        onSecondary = Color.White,
        tertiary = Color(0xFFDAA520), // GoldenRod for light mode
        onTertiary = Color.White,
        background = Color.White,
        onBackground = Color(0xFF0A0A0A),
        surface = Color(0xFFF8F9FA),
        onSurface = Color(0xFF0A0A0A),
        surfaceVariant = Color(0xFFF0F2F5),
        onSurfaceVariant = Color(0xFF495057),
        outline = primary.copy(alpha = 0.3f)
    )
}

@Composable
fun TayfNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    currentTheme: TayfTheme = TayfTheme.MIDNIGHT,
    dynamicColor: Boolean = false,
    defaultFontFamily: String = "Default",
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getDarkColorScheme(currentTheme)
        else -> getLightColorScheme(currentTheme)
    }

    val selectedFont = TayfFonts[defaultFontFamily] ?: androidx.compose.ui.text.font.FontFamily.Default
    val customTypography = Typography.copy(
        headlineLarge = Typography.headlineLarge.copy(fontFamily = selectedFont),
        headlineMedium = Typography.headlineMedium.copy(fontFamily = selectedFont),
        titleLarge = Typography.titleLarge.copy(fontFamily = selectedFont),
        bodyLarge = Typography.bodyLarge.copy(fontFamily = selectedFont),
        bodyMedium = Typography.bodyMedium.copy(fontFamily = selectedFont),
        labelSmall = Typography.labelSmall.copy(fontFamily = selectedFont)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = customTypography,
        content = content
    )
}
