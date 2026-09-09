package com.lenyaplay.simple.timer.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import com.lenyaplay.simple.timer.trace

enum class AppLanguage(val tag: String) {
    English("en"),
    Russian("ru");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.find { it.tag == tag } ?: English
    }
}

fun currentAppLanguage(context: Context): AppLanguage {
    val locales = AppCompatDelegate.getApplicationLocales()
    val language = if (locales.isEmpty) {
        ConfigurationCompat.getLocales(context.resources.configuration)[0]?.language
    } else {
        locales[0]?.language
    }
    trace("Language") { "current app language: $language" }
    return AppLanguage.fromTag(language)
}

fun setAppLanguage(language: AppLanguage) {
    trace("Language") { "switching language to: ${language.tag}" }
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
}
