package com.greenrou.kanata.data.repository

import android.util.Log
import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.repository.EpisodeListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URL

private const val TAG = "Kanata:Episodes"

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
            ?: return fallbackEpisode(animePageUrl, document)

        val dataParams = xfplayer.attr("data-params")
        val base = URL(animePageUrl)
        val ajaxUrl = "${base.protocol}://${base.host}/engine/ajax/controller.php?$dataParams"

        return runCatching {
            val json = Jsoup.connect(ajaxUrl)
                .userAgent(userAgent)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", animePageUrl)
                .ignoreContentType(true)
                .execute().body()

            val playerUrl = JSONObject(json).optString("data")
            if (playerUrl.isBlank()) return@runCatching fallbackEpisode(animePageUrl, document)

            val playerDoc = Jsoup.connect(playerUrl)
                .userAgent(userAgent)
                .header("Referer", animePageUrl)
                .get()

            val inputData = playerDoc.getElementById("inputData")
            if (inputData != null) {
                val episodes = extractFromInputData(inputData.text(), animePageUrl)
                if (episodes.isNotEmpty()) return@runCatching episodes
            }

            val playerBase = URL(playerUrl).let { "${it.protocol}://${it.host}" }
            val encodedRef = java.net.URLEncoder.encode(animePageUrl, "UTF-8")

            val dataEpElements = playerDoc.select("[data-episode]")
            if (dataEpElements.isNotEmpty()) {
                val episodes = dataEpElements.mapNotNull { el ->
                    val epId = el.attr("data-id").ifBlank { return@mapNotNull null }
                    val epHash = el.attr("data-hash").ifBlank { return@mapNotNull null }
                    val ep = el.attr("data-episode")
                    val season = el.attr("data-season").ifBlank { "1" }
                    val label = if (season == "1") "Серія $ep" else "S${season}E$ep"
                    Episode(label, "$playerBase/seria/$epId/$epHash/720p?yref=$encodedRef")
                }.distinctBy { it.url }
                if (episodes.isNotEmpty()) return@runCatching episodes
            }

            val seriesSelect = playerDoc.selectFirst(".serial-series-box select")
            if (seriesSelect != null) {
                val episodes = seriesSelect.select("option").mapNotNull { opt ->
                    val epId = opt.attr("data-id").ifBlank { return@mapNotNull null }
                    val epHash = opt.attr("data-hash").ifBlank { return@mapNotNull null }
                    val title = opt.attr("data-title").ifBlank { "Серія ${opt.attr("value")}" }
                    Episode(title, "$playerBase/seria/$epId/$epHash/720p?yref=$encodedRef")
                }
                Log.d(TAG, "Episodes from serial-series-box: ${episodes.size}")
                if (episodes.isNotEmpty()) return@runCatching episodes
            }

            fallbackEpisode(animePageUrl, document)
        }.getOrElse {
            Log.e(TAG, "parseYummyEpisodes failed", it)
            fallbackEpisode(animePageUrl, document)
        }
    }

    private fun extractFromInputData(json: String, animePageUrl: String): List<Episode> {
        return runCatching {
            val playlist = JSONObject(json)
            val episodes = mutableListOf<Episode>()
            val multiSeason = playlist.length() > 1
            playlist.keys().forEach { seasonKey ->
                val season = playlist.getJSONObject(seasonKey)
                season.keys().forEach { epKey ->
                    val label = if (multiSeason) "S${seasonKey}E${epKey}" else "Серія $epKey"
                    episodes.add(Episode(label, "$animePageUrl?kodikEpisode=$epKey&kodikSeason=$seasonKey"))
                }
            }
            episodes
        }.getOrDefault(emptyList())
    }

    private fun fallbackEpisode(animePageUrl: String, document: org.jsoup.nodes.Document): List<Episode> {
        val title = document.selectFirst("h1.post-title, h1, .fz28")?.text() ?: "Watch"
        return listOf(Episode(title, animePageUrl))
    }
}
