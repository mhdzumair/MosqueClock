package com.mosque.prayerclock.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mosque.prayerclock.R
import com.mosque.prayerclock.data.model.Language

/**
 * The app's bundled type system.
 *
 * Each family below is a single variable font file referenced multiple times at different
 * [FontWeight]s — Android instances the requested weight from the font's own weight axis, so one
 * ~300-700KB file covers the whole range instead of shipping a static file per weight.
 *
 * Fonts are Google-hosted, OFL-licensed:
 * - Archivo — geometric display/numeral face used for clocks, headings and countdowns.
 * - Noto Sans Tamil / Noto Sans Sinhala — localized body text for those languages.
 * - Noto Naskh Arabic — the Quranic dua text on the Dua screen.
 */
object AppFonts {
    val Display = FontFamily(
        Font(R.font.archivo_variable, FontWeight.Normal),
        Font(R.font.archivo_variable, FontWeight.Medium),
        Font(R.font.archivo_variable, FontWeight.SemiBold),
        Font(R.font.archivo_variable, FontWeight.Bold),
        Font(R.font.archivo_variable, FontWeight.ExtraBold),
    )

    val Tamil = FontFamily(
        Font(R.font.noto_sans_tamil_variable, FontWeight.Normal),
        Font(R.font.noto_sans_tamil_variable, FontWeight.Medium),
        Font(R.font.noto_sans_tamil_variable, FontWeight.SemiBold),
        Font(R.font.noto_sans_tamil_variable, FontWeight.Bold),
    )

    val Sinhala = FontFamily(
        Font(R.font.noto_sans_sinhala_variable, FontWeight.Normal),
        Font(R.font.noto_sans_sinhala_variable, FontWeight.Medium),
        Font(R.font.noto_sans_sinhala_variable, FontWeight.SemiBold),
        Font(R.font.noto_sans_sinhala_variable, FontWeight.Bold),
    )

    val Arabic = FontFamily(
        Font(R.font.noto_naskh_arabic_variable, FontWeight.Normal),
        Font(R.font.noto_naskh_arabic_variable, FontWeight.Bold),
    )

    /** The body/UI font for whichever language is active — Archivo already covers English fine. */
    fun bodyFor(language: Language): FontFamily = when (language) {
        Language.TAMIL -> Tamil
        Language.SINHALA -> Sinhala
        else -> Display
    }
}
