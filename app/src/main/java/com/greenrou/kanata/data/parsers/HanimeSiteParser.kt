package com.greenrou.kanata.data.parsers

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.parser.SiteParser
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup

class HanimeSiteParser : SiteParser {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    override val label = "Hanime"
    override val isAdultOnly = true

    override fun supports(host: String) = "hanime.tv" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val requestBody = JSONObject().apply {
            put("search_text", query)
            put("tags", JSONArray())
            put("tags_mode", "AND")
            put("brands", JSONArray())
            put("blacklist", JSONArray())
            put("order_by", "views")
            put("ordering", "desc")
            put("page", 0)
        }.toString()

        val body = Jsoup.connect(SEARCH_URL)
            .userAgent(userAgent)
            .header("Content-Type", "application/json")
            .requestBody(requestBody)
            .ignoreContentType(true)
            .method(Connection.Method.POST)
            .execute().body()

        val hitsRaw = JSONObject(body).optString("hits", "")
        if (hitsRaw.isBlank() || hitsRaw == "null") error("No hits in search response")
        val hits = JSONArray(hitsRaw)
        if (hits.length() == 0) error("No results on Hanime for: $query")

        "https://hanime.tv/videos/hentai/${hits.getJSONObject(0).getString("slug")}"
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> {
        val cleanUrl = pageUrl.substringBefore("?")
        val slug = cleanUrl.trimEnd('/').substringAfterLast("/")
        val searchQuery = slug.replace(Regex("-\\d+$"), "").replace("-", " ")

        val requestBody = JSONObject().apply {
            put("search_text", searchQuery)
            put("tags", JSONArray())
            put("tags_mode", "AND")
            put("brands", JSONArray())
            put("blacklist", JSONArray())
            put("order_by", "created_at_unix")
            put("ordering", "asc")
            put("page", 0)
        }.toString()

        val hits = runCatching {
            JSONArray(
                JSONObject(
                    Jsoup.connect(SEARCH_URL)
                        .userAgent(userAgent)
                        .header("Content-Type", "application/json")
                        .requestBody(requestBody)
                        .ignoreContentType(true)
                        .method(Connection.Method.POST)
                        .execute().body()
                ).optString("hits", "[]")
            )
        }.getOrElse { return listOf(Episode("Watch", cleanUrl)) }

        if (hits.length() == 0) return listOf(Episode("Watch", cleanUrl))

        return (0 until hits.length()).map { i ->
            val hit = hits.getJSONObject(i)
            Episode(
                hit.optString("name", "Episode ${i + 1}"),
                "https://hanime.tv/videos/hentai/${hit.optString("slug")}?hid=${hit.optInt("id", -1)}",
            )
        }.sortedBy { it.title }
    }

    companion object {
        private const val SEARCH_URL = "https://search.htv-services.com/"
    }
}
