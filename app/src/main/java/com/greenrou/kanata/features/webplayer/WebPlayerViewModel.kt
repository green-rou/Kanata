package com.greenrou.kanata.features.webplayer

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.usecase.SavePageUseCase
import com.greenrou.kanata.features.webplayer.model.WebPlayerEvent
import com.greenrou.kanata.features.webplayer.model.WebPlayerState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WebPlayerViewModel(
    private val settingsManager: SettingsManager,
    private val savePage: SavePageUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(WebPlayerState())
    val state = _state.asStateFlow()

    private val _events = Channel<WebPlayerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastDetectedUrl: String? = null
    private var savedWebViewState: Bundle? = null

    fun saveWebViewState(bundle: Bundle) {
        savedWebViewState = bundle
    }

    fun consumeWebViewState(): Bundle? = savedWebViewState?.also { savedWebViewState = null }

    init {
        settingsManager.adBlockerEnabled
            .onEach { enabled -> _state.update { it.copy(adBlockerEnabled = enabled) } }
            .launchIn(viewModelScope)
        settingsManager.webBackNavTopBar
            .onEach { enabled -> _state.update { it.copy(webBackNavTopBar = enabled) } }
            .launchIn(viewModelScope)
    }

    fun handleEvent(event: WebPlayerEvent) {
        when (event) {
            is WebPlayerEvent.AddressBarChanged ->
                _state.update { it.copy(addressBarText = event.text) }

            is WebPlayerEvent.UrlSubmitted -> {
                val url = normalizeUrl(event.url)
                _state.update { it.copy(addressBarText = url, urlToLoad = url, hasNavigated = true) }
            }

            is WebPlayerEvent.PageNavigated -> {
                lastDetectedUrl = null
                _state.update {
                    it.copy(
                        addressBarText = event.url,
                        detectedStreamUrl = null,
                        detectedReferer = null,
                    )
                }
            }

            is WebPlayerEvent.LoadingChanged ->
                _state.update { it.copy(isPageLoading = event.isLoading) }

            is WebPlayerEvent.StreamDetected -> {
                if (event.streamUrl == lastDetectedUrl) return
                lastDetectedUrl = event.streamUrl
                _state.update {
                    it.copy(
                        detectedStreamUrl = event.streamUrl,
                        detectedReferer = event.referer,
                    )
                }
            }

            WebPlayerEvent.OpenInPlayer -> viewModelScope.launch {
                val url = _state.value.detectedStreamUrl ?: return@launch
                val referer = _state.value.detectedReferer.orEmpty()
                _state.update { it.copy(detectedStreamUrl = null, detectedReferer = null) }
                _events.send(WebPlayerEvent.NavigateToPlayer(url, referer))
            }

            WebPlayerEvent.DismissStream -> {
                lastDetectedUrl = null
                _state.update { it.copy(detectedStreamUrl = null, detectedReferer = null) }
            }

            WebPlayerEvent.UrlLoadDispatched ->
                _state.update { it.copy(urlToLoad = null) }

            WebPlayerEvent.DisableAdBlocker -> viewModelScope.launch {
                settingsManager.setAdBlockerEnabled(false)
            }

            WebPlayerEvent.ShowSaveDialog ->
                _state.update { it.copy(showSaveDialog = true) }

            WebPlayerEvent.DismissSaveDialog ->
                _state.update { it.copy(showSaveDialog = false) }

            is WebPlayerEvent.SavePage -> viewModelScope.launch {
                savePage(event.name, event.url)
                _state.update { it.copy(showSaveDialog = false) }
            }

            WebPlayerEvent.NavigateBack -> viewModelScope.launch {
                _events.send(WebPlayerEvent.NavigateBack)
            }

            is WebPlayerEvent.NavigateToPlayer -> Unit
        }
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
        else "https://$trimmed"
    }
}
