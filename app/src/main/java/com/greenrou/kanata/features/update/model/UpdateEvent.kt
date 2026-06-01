package com.greenrou.kanata.features.update.model

sealed interface UpdateEvent {
    data object CheckUpdate : UpdateEvent
    data object CheckUpdateSilent : UpdateEvent
    data object SkipUpdate : UpdateEvent
    data object DismissDialog : UpdateEvent
    data object StartDownload : UpdateEvent
    data object ConsumeNoUpdatesMessage : UpdateEvent
}
