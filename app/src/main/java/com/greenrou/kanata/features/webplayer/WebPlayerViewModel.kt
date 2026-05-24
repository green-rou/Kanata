package com.greenrou.kanata.features.webplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.features.webplayer.model.WebPlayerEvent
import com.greenrou.kanata.features.webplayer.model.WebPlayerState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WebPlayerViewModel : ViewModel() {

    private val _state = MutableStateFlow(WebPlayerState())
    val state = _state.asStateFlow()

    private val _events = Channel<WebPlayerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastDetectedUrl: String? = null

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
