package com.greenrou.kanata.data.repository

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.repository.EpisodeListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URL

class EpisodeListRepositoryImpl : EpisodeListRepository {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    override suspend fun getEpisodes(animePageUrl: String): Result<List<Episode>> = withContext(Dispatchers.IO) {
        runCatching {
            val host = URL(animePageUrl).host
            val document = Jsoup.connect(animePageUrl).userAgent(userAgent).get()
            when {
                "aniwave" in host -> parseAniwaveEpisodes(animePageUrl, document)
                "yummyanime" in host -> parseYummyEpisodes(animePageUrl, document)
                else -> error("Unsupported site: $host")
            }
        }
    }

    private fun parseAniwaveEpisodes(animePageUrl: String, document: org.jsoup.nodes.Document): List<Episode> {
        val slug = animePageUrl.trimEnd('/').substringAfterLast("/")
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
        return listOf(Episode(title, animePageUrl))
    }

    private fun parseYummyEpisodes(animePageUrl: String, document: org.jsoup.nodes.Document): List<Episode> {
        val xfplayer = document
            .select(".tabs-block__content:not(.d-none):not(.hidden) .xfplayer[data-params]")
            .firstOrNull()

        if (xfplayer != null) {
            val dataParams = xfplayer.attr("data-params")
            val base = URL(animePageUrl)
            val ajaxUrl = "${base.protocol}://${base.host}/engine/ajax/controller.php?$dataParams"

            runCatching {
                val json = Jsoup.connect(ajaxUrl)
                    .userAgent(userAgent)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", animePageUrl)
                    .ignoreContentType(true)
                    .execute().body()

                val playerUrl = JSONObject(json).optString("data")
                if (playerUrl.isNotBlank()) {
                    val playerDoc = Jsoup.connect(playerUrl)
                        .userAgent(userAgent)
                        .header("Referer", animePageUrl)
                        .get()

                    val inputData = playerDoc.getElementById("inputData")
                    if (inputData != null) {
                        val playlist = JSONObject(inputData.text())
                        val episodes = mutableListOf<Episode>()
                        playlist.keys().forEach { seasonKey ->
                            val season = playlist.getJSONObject(seasonKey)
                            season.keys().forEach { epKey ->
                                val label = if (playlist.length() > 1)
                                    "S${seasonKey}E${epKey}" else "Episode $epKey"
                                episodes.add(Episode(label, animePageUrl))
                            }
                        }
                        if (episodes.isNotEmpty()) return episodes
                    }
                }
            }
        }

        val title = document.selectFirst("h1.post-title, h1, .fz28")?.text() ?: "Watch"
        return listOf(Episode(title, animePageUrl))
    }
}
