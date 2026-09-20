package com.sakethh.linkora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.sakethh.linkora.shared.generated.resources.Res
import com.sakethh.linkora.shared.generated.resources.googleSansFlex
import com.sakethh.linkora.shared.generated.resources.medium
import com.sakethh.linkora.shared.generated.resources.momo_signature
import com.sakethh.linkora.shared.generated.resources.regular
import com.sakethh.linkora.shared.generated.resources.semibold
import com.sakethh.linkora.ui.domain.Font
import org.jetbrains.compose.resources.Font

 val googleSansFlexFontFamily
    @Composable
    get() =
        FontFamily(
            Font(
                Res.font.googleSansFlex,
                weight = FontWeight.Normal,
                variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(460),
                    FontVariation.width(102f),
                ),
            ),
            Font(
                Res.font.googleSansFlex,
                weight = FontWeight.Medium,
                variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(560),
                    FontVariation.width(102f),
                ),
            ),
            Font(
                Res.font.googleSansFlex,
                weight = FontWeight.SemiBold,
                variationSettings =
                FontVariation.Settings(
                    FontVariation.weight(660),
                    FontVariation.width(102f),
                ),
            ),
        )

private val playWriteFontFamily
    @Composable
    get() =
        FontFamily(
            Font(
                resource = Res.font.momo_signature,
                weight = FontWeight.Normal,
            ),
        )

private val poppinsFontFamily
    @Composable
    get() =
        FontFamily(
            Font(resource = Res.font.regular, weight = FontWeight.Normal),
            Font(resource = Res.font.medium, weight = FontWeight.Medium),
            Font(resource = Res.font.semibold, weight = FontWeight.SemiBold),
        )

@Suppress("ComposableNaming")
@Composable
fun LinkoraTypography(preferredFont: Font): Typography {
    val currentFontFamily =
        if (preferredFont == Font.GOOGLE_SANS_FLEX) googleSansFlexFontFamily else poppinsFontFamily
    return Typography(
        titleLarge = TextStyle(fontFamily = currentFontFamily, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontFamily = currentFontFamily, fontWeight = FontWeight.Medium),
        titleSmall = TextStyle(fontFamily = currentFontFamily, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontFamily = currentFontFamily, fontWeight = FontWeight.Normal),
        labelSmall = TextStyle(fontFamily = playWriteFontFamily, fontWeight = FontWeight.Normal),
    )
}
