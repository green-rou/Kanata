package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.mod.ChapterParserRegistry
import com.greenrou.kanata.data.mod.ParserRegistry
import com.greenrou.kanata.domain.model.OnlineSearchGroup
import com.greenrou.kanata.domain.model.OnlineSearchResult
import com.greenrou.kanata.domain.model.VideoSource
import com.greenrou.kanata.domain.repository.SearchRepository
import com.greenrou.kanata.domain.repository.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SearchRepositoryImpl(
    private val parserRegistry: ParserRegistry,
    private val settingsManager: SettingsManager,
    private val chapterParserRegistry: ChapterParserRegistry,
) : SearchRepository {

    override suspend fun searchAll(titles: List<String>): List<VideoSource> = withContext(Dispatchers.IO) {
        val showAdult = settingsManager.showAdultContent.first()
        val disabled = settingsManager.disabledSources.first()
        val sources = mutableListOf<VideoSource>()
        parserRegistry.parsers.value
            .filter { parser -> !parser.isAdultOnly || showAdult }
            .filter { parser -> parser.label !in disabled }
            .forEach { parser ->
                for (title in titles) {
                    val result = parser.search(title)
                    if (result.isSuccess) {
                        result.onSuccess { url -> sources.add(VideoSource(parser.label, url, parser.sourceType)) }
                        break
                    }
                }
            }
        sources
    }

    override fun searchOnline(query: String, isMangaMode: Boolean): Flow<List<OnlineSearchGroup>> = channelFlow {
        val disabled = settingsManager.disabledSources.first()

        if (isMangaMode) {
            val parsers = chapterParserRegistry.parsers.value
                .filter { it.label !in disabled }

            if (parsers.isEmpty()) {
                send(emptyList())
                return@channelFlow
            }

            val groups = Array(parsers.size) { i ->
                OnlineSearchGroup(parsers[i].label, isLoading = true, results = emptyList(), error = false)
            }
            send(groups.toList())

            val mutex = Mutex()
            parsers.mapIndexed { index, parser ->
                async(Dispatchers.IO) {
                    val result = parser.search(query).mapCatching { url ->
                        listOf(OnlineSearchResult(
                            sourceLabel = parser.label,
                            title = query,
                            pageUrl = url,
                            coverUrl = null,
                        ))
                    }
                    mutex.withLock {
                        groups[index] = result.fold(
                            onSuccess = { results -> groups[index].copy(isLoading = false, results = results) },
                            onFailure = { groups[index].copy(isLoading = false, error = true) },
                        )
                        send(groups.toList())
                    }
                }
            }.forEach { it.await() }
        } else {
            val showAdult = settingsManager.showAdultContent.first()
            val parsers = parserRegistry.parsers.value
                .filter { !it.isAdultOnly || showAdult }
                .filter { it.label !in disabled }

            if (parsers.isEmpty()) {
                send(emptyList())
                return@channelFlow
            }

            val groups = Array(parsers.size) { i ->
                OnlineSearchGroup(parsers[i].label, isLoading = true, results = emptyList(), error = false)
            }
            send(groups.toList())

            val mutex = Mutex()
            parsers.mapIndexed { index, parser ->
                async(Dispatchers.IO) {
                    val result = parser.searchWithResults(query)
                    mutex.withLock {
                        groups[index] = result.fold(
                            onSuccess = { results -> groups[index].copy(isLoading = false, results = results) },
                            onFailure = { groups[index].copy(isLoading = false, error = true) },
                        )
                        send(groups.toList())
                    }
                }
            }.forEach { it.await() }
        }
    }
}
