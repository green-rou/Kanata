package com.kanata.mod.hikka

import android.util.Log
import com.greenrou.kanata.modapi.ModAnimeInfo
import com.greenrou.kanata.modapi.ModInfoProvider
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup

class ModEntry : ModInfoProvider {

    private val userAgent = "Kanata Android App"
    private val apiBase = "https://api.hikka.io"

    override val id = "info-hikka"
    override val label = "Hikka"

    override suspend fun getInfo(titles: List<String>): Result<ModAnimeInfo> = runCatching {
        val slug = findSlug(titles) ?: error("Hikka: no results for $titles")
        Log.d(TAG, "getInfo: found slug=$slug")
        fetchDetail(slug)
    }

    private fun findSlug(titles: List<String>): String? {
        for (title in titles) {
            val url = "$apiBase/anime?page=1&size=5"
            Log.d(TAG, "findSlug: POST $url query=$title")
            val body = post(url, JSONObject().put("query", title).toString())
            val list = JSONObject(body).getJSONArray("list")
            if (list.length() > 0) return list.getJSONObject(0).getString("slug")
        }
        return null
    }

    private fun fetchDetail(slug: String): ModAnimeInfo {
        val url = "$apiBase/anime/$slug"
        Log.d(TAG, "fetchDetail: GET $url")
        val obj = JSONObject(get(url))

        val synopsis = obj.optString("synopsis_ua").takeIf { it.isNotBlank() }

        val score = obj.optDouble("score", 0.0).takeIf { it > 0 }

        val genres = obj.optJSONArray("genres")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                arr.getJSONObject(i).optString("name_ua").takeIf { it.isNotBlank() }
            }
        } ?: emptyList()

        val studios = obj.optJSONArray("companies")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val entry = arr.getJSONObject(i)
                if (entry.optString("type") == "studio") {
                    entry.getJSONObject("company").optString("name").takeIf { it.isNotBlank() }
                } else null
            }
        } ?: emptyList()

        Log.d(TAG, "fetchDetail: synopsis=${synopsis?.take(40)}, score=$score, studios=$studios")
        return ModAnimeInfo(
            synopsis = synopsis,
            score = score,
            scoreLabel = label,
            genres = genres,
            studios = studios,
        )
    }

    private fun get(url: String): String =
        Jsoup.connect(url)
            .userAgent(userAgent)
            .header("Accept", "application/json")
            .ignoreContentType(true)
            .execute()
            .body()

    private fun post(url: String, json: String): String =
        Jsoup.connect(url)
            .userAgent(userAgent)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .ignoreContentType(true)
            .method(Connection.Method.POST)
            .requestBody(json)
            .execute()
            .body()

    companion object {
        private const val TAG = "HikkaMod"
    }
}
