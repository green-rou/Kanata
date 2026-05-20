package com.greenrou.kanata.data.parsers

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.VideoSourceType
import com.greenrou.kanata.domain.parser.SiteParser
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URL
import java.net.URLEncoder

class AstarSiteParser : SiteParser {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    private val animeUrlPattern = Regex("""astar\.bz/\d+[^/]+\.html""")

    override val label = "AniStar"
    override val sourceType = VideoSourceType.ASTAR

    override fun supports(host: String) = "astar.bz" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://v24.astar.bz/index.php?do=search&subaction=search&story=$encoded"
        val document = Jsoup.connect(searchUrl).userAgent(userAgent).get()
        val result = document.select("a[href]")
            .map { it.attr("abs:href") }
            .firstOrNull { animeUrlPattern.containsMatchIn(it) }
        result ?: error("No results on AniStar for: $query")
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> = getEpisodes(pageUrl, 0)

    override suspend fun getEpisodes(pageUrl: String, expectedEpisodes: Int): List<Episode> {
        val document = Jsoup.connect(pageUrl).userAgent(userAgent).get()

        val xfplayer = document
            .select(".tabs-block__content:not(.d-none):not(.hidden) .xfplayer[data-params], .xfplayer[data-params]")
            .firstOrNull()

        if (xfplayer != null) {
            val dataParams = xfplayer.attr("data-params")
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
                                val epLabel = if (playlist.length() > 1)
                                    "S${seasonKey}E${epKey}" else "Серія $epKey"
                                val epUrl = "$pageUrl?kodikSeason=$seasonKey&kodikEpisode=$epKey"
                                episodes.add(Episode(epLabel, epUrl))
                            }
                        }
                        if (episodes.isNotEmpty()) return episodes
                    }
                }
            }.onFailure { e ->
            }
        }

        val iframes = document.select("iframe[src]")
            .map { it.attr("src").trim() }
            .filter { it.isNotBlank() && !it.startsWith("about:") && "adblock" !in it && "banner" !in it }

        val kodikIframeSrc = iframes.firstOrNull { "kodik" in it || "/serial/" in it || "/video/" in it || "/seria/" in it || "/film/" in it }
        if (kodikIframeSrc != null) {
            val kodikUrl = when {
                kodikIframeSrc.startsWith("http") -> kodikIframeSrc
                kodikIframeSrc.startsWith("//") -> "https:$kodikIframeSrc"
                else -> { val b = URL(pageUrl); "${b.protocol}://${b.host}$kodikIframeSrc" }
            }
            runCatching {
                val playerDoc = Jsoup.connect(kodikUrl).userAgent(userAgent).header("Referer", pageUrl).get()
                val inputData = playerDoc.getElementById("inputData")
                if (inputData != null) {
                    val playlist = JSONObject(inputData.text())
                    val episodes = mutableListOf<Episode>()
                    playlist.keys().forEach { seasonKey ->
                        val season = playlist.getJSONObject(seasonKey)
                        season.keys().forEach { epKey ->
                            val epLabel = if (playlist.length() > 1)
                                "S${seasonKey}E${epKey}" else "Серія $epKey"
                            episodes.add(Episode(epLabel, "$pageUrl?kodikSeason=$seasonKey&kodikEpisode=$epKey"))
                        }
                    }
                    if (episodes.isNotEmpty()) return episodes
                }
            }.onFailure { e ->
            }

            if (expectedEpisodes > 1 && "/serial/" in kodikIframeSrc) {
                return (1..expectedEpisodes).map { ep ->
                    Episode("Серія $ep", "$pageUrl?kodikSeason=1&kodikEpisode=$ep")
                }
            }
        }

        val customPlayerIframe = iframes.firstOrNull { "/player" in it || "videoas" in it }
        if (expectedEpisodes > 1 && customPlayerIframe != null) {
            return (1..expectedEpisodes).map { ep ->
                Episode("Серія $ep", "$pageUrl?astarEpisode=$ep")
            }
        }

        val title = document.selectFirst("h1.post-title, h1.fz28, h1")?.text() ?: "Watch"
        return listOf(Episode(title, pageUrl))
    }
}
