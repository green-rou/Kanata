package com.greenrou.kanata.features.pagereader.model

import com.greenrou.kanata.domain.model.ContentPage

data class PageReaderState(
    val isLoading: Boolean = false,
    val pages: List<ContentPage> = emptyList(),
    val error: String? = null,
    val currentChapterIndex: Int = 0,
    val chapterUrls: List<String> = emptyList(),
    val chapterTitles: List<String> = emptyList(),
    val areBarsVisible: Boolean = true,
) {
    val currentChapterTitle: String
        get() = chapterTitles.getOrElse(currentChapterIndex) { "" }

    val hasPrevChapter: Boolean
        get() = currentChapterIndex > 0

    val hasNextChapter: Boolean
        get() = currentChapterIndex < chapterUrls.lastIndex
}
