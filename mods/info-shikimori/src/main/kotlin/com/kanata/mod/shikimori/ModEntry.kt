package com.kanata.mod.shikimori

import android.util.Log
import com.greenrou.kanata.modapi.ModAnimeInfo
import com.greenrou.kanata.modapi.ModInfoProvider
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

class ModEntry : ModInfoProvider {

    private val userAgent = "Kanata Android App"
    private val apiBase = "https://shikimori.one/api"

    override val id = "info-shikimori"
    override val label = "Shikimori"

    override suspend fun getInfo(titles: List<String>): Result<ModAnimeInfo> = runCatching {
        val animeId = findId(titles) ?: error("Shikimori: no results for $titles")
        Log.d(TAG, "getInfo: found id=$animeId")
        fetchDetail(animeId)
    }

    private fun findId(titles: List<String>): Int? {
        for (title in titles) {
            val encoded = URLEncoder.encode(title, "UTF-8")
            val url = "$apiBase/animes?search=$encoded&limit=5&order=popularity"
            Log.d(TAG, "findId: GET $url")
            val body = get(url)
            val arr = JSONArray(body)
            if (arr.length() > 0) return arr.getJSONObject(0).getInt("id")
        }
        return null
    }

    private fun fetchDetail(id: Int): ModAnimeInfo {
        val url = "$apiBase/animes/$id"
        Log.d(TAG, "fetchDetail: GET $url")
        val obj = JSONObject(get(url))

        val synopsis = obj.optString("description").takeIf { it.isNotBlank() }
            ?.let { stripWikiMarkup(it) }

        val scoreRaw = obj.optString("score").toDoubleOrNull()?.takeIf { it > 0 }

        val genres = obj.optJSONArray("genres")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                arr.getJSONObject(i).optString("russian").takeIf { it.isNotBlank() }
            }
        } ?: emptyList()

        val studios = obj.optJSONArray("studios")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                arr.getJSONObject(i).optString("filtered_name").takeIf { it.isNotBlank() }
            }
        } ?: emptyList()

        Log.d(TAG, "fetchDetail: synopsis=${synopsis?.take(40)}, score=$scoreRaw, studios=$studios")
        return ModAnimeInfo(
            synopsis = synopsis,
            score = scoreRaw,
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

    private fun stripWikiMarkup(text: String): String =
        text
            .replace(Regex("""\[url=[^\]]*\]"""), "")
            .replace(Regex("""\[/url\]"""), "")
            .replace(Regex("""\[/?[a-z]+\]"""), "")
            .trim()

    companion object {
        private const val TAG = "ShikimoriMod"
    }
}
