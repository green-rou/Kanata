package com.greenrou.kanata.data.parsers

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.VideoSourceType
import com.greenrou.kanata.domain.parser.SiteParser
import org.jsoup.Jsoup
import java.net.URLEncoder

class HentaiHubSiteParser : SiteParser {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    override val label = "HentaiHub"
    override val sourceType = VideoSourceType.HENTAI_HUB
    override val isAdultOnly = true

    override fun supports(host: String) = "hentai-hub" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        for (searchUrl in listOf(
            "https://v4.hentai-hub.net/index.php?do=search&subaction=search&search_start=0&full_search=0&story=$encoded",
            "https://v4.hentai-hub.net/?s=$encoded",
            "https://v4.hentai-hub.net/search/$encoded",
        )) {
            val document = runCatching { Jsoup.connect(searchUrl).userAgent(userAgent).get() }.getOrNull() ?: continue
            val candidates = document.select("a[href]")
                .map { it.attr("abs:href") }
                .filter { Regex("hentai-hub\\.net/\\d+[^/]*\\.html").containsMatchIn(it) }
                .distinct()
            if (candidates.isNotEmpty()) return@runCatching candidates.first()
        }
        error("No results on HentaiHub for: $query")
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> {
        val document = Jsoup.connect(pageUrl).userAgent(userAgent).get()
        val rawSlug = pageUrl.trimEnd('/').substringAfterLast("/")
        val slug = rawSlug.replace(Regex("-episode-\\d+.*$"), "")

        val episodes = document.select("a[href*=${slug}-episode-]")
            .map { a ->
                val href = a.attr("abs:href")
                val epNum = href.trimEnd('/').substringAfterLast("-episode-")
                Episode("Episode $epNum", href)
            }
            .distinctBy { it.url }
            .reversed()

        if (episodes.isNotEmpty()) return episodes

        val title = document.selectFirst("h1.entry-title, h1.ts-title, h1")?.text() ?: "Watch"
        return listOf(Episode(title, pageUrl))
    }
}
