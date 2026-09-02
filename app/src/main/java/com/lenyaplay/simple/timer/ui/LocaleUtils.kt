package com.lenyaplay.simple.timer.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.core.content.edit
import com.lenyaplay.simple.timer.trace
import java.util.Locale

private const val PREFS_NAME = "locale_prefs"
private const val LANGUAGE_KEY = "language"

fun Context.storedLanguage(): String? =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(LANGUAGE_KEY, null)

fun Context.setStoredLanguage(language: String) {
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(LANGUAGE_KEY, language)
    }
}

fun Context.withAppLocale(): Context {
    val language = storedLanguage()
    trace("Язык") { "attachBaseContext, сохранённый язык: $language" }
    if (language == null) return this
    val configuration = resources.configuration
    configuration.setLocale(Locale(language))
    return createConfigurationContext(configuration)
}

fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}
