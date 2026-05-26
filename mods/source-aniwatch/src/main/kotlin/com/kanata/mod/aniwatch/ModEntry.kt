package com.kanata.mod.aniwatch

import android.util.Log
import com.greenrou.kanata.modapi.ModEpisode
import com.greenrou.kanata.modapi.ModSiteParser
import org.json.JSONArray
import org.jsoup.Jsoup
import java.net.URLEncoder

class ModEntry : ModSiteParser {

    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val base = "https://ww2.aniwatch.fit"

    override val id = "source-aniwatch"
    override val label = "KayoAnime"
    override val language = "en"

    override fun supports(host: String) =
        "aniwatch.fit" in host || "kayoanime.sa.com" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val apiUrl = "$base/wp-json/wp/v2/anime?search=$encoded&per_page=5"
        Log.d(TAG, "search: GET $apiUrl")
        val body = Jsoup.connect(apiUrl)
            .userAgent(userAgent)
            .ignoreContentType(true)
            .execute().body()
        val arr = JSONArray(body)
        Log.d(TAG, "search: ${arr.length()} results")
        if (arr.length() == 0) error("No results on KayoAnime for: $query")
        val link = arr.getJSONObject(0).getString("link")
        Log.d(TAG, "search: returning $link")
        link
    }

    override suspend fun getEpisodes(pageUrl: String): List<ModEpisode> {
        Log.d(TAG, "getEpisodes: GET $pageUrl")
        val doc = Jsoup.connect(pageUrl)
            .userAgent(userAgent)
            .referrer(base)
            .get()

        val directLinks = doc.select("a[href]")
            .map { it.attr("abs:href") }
            .filter { base in it && EPISODE_SEGMENT.containsMatchIn(it) }
            .distinct()
        Log.d(TAG, "getEpisodes: directLinks(${directLinks.size})=${directLinks.take(3)}")

        if (directLinks.isNotEmpty()) {
            val sorted = directLinks.sortedBy {
                EPISODE_SEGMENT.find(it)?.value?.filter { c -> c.isDigit() }?.toIntOrNull() ?: 0
            }
            val episodes = sorted.mapIndexed { i, url ->
                val n = EPISODE_SEGMENT.find(url)?.value?.filter { c -> c.isDigit() }?.toIntOrNull() ?: (i + 1)
                ModEpisode("Episode $n", url)
            }
            Log.d(TAG, "getEpisodes: returning ${episodes.size} direct episodes, ep[0]=${episodes.firstOrNull()?.url}")
            return episodes
        }

        val totalEpisodes = Regex("""Episodes?\s*[:\-]\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(doc.text())
            ?.groupValues?.get(1)?.toIntOrNull()
            ?: 1
        Log.d(TAG, "getEpisodes: no direct links found, totalEpisodes=$totalEpisodes")

        val sampleHref = doc.select("a[href]")
            .map { it.attr("abs:href") }
            .firstOrNull { EPISODE_SEGMENT.containsMatchIn(it) }
        Log.d(TAG, "getEpisodes: sampleHref=$sampleHref")

        val slug: String
        val suffix: String

        if (sampleHref != null) {
            val m = Regex("""/([\w-]+)-episode-\d+(?:-([\w-]+))?/?$""").find(sampleHref)
            slug = m?.groupValues?.get(1) ?: slugFrom(pageUrl)
            suffix = m?.groupValues?.get(2).orEmpty()
        } else {
            slug = slugFrom(pageUrl)
            suffix = ""
        }

        val episodes = (1..totalEpisodes).map { n ->
            val url = if (suffix.isNotEmpty())
                "$base/$slug-episode-$n-$suffix/"
            else
                "$base/$slug-episode-$n/"
            ModEpisode("Episode $n", url)
        }
        Log.d(TAG, "getEpisodes: slug=$slug suffix='$suffix' ep[0]=${episodes.firstOrNull()?.url}")
        return episodes
    }

    private fun slugFrom(seriesUrl: String) =
        seriesUrl.trimEnd('/').substringAfterLast("/")

    companion object {
        private const val TAG = "AniWatchMod"
        private val EPISODE_SEGMENT = Regex("""-episode-(\d+)""")
    }
}
