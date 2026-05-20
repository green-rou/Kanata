package com.greenrou.kanata.data.parsers

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.VideoSourceType
import com.greenrou.kanata.domain.parser.SiteParser
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URL
import java.net.URLEncoder

class HentaizSiteParser : SiteParser {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    override val label = "Hentaiz"
    override val sourceType = VideoSourceType.HENTAIZ
    override val isAdultOnly = true

    override fun supports(host: String) = "hentaiz" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "https://hentaiz.org/index.php?do=search&subaction=search&search_start=0&full_search=0&story=$encoded"
        val document = Jsoup.connect(searchUrl).userAgent(userAgent).get()
        val result = document.select("a[href]")
            .map { it.attr("abs:href") }
            .firstOrNull { Regex("hentaiz\\.org/\\d+[^/]*\\.html").containsMatchIn(it) }
        result ?: error("No results on Hentaiz for: $query")
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> {
        val document = Jsoup.connect(pageUrl).userAgent(userAgent).get()

        val xfplayer = document
            .select(".tabs-block__content:not(.d-none):not(.hidden) .xfplayer[data-params]")
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
                                    "S${seasonKey}E${epKey}" else "Episode $epKey"
                                val epUrl = "$pageUrl?kodikSeason=$seasonKey&kodikEpisode=$epKey"
                                episodes.add(Episode(epLabel, epUrl))
                            }
                        }
                        if (episodes.isNotEmpty()) return episodes
                    } else {
                    }
                }
            }.onFailure { e ->
            }
        } else {
        }

        val title = document.selectFirst("h1.post-title, h1, .fz28")?.text() ?: "Watch"
        return listOf(Episode(title, pageUrl))
    }
}
