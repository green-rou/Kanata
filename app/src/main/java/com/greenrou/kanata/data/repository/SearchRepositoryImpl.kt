package com.greenrou.kanata.data.repository

import android.util.Log
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
import java.util.concurrent.ConcurrentHashMap

class SearchRepositoryImpl(
    private val parserRegistry: ParserRegistry,
    private val settingsManager: SettingsManager,
    private val chapterParserRegistry: ChapterParserRegistry,
) : SearchRepository {

    private val deadHosts: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private companion object {
        const val TAG = "SearchRepo"
    }

    override fun searchAll(titles: List<String>): Flow<VideoSource> = channelFlow {
        val showAdult = settingsManager.showAdultContent.first()
        val disabled = settingsManager.disabledSources.first()
        parserRegistry.parsers.value
            .filter { !it.isAdultOnly || showAdult }
            .filter { it.label !in disabled }
            .map { parser ->
                async(Dispatchers.IO) {
                    if (deadHosts.any { parser.supports(it) }) {
                        Log.d(TAG, "[${parser.label}] ⤳ skipped (dead host)")
                        return@async
                    }
                    var found = false
                    var hitDeadHost = false
                    for (title in titles) {
                        var deadThisRound = false
                        parser.search(title).fold(
                            onSuccess = { url ->
                                Log.d(TAG, "[${parser.label}] ✓ '$title' → $url")
                                send(VideoSource(parser.label, url, parser.sourceType))
                                found = true
                            },
                            onFailure = { e ->
                                val msg = e.message ?: ""
                                if (e is java.net.UnknownHostException || "Unable to resolve host" in msg) {
                                    val host = msg.substringAfter('"').substringBefore('"').takeIf { it.isNotBlank() }
                                    host?.let { deadHosts.add(it) }
                                    Log.w(TAG, "[${parser.label}] ✗ dead host: ${host ?: msg}")
                                    deadThisRound = true
                                    hitDeadHost = true
                                } else {
                                    Log.w(TAG, "[${parser.label}] ✗ '$title': $msg")
                                }
                            },
                        )
                        if (found || deadThisRound) break
                    }
                    if (!found && !hitDeadHost) Log.w(TAG, "[${parser.label}] no result for any title")
                }
            }.forEach { it.await() }
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
