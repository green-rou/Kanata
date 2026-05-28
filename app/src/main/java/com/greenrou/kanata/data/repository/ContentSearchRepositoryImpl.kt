package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.mod.ChapterParserRegistry
import com.greenrou.kanata.domain.model.ContentSource
import com.greenrou.kanata.domain.repository.ContentSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentSearchRepositoryImpl(
    private val registry: ChapterParserRegistry,
) : ContentSearchRepository {

    override suspend fun searchAll(titles: List<String>): List<ContentSource> = withContext(Dispatchers.IO) {
        val sources = mutableListOf<ContentSource>()
        registry.parsers.value.forEach { parser ->
            for (title in titles) {
                val result = parser.search(title)
                if (result.isSuccess) {
                    result.onSuccess { url -> sources.add(ContentSource(parser.label, url)) }
                    break
                }
            }
        }
        sources
    }
}
