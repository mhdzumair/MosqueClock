package com.mosque.prayerclock.ui.components

import com.mosque.prayerclock.data.model.Language

/**
 * Get font scale factor based on language
 * Tamil has longer text content, so it gets normal (1.0) font size
 * English and Sinhala have shorter text, so they get larger fonts (1.15)
 *
 * Note: This should be called with the effective language (never MULTI)
 * MULTI is resolved to the actual current language in MainActivity
 */
fun getLanguageFontScale(language: Language): Float =
    when (language) {
        Language.TAMIL -> 1.0f // Tamil: normal size
        Language.SINHALA -> 1.1f // Sinhala: 10% larger
        Language.ENGLISH -> 1.2f // English: 20% larger
        Language.MULTI -> 1.0f // Fallback (should never happen with effective language)
    }
