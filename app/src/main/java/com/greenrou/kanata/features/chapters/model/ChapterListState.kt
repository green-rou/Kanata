package com.greenrou.kanata.features.chapters.model

import com.greenrou.kanata.domain.model.ContentChapter
import com.greenrou.kanata.domain.model.DownloadItem

data class ChapterListState(
    val isLoading: Boolean = false,
    val chapters: List<ContentChapter> = emptyList(),
    val error: String? = null,
    val retryAttempt: Int = 0,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
    val downloadStatuses: Map<String, DownloadItem> = emptyMap(),
)
