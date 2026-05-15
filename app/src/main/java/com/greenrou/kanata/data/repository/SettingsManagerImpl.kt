package com.greenrou.kanata.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.greenrou.kanata.domain.repository.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManagerImpl(private val context: Context) : SettingsManager {

    private object PreferencesKeys {
        val SHOW_ADULT_CONTENT = booleanPreferencesKey("show_adult_content")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val COVER_FILLS_TOP_BAR = booleanPreferencesKey("cover_fills_top_bar")
    }

    override val showAdultContent: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_ADULT_CONTENT] ?: false
        }

    override val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] ?: false
        }

    override val coverFillsTopBar: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.COVER_FILLS_TOP_BAR] ?: true
        }

    override suspend fun setShowAdultContent(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ADULT_CONTENT] = show
        }
    }

    override suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_THEME] = isDark
        }
    }

    override suspend fun setCoverFillsTopBar(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COVER_FILLS_TOP_BAR] = enabled
        }
    }
}
