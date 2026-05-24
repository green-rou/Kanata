package com.greenrou.kanata.features.webplayer.model

sealed interface WebPlayerEvent {
    data class AddressBarChanged(val text: String) : WebPlayerEvent
    data class UrlSubmitted(val url: String) : WebPlayerEvent
    data class PageNavigated(val url: String) : WebPlayerEvent
    data class LoadingChanged(val isLoading: Boolean) : WebPlayerEvent
    data class StreamDetected(val streamUrl: String, val referer: String) : WebPlayerEvent
    data object OpenInPlayer : WebPlayerEvent
    data object DismissStream : WebPlayerEvent
    data object UrlLoadDispatched : WebPlayerEvent
    data object NavigateBack : WebPlayerEvent
    // navigation-only, sent via Channel
    data class NavigateToPlayer(val streamUrl: String, val referer: String) : WebPlayerEvent
}
