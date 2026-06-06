package com.greenrou.kanata.data.repository

import android.util.Log
import com.greenrou.kanata.data.mod.ChapterParserRegistry
import com.greenrou.kanata.domain.model.ContentSource
import com.greenrou.kanata.domain.repository.ContentSearchRepository
import com.greenrou.kanata.domain.repository.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

class ContentSearchRepositoryImpl(
    private val registry: ChapterParserRegistry,
    private val settingsManager: SettingsManager,
) : ContentSearchRepository {

    private val deadHosts: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override fun searchAll(titles: List<String>): Flow<ContentSource> = channelFlow {
        val disabled = settingsManager.disabledSources.first()
        registry.parsers.value
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
                            send(ContentSource(parser.label, url))
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

    private companion object {
        const val TAG = "ContentSearchRepo"
    }
}
