package com.lenyaplay.simple.timer.ui

import androidx.appcompat.app.AppCompatDelegate
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

fun currentAppLanguage(): AppLanguage {
    val locales = AppCompatDelegate.getApplicationLocales()
    val language = if (locales.isEmpty) null else locales[0]?.language
    trace("Язык") { "текущий язык приложения: $language" }
    return AppLanguage.fromTag(language)
}

fun setAppLanguage(language: AppLanguage) {
    trace("Язык") { "переключение языка на: ${language.tag}" }
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
}
