package com.greenrou.kanata.data.parsers

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.VideoSourceType
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
    override val sourceType = VideoSourceType.YOUTUBE

    override fun supports(host: String) = "youtube" in host || "youtu.be" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val searchQuery = "$query аніме українська"

        val info = SearchInfo.getInfo(
            service,
            service.searchQHFactory.fromQuery(
                searchQuery,
                listOf(YoutubeSearchQueryHandlerFactory.PLAYLISTS),
                null
            )
        )

        val playlist = info.relatedItems
            .filterIsInstance<PlaylistInfoItem>()
            .firstOrNull() ?: error("No playlists found for: $query")

        playlist.url
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> {
        val listUrl = normalizePlaylistUrl(pageUrl)

        val info = PlaylistInfo.getInfo(service, listUrl)

        val items = info.relatedItems.filterIsInstance<StreamInfoItem>()

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
}
