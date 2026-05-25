package com.greenrou.kanata.features.webplayer.model

data class WebPlayerState(
    val addressBarText: String = "",
    val isPageLoading: Boolean = false,
    val detectedStreamUrl: String? = null,
    val detectedReferer: String? = null,
    val urlToLoad: String? = null,
    val hasNavigated: Boolean = false,
    val adBlockerEnabled: Boolean = true,
    val webBackNavTopBar: Boolean = false,
    val showSaveDialog: Boolean = false,
)
