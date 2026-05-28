package com.greenrou.kanata.features.chapters.model

import com.greenrou.kanata.domain.model.ContentChapter

data class ChapterListState(
    val isLoading: Boolean = false,
    val chapters: List<ContentChapter> = emptyList(),
    val error: String? = null,
)
