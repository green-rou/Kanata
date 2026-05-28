package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.mod.ChapterParserRegistry
import com.greenrou.kanata.domain.model.ContentPage
import com.greenrou.kanata.domain.repository.ContentPagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class ContentPagesRepositoryImpl(
    private val registry: ChapterParserRegistry,
) : ContentPagesRepository {

    override suspend fun getPages(chapterUrl: String): Result<List<ContentPage>> = withContext(Dispatchers.IO) {
        runCatching {
            val host = URL(chapterUrl).host
            val parser = registry.parsers.value.find { it.supports(host) }
                ?: error("No chapter parser supports: $host")
            parser.getPages(chapterUrl)
        }
    }
}
