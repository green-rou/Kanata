package com.greenrou.kanata.core.util

import android.content.Context

object LanguagePrefs {
    private const val PREFS_NAME = "kanata_prefs"
    private const val KEY_LANGUAGE = "app_language"
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val UKRAINIAN = "uk"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SYSTEM) ?: SYSTEM

    fun set(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, language).apply()
    }
}
