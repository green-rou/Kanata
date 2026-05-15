package com.greenrou.kanata.data.parsers

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.parser.SiteParser
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

class MikaiSiteParser : SiteParser {

    private val apiBase = "https://api.mikai.me/v1"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    override val label = "Mikai"

    override fun supports(host: String) = "mikai" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val json = Jsoup.connect("$apiBase/anime/search?name=$encoded&limit=1")
            .userAgent(userAgent)
            .ignoreContentType(true)
            .execute().body()

        val result = JSONObject(json)
        if (!result.optBoolean("ok")) error("Mikai API error: ${result.optJSONObject("error")?.optString("message")}")

        val items = result.optJSONArray("result") ?: error("No results for query: $query")
        if (items.length() == 0) error("No results for query: $query")

        val first = items.getJSONObject(0)
        val id = first.getInt("id")
        val slug = first.getString("slug")
        "https://mikai.me/anime/$id-$slug"
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> {
        val id = Regex("""/anime/(\d+)""").find(pageUrl)?.groupValues?.get(1)
            ?: error("Cannot extract anime id from URL: $pageUrl")

        val json = Jsoup.connect("$apiBase/anime/$id")
            .userAgent(userAgent)
            .ignoreContentType(true)
            .execute().body()

        val result = JSONObject(json)
        if (!result.optBoolean("ok")) error("Mikai API error for anime $id")

        val players = result.getJSONObject("result").optJSONArray("players")
            ?: return listOf(Episode("Watch", pageUrl))

        val bestPlayer = (0 until players.length())
            .map { players.getJSONObject(it) }
            .maxByOrNull { player ->
                val providers = player.optJSONArray("providers") ?: return@maxByOrNull 0
                (0 until providers.length()).sumOf { providers.getJSONObject(it).optJSONArray("episodes")?.length() ?: 0 }
            } ?: return listOf(Episode("Watch", pageUrl))

        val providers = bestPlayer.optJSONArray("providers")
            ?: return listOf(Episode("Watch", pageUrl))

        val bestProvider = (0 until providers.length())
            .map { providers.getJSONObject(it) }
            .maxByOrNull { it.optJSONArray("episodes")?.length() ?: 0 }
            ?: return listOf(Episode("Watch", pageUrl))

        val episodes = bestProvider.optJSONArray("episodes")
            ?: return listOf(Episode("Watch", pageUrl))

        return (0 until episodes.length()).map { i ->
            val ep = episodes.getJSONObject(i)
            val number = ep.optInt("number", i + 1)
            val link = ep.getString("playLink")
            Episode("Серія $number", link)
        }
    }
}
