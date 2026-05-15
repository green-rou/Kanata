package com.greenrou.kanata.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsManager {
    val showAdultContent: Flow<Boolean>
    val isDarkTheme: Flow<Boolean>
    val coverFillsTopBar: Flow<Boolean>

    suspend fun setShowAdultContent(show: Boolean)
    suspend fun setDarkTheme(isDark: Boolean)
    suspend fun setCoverFillsTopBar(enabled: Boolean)
}
