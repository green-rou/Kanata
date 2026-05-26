package com.kanata.mod.anilibria

import android.util.Log
import com.greenrou.kanata.modapi.ModEpisode
import com.greenrou.kanata.modapi.ModSiteParser
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

class ModEntry : ModSiteParser {

    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val base = "https://anilibria.top"
    private val apiBase = "$base/api/v1"

    override val id = "source-anilibria"
    override val label = "AniLibria"
    override val language = "ru"

    override fun supports(host: String) = "anilibria.top" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "$apiBase/anime/catalog/releases?search=$encoded&limit=5&page=1"
        Log.d(TAG, "search: GET $url")

        val body = Jsoup.connect(url)
            .userAgent(userAgent)
            .ignoreContentType(true)
            .execute()
            .body()

        val data = JSONObject(body).getJSONArray("data")
        Log.d(TAG, "search: ${data.length()} results")
        if (data.length() == 0) error("No results on AniLibria for: $query")

        val alias = data.getJSONObject(0).getString("alias")
        val result = "$base/anime/releases/$alias"
        Log.d(TAG, "search: returning $result (alias=$alias)")
        result
    }

    override suspend fun getEpisodes(pageUrl: String): List<ModEpisode> {
        val alias = pageUrl.trimEnd('/').substringAfterLast("/")
        val url = "$apiBase/anime/releases/$alias"
        Log.d(TAG, "getEpisodes: alias=$alias GET $url")

        val body = Jsoup.connect(url)
            .userAgent(userAgent)
            .ignoreContentType(true)
            .execute()
            .body()

        val obj = JSONObject(body)
        val episodesArray = obj.optJSONArray("episodes") ?: return emptyList()
        Log.d(TAG, "getEpisodes: ${episodesArray.length()} episodes found")

        val episodes = (0 until episodesArray.length())
            .map { i -> episodesArray.getJSONObject(i) }
            .mapNotNull { ep ->
                val ordinal = ep.optInt("ordinal", -1).takeIf { it >= 0 } ?: return@mapNotNull null
                val name = ep.optString("name").takeIf { it.isNotBlank() } ?: "Episode $ordinal"
                val hlsUrl = ep.optString("hls_1080").takeIf { it.isNotBlank() }
                    ?: ep.optString("hls_720").takeIf { it.isNotBlank() }
                    ?: ep.optString("hls_480").takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                ModEpisode("$ordinal. $name", hlsUrl)
            }
            .sortedBy { it.title.substringBefore(".").toIntOrNull() ?: 0 }

        Log.d(TAG, "getEpisodes: ep[0]=${episodes.firstOrNull()?.url}")
        return episodes
    }

    companion object {
        private const val TAG = "AniLibriaMod"
    }
}
