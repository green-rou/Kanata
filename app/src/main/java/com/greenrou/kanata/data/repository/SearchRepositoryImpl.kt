package com.greenrou.kanata.data.repository

import com.greenrou.kanata.domain.model.VideoSource
import com.greenrou.kanata.domain.parser.SiteParser
import com.greenrou.kanata.domain.repository.SearchRepository
import com.greenrou.kanata.domain.repository.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SearchRepositoryImpl(
    private val parsers: List<SiteParser>,
    private val settingsManager: SettingsManager,
) : SearchRepository {

    override suspend fun searchAll(query: String): List<VideoSource> = withContext(Dispatchers.IO) {
        val showAdult = settingsManager.showAdultContent.first()
        val sources = mutableListOf<VideoSource>()
        parsers
            .filter { parser -> !parser.isAdultOnly || showAdult }
            .forEach { parser ->
                parser.search(query).onSuccess { url ->
                    sources.add(VideoSource(parser.label, url, parser.sourceType))
                }
            }
        sources
    }
}
