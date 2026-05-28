package com.greenrou.kanata.domain.parser

import com.greenrou.kanata.domain.model.ContentChapter
import com.greenrou.kanata.domain.model.ContentPage

interface ChapterParser {
    val label: String
    fun supports(host: String): Boolean
    suspend fun search(query: String): Result<String>
    suspend fun getChapters(pageUrl: String): List<ContentChapter>
    suspend fun getPages(chapterUrl: String): List<ContentPage>
}
