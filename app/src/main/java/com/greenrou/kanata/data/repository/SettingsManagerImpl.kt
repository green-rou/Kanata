package com.greenrou.kanata.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val DOWNLOAD_FOLDER = stringPreferencesKey("download_folder")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val DISABLED_SOURCES = stringPreferencesKey("disabled_sources")
        val AD_BLOCKER_ENABLED = booleanPreferencesKey("ad_blocker_enabled")
        val WEB_BACK_NAV_TOPBAR = booleanPreferencesKey("web_back_nav_topbar")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val ANALYTICS_CONSENT_SHOWN = booleanPreferencesKey("analytics_consent_shown")
        val SKIPPED_VERSION = stringPreferencesKey("skipped_version")
        val ACTIVE_INFO_PROVIDER_ID = stringPreferencesKey("active_info_provider_id")
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

    override val downloadFolder: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DOWNLOAD_FOLDER] ?: ""
        }

    override val accentColor: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] ?: "Gray"
        }

    override val disabledSources: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DISABLED_SOURCES]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet()
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

    override suspend fun setDownloadFolder(path: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DOWNLOAD_FOLDER] = path
        }
    }

    override suspend fun setAccentColor(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] = name
        }
    }

    override suspend fun setDisabledSources(sources: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISABLED_SOURCES] = sources.joinToString(",")
        }
    }

    override val adBlockerEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AD_BLOCKER_ENABLED] ?: true
        }

    override suspend fun setAdBlockerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AD_BLOCKER_ENABLED] = enabled
        }
    }

    override val webBackNavTopBar: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WEB_BACK_NAV_TOPBAR] ?: false
        }

    override suspend fun setWebBackNavTopBar(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEB_BACK_NAV_TOPBAR] = enabled
        }
    }

    override val analyticsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.ANALYTICS_ENABLED] ?: true }

    override suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANALYTICS_ENABLED] = enabled
        }
    }

    override val analyticsConsentShown: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.ANALYTICS_CONSENT_SHOWN] ?: false }

    override suspend fun setAnalyticsConsentShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANALYTICS_CONSENT_SHOWN] = shown
        }
    }

    override val skippedVersion: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SKIPPED_VERSION] ?: ""
        }

    override suspend fun setSkippedVersion(version: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SKIPPED_VERSION] = version
        }
    }

    override val activeInfoProviderId: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.ACTIVE_INFO_PROVIDER_ID] }

    override suspend fun setActiveInfoProviderId(id: String?) {
        context.dataStore.edit { preferences ->
            if (id == null) preferences.remove(PreferencesKeys.ACTIVE_INFO_PROVIDER_ID)
            else preferences[PreferencesKeys.ACTIVE_INFO_PROVIDER_ID] = id
        }
    }
}
