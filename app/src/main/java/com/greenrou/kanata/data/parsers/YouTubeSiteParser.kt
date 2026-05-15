package com.greenrou.kanata.data.parsers

import android.util.Log
import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.parser.SiteParser
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class YouTubeSiteParser : SiteParser {

    private val service = ServiceList.YouTube

    override val label = "YouTube"

    override fun supports(host: String) = "youtube" in host || "youtu.be" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val searchQuery = "$query аніме українська"
        Log.d(TAG, "search: query=\"$searchQuery\"")

        val info = SearchInfo.getInfo(
            service,
            service.searchQHFactory.fromQuery(
                searchQuery,
                listOf(YoutubeSearchQueryHandlerFactory.PLAYLISTS),
                null
            )
        )

        Log.d(TAG, "search: total results=${info.relatedItems.size}")
        info.relatedItems.take(5).forEachIndexed { i, item ->
            Log.d(TAG, "  result[$i]: ${item.javaClass.simpleName} name='${item.name}' url='${item.url}'")
        }

        val playlist = info.relatedItems
            .filterIsInstance<PlaylistInfoItem>()
            .firstOrNull() ?: error("No playlists found for: $query")

        Log.d(TAG, "search: selected playlist '${playlist.name}' → ${playlist.url}")
        playlist.url
    }.also { it.onFailure { e -> Log.e(TAG, "search: failed for \"$query\"", e) } }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> {
        val listUrl = normalizePlaylistUrl(pageUrl)
        Log.d(TAG, "getEpisodes: fetching playlist $listUrl")

        val info = PlaylistInfo.getInfo(service, listUrl)
        Log.d(TAG, "getEpisodes: playlist '${info.name}' streamCount=${info.streamCount}")

        val items = info.relatedItems.filterIsInstance<StreamInfoItem>()
        Log.d(TAG, "getEpisodes: found ${items.size} stream items")
        items.take(3).forEachIndexed { i, item ->
            Log.d(TAG, "  item[$i]: '${item.name}' url='${item.url}'")
        }

        if (items.isEmpty()) return listOf(Episode("Watch", pageUrl))

        return items.mapIndexed { index, item ->
            Episode("Серія ${index + 1}: ${item.name}", item.url)
        }
    }

    private fun normalizePlaylistUrl(url: String): String {
        val listMatch = Regex("""[?&]list=([A-Za-z0-9_-]+)""").find(url)
        val listId = listMatch?.groupValues?.get(1)
        return if (listId != null) "https://www.youtube.com/playlist?list=$listId" else url
    }

    companion object {
        private const val TAG = "YouTubeParser"
    }
}
