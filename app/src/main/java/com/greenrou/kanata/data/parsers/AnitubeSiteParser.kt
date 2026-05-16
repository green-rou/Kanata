package com.greenrou.kanata.data.parsers

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.VideoSourceType
import com.greenrou.kanata.domain.parser.SiteParser
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URL
import java.net.URLEncoder

class AnitubeSiteParser : SiteParser {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    private val animeUrlPattern = Regex("""/\d+-[^/]+\.html$""")

    override val label = "AniTube"
    override val sourceType = VideoSourceType.ANITUBE

    override fun supports(host: String) = "anitube" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val document = Jsoup.connect("https://anitube.in.ua/index.php?do=search&subaction=search&story=$encoded")
            .userAgent(userAgent).get()

        val searchArea = document.selectFirst("#dle-content, #content, .content, .search-results") ?: document

        searchArea.select("a[href]")
            .map { it.attr("abs:href") }
            .filter { href -> "anitube.in.ua" in href && animeUrlPattern.containsMatchIn(href) }
            .distinct()
            .firstOrNull() ?: error("No results found on AniTube for: $query")
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> {
        val siteHost = URL(pageUrl).host
        val document = Jsoup.connect(pageUrl).userAgent(userAgent).get()

        val xfplayer = document.select("[data-params], [data-player]").firstOrNull()
        if (xfplayer != null) {
            val attrName = if (xfplayer.hasAttr("data-params")) "data-params" else "data-player"
            val dataParams = xfplayer.attr(attrName)
            val base = URL(pageUrl)
            val ajaxUrl = "${base.protocol}://${base.host}/engine/ajax/controller.php?$dataParams"

            runCatching {
                val json = Jsoup.connect(ajaxUrl)
                    .userAgent(userAgent)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", pageUrl)
                    .ignoreContentType(true)
                    .execute().body()

                val playerUrl = JSONObject(json).optString("data")
                if (playerUrl.isNotBlank()) {
                    val inputData = Jsoup.connect(playerUrl)
                        .userAgent(userAgent)
                        .header("Referer", pageUrl)
                        .get()
                        .getElementById("inputData")

                    if (inputData != null) {
                        val playlist = JSONObject(inputData.text())
                        val episodes = mutableListOf<Episode>()
                        playlist.keys().forEach { seasonKey ->
                            val season = playlist.getJSONObject(seasonKey)
                            season.keys().forEach { epKey ->
                                val label = if (playlist.length() > 1) "S${seasonKey}E${epKey}" else "Episode $epKey"
                                episodes.add(Episode(label, pageUrl))
                            }
                        }
                        if (episodes.isNotEmpty()) return episodes
                    }
                }
            }
        }

        val iframe = document.select("iframe[src], iframe[data-src], iframe[data-litespeed-src]").firstOrNull()
        if (iframe != null) return listOf(Episode("Watch", pageUrl))

        val slug = pageUrl.trimEnd('/').substringAfterLast("/").removeSuffix(".html")
        val linkedEpisodes = document
            .select("a[href*=${slug}]")
            .map { it.attr("abs:href") }
            .filter { href ->
                runCatching { URL(href).host }.getOrDefault("") == siteHost && href != pageUrl
            }
            .map { href ->
                val epNumber = Regex("""-(\d+)(?:\.html)?$""").find(href)?.groupValues?.get(1) ?: ""
                Episode(if (epNumber.isNotEmpty()) "Episode $epNumber" else href.substringAfterLast("/"), href)
            }
            .distinctBy { it.url }
            .sortedBy { it.title }

        if (linkedEpisodes.isNotEmpty()) return linkedEpisodes

        val title = document.selectFirst("h1.story__title, h1.fz28, h1")?.text() ?: "Watch"
        return listOf(Episode(title, pageUrl))
    }
}
