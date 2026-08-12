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
        primaryContainer = primary.copy(alpha = 0.3f),
        onPrimaryContainer = Color.White,
        secondary = primary.copy(alpha = 0.7f),
        onSecondary = Color.Black,
        background = Color(0xFF0A0A0A),
        onBackground = Color.White,
        surface = Color(0xFF121212),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1E1E1E),
        onSurfaceVariant = Color(0xFFB0B0B0),
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
        background = Color.White,
        onBackground = Color(0xFF0A0A0A),
        surface = Color(0xFFF8F9FA),
        onSurface = Color(0xFF0A0A0A),
        surfaceVariant = Color(0xFFE9ECEF),
        onSurfaceVariant = Color(0xFF495057),
        outline = primary.copy(alpha = 0.3f)
    )
}

@Composable
fun TayfNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    currentTheme: TayfTheme = TayfTheme.MIDNIGHT,
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
