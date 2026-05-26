package com.kanata.mod.aniwatch

import android.util.Log
import com.greenrou.kanata.modapi.ModEpisode
import com.greenrou.kanata.modapi.ModSiteParser
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
        val searchUrl = "$base/?s=$encoded"
        Log.d(TAG, "search: GET $searchUrl")
        val doc = Jsoup.connect(searchUrl)
            .userAgent(userAgent)
            .referrer(base)
            .get()

        val allHrefs = doc.select("a[href]").map { it.attr("abs:href") }.distinct()
        Log.d(TAG, "search: ${allHrefs.size} links total")

        val seriesLinks = allHrefs.filter { "/series/" in it }
        val animeLinks  = allHrefs.filter { "/anime/"  in it }
        Log.d(TAG, "search: /series/ links=${seriesLinks.take(3)}, /anime/ links=${animeLinks.take(3)}")

        val result = seriesLinks.firstOrNull()
            ?: animeLinks.firstOrNull()
            ?: allHrefs.filter { base in it && it.count { c -> c == '/' } >= 4 }.firstOrNull()
            ?: error("No results on KayoAnime for: $query. Sample links: ${allHrefs.take(5)}")
        Log.d(TAG, "search: returning $result")
        result
    }

    override suspend fun getEpisodes(pageUrl: String): List<ModEpisode> {
        Log.d(TAG, "getEpisodes: GET $pageUrl")
        val doc = Jsoup.connect(pageUrl)
            .userAgent(userAgent)
            .referrer(base)
            .get()

        val totalEpisodes = Regex("""Episodes?\s*[:\-]\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(doc.text())
            ?.groupValues?.get(1)?.toIntOrNull()
            ?: 1
        Log.d(TAG, "getEpisodes: totalEpisodes=$totalEpisodes")

        val sampleHref = doc.select("a[href]")
            .map { it.attr("abs:href") }
            .firstOrNull { EPISODE_SEGMENT.containsMatchIn(it) }
        Log.d(TAG, "getEpisodes: sampleHref=$sampleHref")

        val slug: String
        val suffix: String

        if (sampleHref != null) {
            val m = Regex("""/([\w-]+)-episode-\d+-([\w-]+)/?$""").find(sampleHref)
            slug = m?.groupValues?.get(1) ?: slugFrom(pageUrl)
            suffix = m?.groupValues?.get(2) ?: "english-subbed"
        } else {
            slug = slugFrom(pageUrl)
            suffix = "english-subbed"
        }

        val episodes = (1..totalEpisodes).map { n ->
            ModEpisode("Episode $n", "$base/$slug-episode-$n-$suffix/")
        }
        Log.d(TAG, "getEpisodes: slug=$slug suffix=$suffix ep[0]=${episodes.firstOrNull()?.url}")
        return episodes
    }

    private fun slugFrom(seriesUrl: String) =
        seriesUrl.trimEnd('/').substringAfterLast("/")

    companion object {
        private const val TAG = "AniWatchMod"
        private val EPISODE_SEGMENT = Regex("""-episode-\d+-""")
    }
}
