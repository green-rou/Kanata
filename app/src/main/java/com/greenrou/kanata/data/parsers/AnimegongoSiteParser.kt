package com.greenrou.kanata.data.parsers

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.Translation
import com.greenrou.kanata.domain.model.VideoSourceType
import com.greenrou.kanata.domain.parser.SiteParser
import org.jsoup.Jsoup
import java.net.URLEncoder

class AnimegongoSiteParser : SiteParser {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    private val animeUrlPattern = Regex("""animego\.ngo/\d+-[^/?#]+\.html$""")

    override val label = "AnimeGO"
    override val sourceType = VideoSourceType.ANIMEGO_NGO

    override fun supports(host: String) = "animego.ngo" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val document = Jsoup.connect("https://animego.ngo/index.php?do=search&subaction=search&story=$encoded")
            .userAgent(userAgent)
            .referrer("https://animego.ngo/")
            .get()

        document.select("a[href]")
            .map { it.attr("abs:href") }
            .filter { animeUrlPattern.containsMatchIn(it) }
            .distinct()
            .firstOrNull() ?: error("No results on AnimeGO for: $query")
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> {
        val document = Jsoup.connect(pageUrl).userAgent(userAgent).get()

        val episodeLinks = document.select("a[href*='/episode-']")
            .map { it.attr("abs:href") }
            .filter { "animego.ngo" in it }
            .distinct()

        if (episodeLinks.isEmpty()) {
            return listOf(Episode("Дивитись", pageUrl))
        }

        return episodeLinks
            .map { url ->
                val epNum = Regex("""/episode-(\d+)\.html""").find(url)?.groupValues?.get(1) ?: "1"
                val seasonNum = Regex("""/season-(\d+)/""").find(url)?.groupValues?.get(1)
                val title = if (seasonNum != null && seasonNum != "1") "S${seasonNum}E$epNum" else "E$epNum"
                Episode(title, url)
            }
            .sortedWith(compareBy(
                { Regex("""/season-(\d+)/""").find(it.url)?.groupValues?.get(1)?.toIntOrNull() ?: 1 },
                { Regex("""/episode-(\d+)""").find(it.url)?.groupValues?.get(1)?.toIntOrNull() ?: 0 },
            ))
    }

    fun getTranslations(episodePageUrl: String): Result<List<Translation>> = runCatching {
        val doc = Jsoup.connect(episodePageUrl)
            .userAgent(userAgent)
            .header("Referer", "https://animego.ngo/")
            .get()

        val kodikUrl = doc.selectFirst("a#kodik-tab[data-url]")
            ?.attr("data-url")
            ?.let { if (it.startsWith("//")) "https:$it" else it }
            ?: error("No Kodik tab found on: $episodePageUrl")

        val kodikDoc = Jsoup.connect(kodikUrl)
            .userAgent(userAgent)
            .header("Referer", episodePageUrl)
            .get()

        kodikDoc.select(".movie-translations-box select option")
            .map { opt ->
                Translation(
                    id = opt.attr("data-id"),
                    title = opt.attr("data-title").ifBlank { opt.text() },
                    type = opt.attr("data-translation-type"),
                    mediaId = opt.attr("data-media-id"),
                    mediaHash = opt.attr("data-media-hash"),
                    mediaType = opt.attr("data-media-type").ifBlank { "seria" },
                )
            }
            .filter { it.mediaId.isNotBlank() && it.mediaHash.isNotBlank() }
            .ifEmpty { error("No translations found for: $episodePageUrl") }
    }
}
