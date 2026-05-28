package com.greenrou.kanata.data.mod

import com.greenrou.kanata.domain.model.ContentChapter
import com.greenrou.kanata.domain.model.ContentPage
import com.greenrou.kanata.domain.parser.ChapterParser
import com.greenrou.kanata.modapi.ModChapterParser

class ChapterParserAdapter(private val mod: ModChapterParser) : ChapterParser {
    override val label: String = mod.label
    override fun supports(host: String) = mod.supports(host)
    override suspend fun search(query: String) = mod.search(query)
    override suspend fun getChapters(pageUrl: String) =
        mod.getChapters(pageUrl).map { ContentChapter(it.title, it.url) }
    override suspend fun getPages(chapterUrl: String) =
        mod.getPages(chapterUrl).map { ContentPage(it.url, it.headers) }
}
