package com.greenrou.kanata.features.pagereader.model

sealed interface PageReaderEvent {
    data object BackClicked : PageReaderEvent
    data object ToggleBars : PageReaderEvent
    data object PrevChapter : PageReaderEvent
    data object NextChapter : PageReaderEvent
    data object RetryClicked : PageReaderEvent

    data class SaveProgress(val currentPageIndex: Int, val totalPages: Int) : PageReaderEvent

    data object NavigateBack : PageReaderEvent
}
