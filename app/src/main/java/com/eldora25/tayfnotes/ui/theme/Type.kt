package com.eldora25.tayfnotes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.R

val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Premium Font Altyapısı - 20 Font
val TayfFonts = mapOf(
    "Default" to FontFamily.Default,
    "Inter" to FontFamily(Font(googleFont = GoogleFont("Inter"), fontProvider = fontProvider)),
    "Roboto" to FontFamily(Font(googleFont = GoogleFont("Roboto"), fontProvider = fontProvider)),
    "Playfair" to FontFamily(Font(googleFont = GoogleFont("Playfair Display"), fontProvider = fontProvider)),
    "Montserrat" to FontFamily(Font(googleFont = GoogleFont("Montserrat"), fontProvider = fontProvider)),
    "Lora" to FontFamily(Font(googleFont = GoogleFont("Lora"), fontProvider = fontProvider)),
    "Merriweather" to FontFamily(Font(googleFont = GoogleFont("Merriweather"), fontProvider = fontProvider)),
    "Oswald" to FontFamily(Font(googleFont = GoogleFont("Oswald"), fontProvider = fontProvider)),
    "Poppins" to FontFamily(Font(googleFont = GoogleFont("Poppins"), fontProvider = fontProvider)),
    "Dancing Script" to FontFamily(Font(googleFont = GoogleFont("Dancing Script"), fontProvider = fontProvider)),
    "Pacifico" to FontFamily(Font(googleFont = GoogleFont("Pacifico"), fontProvider = fontProvider)),
    "Caveat" to FontFamily(Font(googleFont = GoogleFont("Caveat"), fontProvider = fontProvider)),
    "Abril Fatface" to FontFamily(Font(googleFont = GoogleFont("Abril Fatface"), fontProvider = fontProvider)),
    "Lobster" to FontFamily(Font(googleFont = GoogleFont("Lobster"), fontProvider = fontProvider)),
    "Ubuntu" to FontFamily(Font(googleFont = GoogleFont("Ubuntu"), fontProvider = fontProvider)),
    "Quicksand" to FontFamily(Font(googleFont = GoogleFont("Quicksand"), fontProvider = fontProvider)),
    "Titillium Web" to FontFamily(Font(googleFont = GoogleFont("Titillium Web"), fontProvider = fontProvider)),
    "Comfortaa" to FontFamily(Font(googleFont = GoogleFont("Comfortaa"), fontProvider = fontProvider)),
    "Zilla Slab" to FontFamily(Font(googleFont = GoogleFont("Zilla Slab"), fontProvider = fontProvider)),
    "Cinzel" to FontFamily(Font(googleFont = GoogleFont("Cinzel"), fontProvider = fontProvider))
)

val InterFont = TayfFonts["Inter"] ?: FontFamily.Default

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
