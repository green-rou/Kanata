package com.greenrou.kanata.features.chapters.model

sealed interface ChapterListEvent {
    data object BackClicked : ChapterListEvent
    data class ChapterClicked(val index: Int) : ChapterListEvent
    data class DownloadChapter(val chapterUrl: String, val chapterTitle: String) : ChapterListEvent

    data object NavigateBack : ChapterListEvent
    data class NavigateToReader(
        val chapterUrls: List<String>,
        val chapterTitles: List<String>,
        val startIndex: Int,
    ) : ChapterListEvent
}
