package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.mod.ChapterParserRegistry
import com.greenrou.kanata.domain.model.ContentChapter
import com.greenrou.kanata.domain.repository.ChapterListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class ChapterListRepositoryImpl(
    private val registry: ChapterParserRegistry,
) : ChapterListRepository {

    override suspend fun getChapters(pageUrl: String): Result<List<ContentChapter>> = withContext(Dispatchers.IO) {
        runCatching {
            val host = URL(pageUrl).host
            val parser = registry.parsers.value.find { it.supports(host) }
                ?: error("No chapter parser supports: $host")
            parser.getChapters(pageUrl)
        }
    }
}
