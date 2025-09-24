package com.mosque.prayerclock.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mosque.prayerclock.data.model.Language
import java.util.Locale

object LocaleManager {
    fun setLocale(
        context: Context,
        language: Language,
    ): Context =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, language.code)
        } else {
            updateResourcesLegacy(context, language.code)
        }

    fun applyLanguage(language: Language) {
        val locale = Locale(language.code)
        val localeList = LocaleListCompat.create(locale)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    private fun updateResources(
        context: Context,
        languageCode: String,
    ): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLocales(android.os.LocaleList(locale))

        return context.createConfigurationContext(configuration)
    }

    private fun updateResourcesLegacy(
        context: Context,
        languageCode: String,
    ): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.locale = locale

        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        return context
    }

    fun getCurrentLocale(context: Context): Locale =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            context.resources.configuration.locale
        }
}
