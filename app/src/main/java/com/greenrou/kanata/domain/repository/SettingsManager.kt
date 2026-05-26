package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.VideoSourceType
import kotlinx.coroutines.flow.Flow

interface SettingsManager {
    val showAdultContent: Flow<Boolean>
    val isDarkTheme: Flow<Boolean>
    val coverFillsTopBar: Flow<Boolean>
    val downloadFolder: Flow<String>
    val accentColor: Flow<String>
    val disabledSources: Flow<Set<VideoSourceType>>
    val adBlockerEnabled: Flow<Boolean>
    val webBackNavTopBar: Flow<Boolean>
    val analyticsEnabled: Flow<Boolean>
    val analyticsConsentShown: Flow<Boolean>
    val skippedVersion: Flow<String>
    val activeInfoProviderId: Flow<String?>

    suspend fun setShowAdultContent(show: Boolean)
    suspend fun setDarkTheme(isDark: Boolean)
    suspend fun setCoverFillsTopBar(enabled: Boolean)
    suspend fun setDownloadFolder(path: String)
    suspend fun setAccentColor(name: String)
    suspend fun setDisabledSources(sources: Set<VideoSourceType>)
    suspend fun setAdBlockerEnabled(enabled: Boolean)
    suspend fun setWebBackNavTopBar(enabled: Boolean)
    suspend fun setAnalyticsEnabled(enabled: Boolean)
    suspend fun setAnalyticsConsentShown(shown: Boolean)
    suspend fun setSkippedVersion(version: String)
    suspend fun setActiveInfoProviderId(id: String?)
}
