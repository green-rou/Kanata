package com.greenrou.kanata.data.repository

import com.greenrou.kanata.domain.model.VideoSource
import com.greenrou.kanata.domain.parser.SiteParser
import com.greenrou.kanata.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepositoryImpl(
    private val parsers: List<SiteParser>
) : SearchRepository {

    override suspend fun searchAll(query: String): List<VideoSource> = withContext(Dispatchers.IO) {
        val sources = mutableListOf<VideoSource>()
        parsers.forEach { parser ->
            parser.search(query).onSuccess { url ->
                sources.add(VideoSource(parser.label, url))
            }
        }
        sources
    }
}
