package com.zirakusa.gatewayhub

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** The app's own language (chosen on the first launch), applied to an Activity or Service context. */
object LocaleContext {
    fun wrap(base: Context, language: String?): Context {
        if (language == null) return base
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale); setLayoutDirection(locale) }
        return base.createConfigurationContext(config)
    }
}
