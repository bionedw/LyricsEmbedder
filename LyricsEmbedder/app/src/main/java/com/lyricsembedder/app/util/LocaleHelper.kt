package com.lyricsembedder.app.util

import android.content.Context
import android.content.res.Configuration
import com.lyricsembedder.app.data.model.AppLanguage
import java.util.Locale

/**
 * Wraps an Android Context with the selected app locale so switching between
 * System / English / Vietnamese / Japanese works on Android 5.0 (API 21) through Android 15.
 */
object LocaleHelper {
    fun createLocalizedContext(base: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return base
        val locale = when (language) {
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.VIETNAMESE -> Locale("vi")
            AppLanguage.JAPANESE -> Locale.JAPANESE
            AppLanguage.SYSTEM -> Locale.getDefault()
        }
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
