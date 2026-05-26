package com.kanata.mod.aniwatch

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
        val doc = Jsoup.connect("$base/?s=$encoded")
            .userAgent(userAgent)
            .referrer(base)
            .get()

        doc.select("a[href]")
            .map { it.attr("abs:href") }
            .filter { "/series/" in it }
            .distinct()
            .firstOrNull() ?: error("No results on KayoAnime for: $query")
    }

    override suspend fun getEpisodes(pageUrl: String): List<ModEpisode> {
        val doc = Jsoup.connect(pageUrl)
            .userAgent(userAgent)
            .referrer(base)
            .get()

        val totalEpisodes = Regex("""Episodes?\s*[:\-]\s*(\d+)""", RegexOption.IGNORE_CASE)
            .find(doc.text())
            ?.groupValues?.get(1)?.toIntOrNull()
            ?: 1

        val sampleHref = doc.select("a[href]")
            .map { it.attr("abs:href") }
            .firstOrNull { EPISODE_SEGMENT.containsMatchIn(it) }

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

        return (1..totalEpisodes).map { n ->
            ModEpisode("Episode $n", "$base/$slug-episode-$n-$suffix/")
        }
    }

    private fun slugFrom(seriesUrl: String) =
        seriesUrl.trimEnd('/').substringAfterLast("/")

    companion object {
        private val EPISODE_SEGMENT = Regex("""-episode-\d+-""")
    }
}
