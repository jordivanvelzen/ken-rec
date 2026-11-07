package com.kendamasenses.kenrec.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kendamasenses.kenrec.R

/** The Kendama Senses look, taken from the storefront's dark theme. */
object Senses {
    val BgDeep = Color(0xFF0D1E23)
    val Surface = Color(0xFF16292C)
    val Ink = Color(0xFFF7F3E4)
    val InkMute = Color(0x7AF7F3E4)
    val Border = Color(0x29F7F3E4)
    val Mint = Color(0xFF9CDCC3)
    val MintInk = Color(0xFF0B1B1C)
    val Coral = Color(0xFFDF5553)
}

val Poppins = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
)

private fun TextStyle.poppins() = copy(fontFamily = Poppins)

@Composable
fun SensesTheme(content: @Composable () -> Unit) {
    val base = Typography()
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Senses.Mint,
            onPrimary = Senses.MintInk,
            background = Senses.BgDeep,
            onBackground = Senses.Ink,
            surface = Senses.Surface,
            onSurface = Senses.Ink,
            error = Senses.Coral,
        ),
        typography = Typography(
            displayLarge = base.displayLarge.poppins(),
            displayMedium = base.displayMedium.poppins(),
            displaySmall = base.displaySmall.poppins(),
            headlineLarge = base.headlineLarge.poppins(),
            headlineMedium = base.headlineMedium.poppins(),
            headlineSmall = base.headlineSmall.poppins(),
            titleLarge = base.titleLarge.poppins(),
            titleMedium = base.titleMedium.poppins(),
            titleSmall = base.titleSmall.poppins(),
            bodyLarge = base.bodyLarge.poppins(),
            bodyMedium = base.bodyMedium.poppins(),
            bodySmall = base.bodySmall.poppins(),
            labelLarge = base.labelLarge.poppins(),
            labelMedium = base.labelMedium.poppins(),
            labelSmall = base.labelSmall.poppins(),
        ),
        content = content,
    )
}
