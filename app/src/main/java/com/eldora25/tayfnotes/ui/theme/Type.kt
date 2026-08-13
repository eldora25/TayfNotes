package com.eldora25.tayfnotes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.R

// Local Premium Font Families
val Alkatra = FontFamily(
    Font(R.font.alkatra_regular, FontWeight.Normal),
    Font(R.font.alkatra_medium, FontWeight.Medium),
    Font(R.font.alkatra_bold, FontWeight.Bold)
)

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

val NotoSerif = FontFamily(
    Font(R.font.notoserif_regular, FontWeight.Normal),
    Font(R.font.notoserif_medium, FontWeight.Medium),
    Font(R.font.notoserif_semibold, FontWeight.SemiBold),
    Font(R.font.notoserif_bold, FontWeight.Bold)
)

val DynaPuff = FontFamily(
    Font(R.font.dynapuff_regular, FontWeight.Normal),
    Font(R.font.dynapuff_medium, FontWeight.Medium),
    Font(R.font.dynapuff_semibold, FontWeight.SemiBold),
    Font(R.font.dynapuff_bold, FontWeight.Bold)
)

val ComicRelief = FontFamily(
    Font(R.font.comicrelief_regular, FontWeight.Normal),
    Font(R.font.comicrelief_bold, FontWeight.Bold)
)

val AlfaSlabOne = FontFamily(Font(R.font.alfaslabone_regular, FontWeight.Normal))
val Ballet = FontFamily(Font(R.font.ballet_regular_variablefont_opsz, FontWeight.Normal))
val Borel = FontFamily(Font(R.font.borel_regular, FontWeight.Normal))
val Ruthie = FontFamily(Font(R.font.ruthie_regular, FontWeight.Normal))
val Sekuya = FontFamily(Font(R.font.sekuya_regular, FontWeight.Normal))
val Sriracha = FontFamily(Font(R.font.sriracha_regular, FontWeight.Normal))
val Romanesco = FontFamily(Font(R.font.romanesco_regular, FontWeight.Normal))
val FleurDeLeah = FontFamily(Font(R.font.fleurdeleah_regular, FontWeight.Normal))
val KaushanScript = FontFamily(Font(R.font.kaushanscript_regular, FontWeight.Normal))
val LavishlyYours = FontFamily(Font(R.font.lavishlyyours_regular, FontWeight.Normal))
val PlaywriteDK = FontFamily(Font(R.font.playwritedkuloopet_regular, FontWeight.Normal))
val PlaywriteDE = FontFamily(Font(R.font.playwritedesasguides_regular, FontWeight.Normal))
val Quicksand = FontFamily(Font(R.font.quicksand_variablefont_wght, FontWeight.Normal))
val Honk = FontFamily(Font(R.font.honk_regular_variablefont_morfshln, FontWeight.Normal))

// Comprehensive Font Map for UI Selection
val TayfFonts = mapOf(
    "Default" to FontFamily.Default,
    "Alkatra" to Alkatra,
    "Poppins" to Poppins,
    "Noto Serif" to NotoSerif,
    "DynaPuff" to DynaPuff,
    "Comic Relief" to ComicRelief,
    "Alfa Slab" to AlfaSlabOne,
    "Ballet" to Ballet,
    "Borel" to Borel,
    "Ruthie" to Ruthie,
    "Sekuya" to Sekuya,
    "Sriracha" to Sriracha,
    "Romanesco" to Romanesco,
    "Fleur De Leah" to FleurDeLeah,
    "Kaushan" to KaushanScript,
    "Lavishly" to LavishlyYours,
    "Playwrite DK" to PlaywriteDK,
    "Playwrite DE" to PlaywriteDE,
    "Quicksand" to Quicksand,
    "Honk" to Honk
)

val InterFont = Poppins // Using Poppins as default "modern" font

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
