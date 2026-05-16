package com.greenrou.kanata.data.parsers

import com.greenrou.kanata.domain.model.Episode
import com.greenrou.kanata.domain.model.VideoSourceType
import com.greenrou.kanata.domain.parser.SiteParser
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder

class ArchiveOrgSiteParser : SiteParser {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    override val label = "Archive.org"
    override val sourceType = VideoSourceType.ARCHIVE_ORG

    override fun supports(host: String) = "archive.org" in host

    override suspend fun search(query: String): Result<String> = runCatching {
        val encoded = URLEncoder.encode("$query AND mediatype:movies", "UTF-8")
        val checkUrl = "https://archive.org/advancedsearch.php?q=$encoded&fl[]=identifier&rows=1&output=json"
        val docs = JSONObject(
            Jsoup.connect(checkUrl).userAgent(userAgent).ignoreContentType(true).execute().body()
        ).optJSONObject("response")?.optJSONArray("docs")
        if (docs == null || docs.length() == 0) error("No results on Archive.org for: $query")
        "https://archive.org/search?query=${URLEncoder.encode(query, "UTF-8")}"
    }

    override suspend fun getEpisodes(pageUrl: String): List<Episode> =
        if ("archive.org/search" in pageUrl) getEpisodesFromSearch(pageUrl)
        else getEpisodesFromItem(pageUrl)

    private fun getEpisodesFromSearch(searchUrl: String): List<Episode> {
        val query = URLDecoder.decode(searchUrl.substringAfter("query=").substringBefore("&"), "UTF-8")
        val encoded = URLEncoder.encode("$query AND mediatype:movies", "UTF-8")
        val apiUrl = "https://archive.org/advancedsearch.php?q=$encoded&fl[]=identifier,title&rows=30&sort[]=titleSorter+asc&output=json"
        val docs = runCatching {
            JSONObject(
                Jsoup.connect(apiUrl).userAgent(userAgent).ignoreContentType(true).execute().body()
            ).optJSONObject("response")?.optJSONArray("docs")
        }.getOrNull() ?: return emptyList()

        return (0 until docs.length()).map { i ->
            val doc = docs.getJSONObject(i)
            Episode(
                doc.optString("title", doc.optString("identifier")),
                "https://archive.org/details/${doc.optString("identifier")}",
            )
        }.sortedBy { it.title }
    }

    private fun getEpisodesFromItem(pageUrl: String): List<Episode> {
        val identifier = pageUrl.trimEnd('/').substringAfterLast("/")
        val files = runCatching {
            JSONObject(
                Jsoup.connect("https://archive.org/metadata/$identifier")
                    .userAgent(userAgent).ignoreContentType(true).execute().body()
            ).optJSONArray("files")
        }.getOrNull() ?: return listOf(Episode("Watch", pageUrl))

        val videoExtensions = setOf("mp4", "mkv", "avi", "ogv", "webm")
        val allVideos = (0 until files.length())
            .map { files.getJSONObject(it) }
            .filter { file ->
                file.optString("name").lowercase().substringAfterLast(".", "") in videoExtensions
            }

        if (allVideos.isEmpty()) return listOf(Episode("Watch", pageUrl))

        val primaryVideos = allVideos
            .filter { file -> DERIVATIVE_SUFFIXES.none { file.optString("name").lowercase().contains(it) } }
            .ifEmpty { allVideos }

        return primaryVideos
            .sortedWith(compareBy(
                { if (it.optString("name").lowercase().endsWith(".mp4")) 0 else 1 },
                { it.optString("name") },
            ))
            .distinctBy { it.optString("name").substringBeforeLast(".").lowercase() }
            .sortedBy { it.optString("name") }
            .map { file ->
                val name = file.optString("name")
                Episode(
                    name.substringBeforeLast(".").replace("_", " ").replace(".", " ").trim(),
                    "https://archive.org/download/$identifier/$name",
                )
            }
    }

    companion object {
        private val DERIVATIVE_SUFFIXES = listOf("_512kb", "_128kb", "_256kb", "_64kb", "_h264", "_hq", "_lq")
    }
}
