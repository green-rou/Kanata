package com.kanata.mod.aniwatch

import android.util.Log
import com.greenrou.kanata.modapi.ModEpisode
import com.greenrou.kanata.modapi.ModSiteParser
import org.jsoup.Jsoup

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
        slugCandidates(query)
            .firstNotNullOfOrNull { slug ->
                val url = "$base/anime/$slug/"
                Log.d(TAG, "search: trying $url")
                if (pageExists(url)) url.also { Log.d(TAG, "search: hit $url") } else null
            } ?: error("No results on KayoAnime for: $query")
    }.onFailure { Log.w(TAG, "search failed '$query': ${it.javaClass.simpleName}: ${it.message}") }

    private fun slugCandidates(query: String): List<String> {
        val out = LinkedHashSet<String>()
        out.add(toSlug(query))
        val beforeColon = query.substringBefore(":").trim()
        if (beforeColon != query) out.add(toSlug(beforeColon))
        val words = query.split(Regex("[\\s:-]+")).filter { it.isNotEmpty() }
        if (words.size > 3) out.add(toSlug(words.take(3).joinToString(" ")))
        if (words.size > 2) out.add(toSlug(words.take(2).joinToString(" ")))
        return out.toList()
    }

    private fun toSlug(title: String): String =
        title.lowercase()
            .replace(Regex("[^a-z0-9 -]"), "")
            .trim()
            .replace(Regex("[ -]+"), "-")

    private fun pageExists(url: String): Boolean = try {
        val res = Jsoup.connect(url)
            .userAgent(userAgent)
            .referrer(base)
            .ignoreHttpErrors(true)
            .timeout(8_000)
            .execute()
        res.statusCode() == 200 && "Page not found" !in res.body().take(1000)
    } catch (e: Exception) {
        Log.w(TAG, "pageExists: $url threw ${e.javaClass.simpleName}")
        false
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
