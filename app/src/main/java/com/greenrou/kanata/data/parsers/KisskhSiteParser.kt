package com.greenrou.kanata.data.parsers

import android.util.Log
import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.VideoSourceType
import com.greenrou.kanata.domain.parser.SiteParser
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

class KisskhSiteParser : SiteParser {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val base = "https://kisskh.co"

    override val label = "KissKH"
    override val sourceType = VideoSourceType.KISSKH

    override fun supports(host: String) = "kisskh.co" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "$base/api/DramaList/Search?q=$encoded&type=0"
        Log.d("KissKH", "search: GET $url")
        val body = Jsoup.connect(url)
            .userAgent(userAgent)
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "$base/")
            .header("X-Requested-With", "XMLHttpRequest")
            .ignoreContentType(true)
            .execute().body()

        Log.d("KissKH", "search response (first 300): ${body.take(300)}")
        val arr = JSONArray(body)
        if (arr.length() == 0) error("No results on KissKH for: $query")

        val first = arr.getJSONObject(0)
        val id = first.getInt("id")
        Log.d("KissKH", "search: first result id=$id title=${first.optString("title")}")
        "$base/drama/$id"
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> {
        val id = Regex("""/drama/(\d+)""").find(pageUrl)?.groupValues?.get(1)
            ?: error("Cannot extract drama ID from KissKH URL: $pageUrl")

        Log.d("KissKH", "getEpisodes: drama id=$id")
        val body = Jsoup.connect("$base/api/DramaList/Drama/$id?isq=false")
            .userAgent(userAgent)
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "$base/")
            .header("X-Requested-With", "XMLHttpRequest")
            .ignoreContentType(true)
            .execute().body()

        val drama = JSONObject(body)
        Log.d("KissKH", "getEpisodes: drama title=${drama.optString("title")} episodesCount=${drama.optInt("episodesCount")}")

        val episodes = drama.optJSONArray("episodes")
            ?: run {
                Log.w("KissKH", "getEpisodes: no 'episodes' array in response, keys=${drama.keys().asSequence().toList()}")
                return listOf(Episode("Дивитись", pageUrl))
            }

        val titleSlug = drama.optString("title")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .trim()
            .replace(Regex("\\s+"), "-")

        Log.d("KissKH", "getEpisodes: episodes in JSON = ${episodes.length()}, titleSlug=$titleSlug")
        return (0 until episodes.length())
            .map { i ->
                val ep = episodes.getJSONObject(i)
                val epId = ep.getInt("id")
                val num = ep.optDouble("number", (i + 1).toDouble())
                val numStr = if (num % 1.0 == 0.0) num.toInt().toString() else "%.1f".format(num)
                val epNumUrl = if (num % 1.0 == 0.0) num.toInt().toString() else numStr
                val dramaPage = "$base/Drama/$titleSlug/Episode-$epNumUrl?id=$id&ep=$epId&page=0&pageSize=100"
                Episode("Серія $numStr", "$base/episode/$epId?drama_page=${URLEncoder.encode(dramaPage, "UTF-8")}")
            }
            .reversed()
            .also { Log.d("KissKH", "getEpisodes: returning ${it.size} episodes (first=${it.firstOrNull()?.url})") }
    }
}
