package com.greenrou.kanata.data.parsers

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.parser.SiteParser
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URL
import java.net.URLEncoder

class AnitubeSiteParser : SiteParser {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    private val animeUrlPattern = Regex("""/\d+-[^/]+\.html$""")

    override val label = "AniTube"

    override fun supports(host: String) = "anitube" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://anitube.in.ua/index.php?do=search&subaction=search&story=$encodedQuery"
        val document = Jsoup.connect(url).userAgent(userAgent).get()
        val searchArea = document.selectFirst("#dle-content, #content, .content, .search-results")
        val target = searchArea ?: document
        val matches = target.select("a[href]")
            .map { it.attr("abs:href") }
            .filter { href -> "anitube.in.ua" in href && animeUrlPattern.containsMatchIn(href) }
            .distinct()
        matches.firstOrNull() ?: error("No results found on AniTube for query: $query")
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> {
        val siteHost = URL(pageUrl).host
        val document = Jsoup.connect(pageUrl).userAgent(userAgent).get()

        val newsId = Regex("""/(\d+)-[^/]+\.html""").find(pageUrl)?.groupValues?.get(1)
        if (newsId != null) {
            val base = "https://$siteHost"
            for (mod in listOf("kodik", "dleplayer", "player", "videocdn")) {
                val episodes = runCatching {
                    val ajaxUrl = "$base/engine/ajax/controller.php?mod=$mod&news_id=$newsId"
                    val json = Jsoup.connect(ajaxUrl)
                        .userAgent(userAgent)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Referer", pageUrl)
                        .ignoreContentType(true)
                        .execute().body()
                    val playerUrl = JSONObject(json).optString("data")
                    if (playerUrl.isBlank()) return@runCatching null
                    val playerDoc = Jsoup.connect(playerUrl)
                        .userAgent(userAgent)
                        .header("Referer", pageUrl)
                        .get()
                    val inputData = playerDoc.getElementById("inputData") ?: return@runCatching null
                    val playlist = JSONObject(inputData.text())
                    val result = mutableListOf<Episode>()
                    playlist.keys().forEach { seasonKey ->
                        val season = playlist.getJSONObject(seasonKey)
                        season.keys().forEach { epKey ->
                            val label = if (playlist.length() > 1) "S${seasonKey}E${epKey}" else "Episode $epKey"
                            result.add(Episode(label, pageUrl))
                        }
                    }
                    if (result.isNotEmpty()) result else null
                }.getOrNull()
                if (episodes != null) return episodes
            }
        }

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
                    val playerDoc = Jsoup.connect(playerUrl)
                        .userAgent(userAgent)
                        .header("Referer", pageUrl)
                        .get()
                    val inputData = playerDoc.getElementById("inputData")
                    if (inputData != null) {
                        val playlist = JSONObject(inputData.text())
                        val episodes = mutableListOf<Episode>()
                        playlist.keys().forEach { seasonKey ->
                            val season = playlist.getJSONObject(seasonKey)
                            season.keys().forEach { epKey ->
                                val epLabel = if (playlist.length() > 1) "S${seasonKey}E${epKey}" else "Episode $epKey"
                                episodes.add(Episode(epLabel, pageUrl))
                            }
                        }
                        if (episodes.isNotEmpty()) return episodes
                    }
                }
            }
        }

        val iframe = document.select("iframe[src], iframe[data-src], iframe[data-litespeed-src]").firstOrNull()
        if (iframe != null) return listOf(Episode("Watch", pageUrl))

        val playerKeywords = listOf(
            "var file", "var pl", "playerUrl", "hls:", "m3u8",
            "DLEPlayer", "\"file\":", "'file':", "playerSource", ".init(",
            "kodik", "iframe"
        )
        val scriptWithPlayer = document.select("script:not([src])").firstOrNull { script ->
            playerKeywords.any { script.data().contains(it) }
        }
        if (scriptWithPlayer != null) {
            val dleMatch = Regex("""DLEPlayer\.init\s*\(\s*["'][^"']*["']\s*,\s*(\[[\s\S]*?\])\s*\)""")
                .find(scriptWithPlayer.data())
            if (dleMatch != null) {
                val episodes = runCatching {
                    val arr = org.json.JSONArray(dleMatch.groupValues[1])
                    (0 until arr.length()).map { i ->
                        val obj = arr.getJSONObject(i)
                        Episode(obj.optString("label").ifBlank { "Episode ${i + 1}" }, obj.optString("file"))
                    }.filter { it.url.isNotBlank() }
                }.getOrNull()
                if (!episodes.isNullOrEmpty()) return episodes
            }
            return listOf(Episode("Watch", pageUrl))
        }

        val slug = pageUrl.trimEnd('/').substringAfterLast("/").removeSuffix(".html")
        val linkedEpisodes = document
            .select("a[href*=${slug}]")
            .map { it.attr("abs:href") }
            .filter { href ->
                val hrefHost = runCatching { URL(href).host }.getOrDefault("")
                hrefHost == siteHost && href != pageUrl
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
