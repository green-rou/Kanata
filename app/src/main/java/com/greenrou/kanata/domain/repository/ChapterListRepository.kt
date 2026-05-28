package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.ContentChapter

interface ChapterListRepository {
    suspend fun getChapters(pageUrl: String): Result<List<ContentChapter>>
}
