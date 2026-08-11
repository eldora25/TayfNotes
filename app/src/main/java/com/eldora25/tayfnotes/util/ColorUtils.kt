package com.eldora25.tayfnotes.util

import androidx.compose.ui.graphics.Color

/**
 * Utility to parse Hex color strings safely
 */
fun parseNoteColor(colorHex: String?): Color {
    return try {
        if (!colorHex.isNullOrEmpty()) {
            Color(android.graphics.Color.parseColor(colorHex))
        } else {
            Color.Unspecified
        }
    } catch (e: Exception) {
        Color.Unspecified
    }
}
