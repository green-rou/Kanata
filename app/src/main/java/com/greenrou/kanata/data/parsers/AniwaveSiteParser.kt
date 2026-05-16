package com.greenrou.kanata.data.parsers

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.VideoSourceType
import com.greenrou.kanata.domain.parser.SiteParser
import org.jsoup.Jsoup
import java.net.URLEncoder

class AniwaveSiteParser : SiteParser {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    override val label = "Aniwave"
    override val sourceType = VideoSourceType.ANIWAVE

    override fun supports(host: String) = "aniwave" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://aniwave.dk/?s=$encodedQuery"
        val document = Jsoup.connect(url).userAgent(userAgent).get()
        document.select(".listupd a").firstOrNull()?.attr("abs:href")
            ?: error("No results found on Aniwave for query: $query")
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> {
        val document = Jsoup.connect(pageUrl).userAgent(userAgent).get()
        val rawSlug = pageUrl.trimEnd('/').substringAfterLast("/")
        val slug = rawSlug.replace(Regex("-episode-\\d+.*$"), "")

        val episodes = document.select("a[href*=${slug}-episode-]")
            .map { a ->
                val href = a.attr("abs:href")
                val epPart = href.trimEnd('/').substringAfterLast("-episode-")
                Episode("Episode $epPart", href)
            }
            .distinctBy { it.url }
            .reversed()

        if (episodes.isNotEmpty()) return episodes

        val title = document.selectFirst("h1.entry-title, h1")?.text() ?: "Watch"
        return listOf(Episode(title, pageUrl))
    }
}
