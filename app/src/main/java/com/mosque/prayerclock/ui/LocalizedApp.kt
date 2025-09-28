package com.mosque.prayerclock.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.mosque.prayerclock.data.model.Language
import com.mosque.prayerclock.utils.LocaleManager

val LocalLocalizedContext = compositionLocalOf<Context> { error("No localized context provided") }

@Composable
fun LocalizedApp(
    language: Language,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current

    // Create localized context - keep it stable to avoid full app recomposition
    val localizedContext =
        remember(language) {
            LocaleManager.setLocale(baseContext, language)
        }

    CompositionLocalProvider(
        LocalLocalizedContext provides localizedContext,
    ) {
        content()
    }
}
