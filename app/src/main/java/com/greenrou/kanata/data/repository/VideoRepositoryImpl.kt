package com.greenrou.kanata.data.repository

import android.content.Context
import android.util.Base64
import com.greenrou.kanata.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.InetAddress
import java.net.URL
import java.net.URLDecoder

private val KODIK_GVI_CANDIDATES = listOf(
    "https://kodikplayer.com/ftor",
    "https://kodikres.com/ftor",
    "https://kodikres.com/gvi",
    "https://kodik.info/gvi",
    "https://kodik.cc/gvi",
    "https://kodik.biz/gvi",
    "https://kodik.pm/gvi",
)

class VideoRepositoryImpl(
    private val context: Context
) : VideoRepository {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    private val kodikClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    if (isKodikApiHost(hostname)) {
                        val ip = resolveViaDoH(hostname)
                        if (ip != null) return listOf(InetAddress.getByName(ip))
                    }
                    return Dns.SYSTEM.lookup(hostname)
                }
            })
            .build()
    }

    private fun isKodikApiHost(hostname: String) =
        hostname == "kodik.info" || hostname == "kodik.cc" ||
        hostname == "kodik.biz" || hostname == "kodik.pm" ||
        hostname == "kodikres.com" || hostname.endsWith(".kodikres.com")

    private fun resolveViaDoH(hostname: String): String? {
        val dohEndpoints = listOf(
            "https://1.1.1.1/dns-query?name=$hostname&type=A",
            "https://cloudflare-dns.com/dns-query?name=$hostname&type=A",
            "https://dns.google/resolve?name=$hostname&type=A",
        )
        for (url in dohEndpoints) {
            val ip = runCatching {
                val body = Jsoup.connect(url)
                    .header("Accept", "application/dns-json")
                    .ignoreContentType(true)
                    .timeout(5000)
                    .execute().body()
                val json = JSONObject(body)
                if (json.optInt("Status", -1) != 0) return@runCatching null
                val answers = json.optJSONArray("Answer") ?: return@runCatching null
                for (i in 0 until answers.length()) {
                    val record = answers.getJSONObject(i)
                    if (record.optInt("type") == 1) {
                        val data = record.optString("data")
                        if (data.isNotBlank() && !data.contains(":")) return@runCatching data
                    }
                }
                null
            }.getOrNull()
            if (ip != null) return ip
        }
        return null
    }

    override suspend fun getVideoStream(siteUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (isKodikUrl(siteUrl)) {
                val params = parseQueryParams(siteUrl)
                val yref = params["yref"] ?: siteUrl
                return@runCatching extractKodikStream(yref, siteUrl.substringBefore("?"))
            }

            val cleanPageUrl = siteUrl.substringBefore("?")
            val document = Jsoup.connect(cleanPageUrl).userAgent(userAgent).get()

            val standardIframe = document
                .select("iframe[src*=/serial/], iframe[src*=/video/], iframe[src*=kodik]")
                .firstOrNull()?.attr("src")
            if (standardIframe != null) {
                return@runCatching tryExtractStream(siteUrl, resolveUrl(siteUrl, standardIframe))
            }

            val litespeedIframe = document.select("iframe[data-litespeed-src]").firstOrNull()
            if (litespeedIframe != null) {
                return@runCatching tryExtractStream(siteUrl, litespeedIframe.attr("data-litespeed-src"))
            }

            val videoSrc = document.select("video source, video").firstOrNull()?.attr("src")
            if (videoSrc != null) return@runCatching videoSrc

            val xfplayer = document
                .select(".tabs-block__content:not(.d-none):not(.hidden) .xfplayer[data-params]")
                .firstOrNull() ?: error("No video player found on page")

            val base = URL(siteUrl)
            val ajaxUrl = "${base.protocol}://${base.host}/engine/ajax/controller.php?${xfplayer.attr("data-params")}"

            val json = Jsoup.connect(ajaxUrl)
                .userAgent(userAgent)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", siteUrl)
                .ignoreContentType(true)
                .execute().body()

            val playerUrl = JSONObject(json).optString("data")
            if (playerUrl.isBlank()) error("Player returned empty URL")

            tryExtractStream(siteUrl, playerUrl)
        }
    }

    private fun tryExtractStream(referer: String, playerUrl: String): String {
        if (isKodikUrl(playerUrl)) {
            runCatching { extractKodikStream(referer, playerUrl) }
                .onSuccess { return it }
        }

        val playerDoc = runCatching {
            Jsoup.connect(playerUrl)
                .userAgent(userAgent)
                .header("Referer", referer)
                .get()
        }.getOrElse {
            error("Failed to fetch player page $playerUrl: ${it.message}")
        }

        val pageDecoders: List<(Document) -> String?> = listOf(
            ::decodeFromDataConfig,
            ::decodeFromPageRegex,
            { doc -> decodeFromNestedIframe(referer, playerUrl, doc) },
        )

        val errors = mutableListOf<String>()
        for (decoder in pageDecoders) {
            runCatching { decoder(playerDoc) }
                .onSuccess { result -> if (result != null) return result }
                .onFailure { errors += it.message ?: "?" }
        }

        error("No playable stream found on $playerUrl. Errors: ${errors.joinToString("; ")}")
    }

    private fun decodeFromDataConfig(doc: Document): String? {
        val dataConfig = doc.select("[data-config]").firstOrNull()?.attr("data-config") ?: return null
        val hls = JSONObject(dataConfig).optString("hls")
        return if (hls.isNotBlank()) hls else null
    }

    private fun decodeFromPageRegex(doc: Document): String? =
        Regex("""https?://[^\s"']+\.(m3u8|mp4)[^\s"']*""").find(doc.html())?.value

    private fun decodeFromNestedIframe(referer: String, playerUrl: String, doc: Document): String? {
        val nested = doc.select("iframe[src]").firstOrNull()?.attr("src") ?: return null
        return tryExtractStream(playerUrl, resolveUrl(playerUrl, nested))
    }

    private fun isKodikUrl(url: String): Boolean {
        val host = runCatching { URL(url).host }.getOrElse { return false }
        return host.contains("kodik") || host.contains("kodikplayer")
    }

    private fun extractKodikStream(referer: String, playerUrl: String): String {
        val match = Regex("""/(serial|seria|video|film)/(\d+)/([a-f0-9]+)/\d+p""")
            .find(playerUrl) ?: error("Cannot parse Kodik URL: $playerUrl")
        val (typeFromUrl, idFromUrl, hashFromUrl) = match.destructured

        val playerPage = Jsoup.connect(playerUrl)
            .userAgent(userAgent)
            .header("Referer", referer)
            .get()
        val pageHtml = playerPage.html()

        Regex("""https?://[^\s"']+\.(m3u8|mp4)[^\s"']*""").find(pageHtml)?.value?.let { return it }

        val urlParamsRaw = Regex("""var\s+urlParams\s*=\s*'(\{[^']+\})'""")
            .find(pageHtml)?.groupValues?.get(1)
        val urlParams = urlParamsRaw?.let { runCatching { JSONObject(it) }.getOrNull() }

        val type = Regex("""vInfo\.type\s*=\s*'([^']+)'""").find(pageHtml)?.groupValues?.get(1) ?: typeFromUrl
        val id   = Regex("""vInfo\.id\s*=\s*'([^']+)'""").find(pageHtml)?.groupValues?.get(1) ?: idFromUrl
        val hash = Regex("""vInfo\.hash\s*=\s*'([^']+)'""").find(pageHtml)?.groupValues?.get(1) ?: hashFromUrl

        val urlQueryParams = parseQueryParams(referer)
        val season = urlQueryParams["kodikSeason"]
            ?: Regex("""var\s+(?:current)?[Ss]eason\s*=\s*(\d+)""").find(pageHtml)?.groupValues?.get(1)
            ?: "1"
        val episode = urlQueryParams["kodikEpisode"]
            ?: Regex("""var\s+(?:current)?[Ee]pisode\s*=\s*(\d+)""").find(pageHtml)?.groupValues?.get(1)
            ?: "1"

        if (urlParams != null) {
            val paramsUrl = buildString {
                append(playerUrl)
                append("?")
                for (key in listOf("d", "d_sign", "pd", "pd_sign", "ref", "ref_sign")) {
                    val raw = urlParams.optString(key)
                    if (raw.isNotBlank()) append("&$key=${java.net.URLEncoder.encode(raw, "UTF-8")}")
                }
            }.replace("?&", "?")
            runCatching {
                val html = Jsoup.connect(paramsUrl).userAgent(userAgent).header("Referer", referer).get().html()
                Regex("""https?://[^\s"']+\.(m3u8|mp4)[^\s"']*""").find(html)?.value?.let { return it }
            }
        }

        val isSingleEpisode = type == "seria" || type == "video" || type == "film"
        val postParams = mutableMapOf(
            "type" to type,
            "id" to id,
            "hash" to hash,
            "bad_user" to "false",
            "cdn_is_working" to "true",
        )
        if (!isSingleEpisode) {
            postParams["season"] = season
            postParams["episode"] = episode
        }

        if (urlParams != null) {
            for (key in listOf("d", "d_sign", "pd", "pd_sign", "ref", "ref_sign")) {
                val raw = urlParams.optString(key)
                if (raw.isNotBlank()) {
                    postParams[key] = runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw)
                }
            }
        }

        val responseBody = tryKodikGvi(KODIK_GVI_CANDIDATES, postParams, playerUrl)
        val streamUrl = parseKodikResponse(responseBody)
            ?: error("Kodik: no playable URL in response: $responseBody")

        return if (streamUrl.startsWith("//")) "https:$streamUrl" else streamUrl
    }

    private fun parseKodikResponse(body: String): String? {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null

        val links = json.optJSONObject("links")
        if (links != null) {
            val allKeys = buildList {
                addAll(listOf("720", "1080", "480", "360"))
                val names = links.names() ?: return@buildList
                for (i in 0 until names.length()) add(names.getString(i))
            }.distinct()
            for (key in allKeys) {
                val sources = links.optJSONArray(key) ?: continue
                for (i in 0 until sources.length()) {
                    val src = sources.optJSONObject(i)?.optString("src") ?: continue
                    if (src.isNotBlank()) {
                        val decoded = decodeKodikFtorSrc(src)
                        if (decoded.contains("//")) return decoded
                    }
                }
            }
        }

        val link = json.optString("link")
        if (link.isNotBlank()) return link

        val src = json.optString("src")
        if (src.isNotBlank()) return decodeKodikSrc(src)

        return null
    }

    private fun decodeKodikFtorSrc(src: String): String {
        if (src.contains("//")) return src
        return try {
            val rot18 = src.map { c ->
                when {
                    c.isUpperCase() -> {
                        val shifted = c.code + 18
                        if (shifted <= 90) shifted.toChar() else (shifted - 26).toChar()
                    }
                    c.isLowerCase() -> {
                        val shifted = c.code + 18
                        if (shifted <= 122) shifted.toChar() else (shifted - 26).toChar()
                    }
                    else -> c
                }
            }.joinToString("")
            String(Base64.decode(rot18, Base64.DEFAULT))
        } catch (e: Exception) {
            src
        }
    }

    private fun tryKodikGvi(
        candidates: List<String>,
        params: Map<String, String>,
        referer: String,
    ): String {
        val errors = mutableListOf<String>()
        for (gviUrl in candidates) {
            val body = runCatching {
                val formBody = FormBody.Builder().apply {
                    params.forEach { (k, v) -> add(k, v) }
                }.build()
                val request = Request.Builder()
                    .url(gviUrl)
                    .post(formBody)
                    .header("User-Agent", userAgent)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", referer)
                    .build()
                val response = kodikClient.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody?.trimStart()?.startsWith("{") == true) {
                    responseBody
                } else {
                    errors += "$gviUrl: HTTP ${response.code}"
                    null
                }
            }.onFailure { e ->
                errors += "$gviUrl: ${e.javaClass.simpleName}"
            }.getOrNull()

            if (body != null) return body
        }
        error("All kodik gvi endpoints failed:\n${errors.joinToString("\n")}")
    }

    private fun decodeKodikSrc(src: String): String {
        return try {
            val prepared = src.reversed().replace("-", "+").replace("_", "/")
            String(Base64.decode(prepared, Base64.DEFAULT))
        } catch (e: Exception) {
            src
        }
    }

    private fun parseQueryParams(url: String): Map<String, String> = runCatching {
        val query = URL(url).query ?: return@runCatching emptyMap()
        query.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            parts[0] to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
        }
    }.getOrDefault(emptyMap())

    private fun resolveUrl(base: String, url: String): String = when {
        url.startsWith("//") -> "https:$url"
        !url.startsWith("http") -> URL(base).run { "$protocol://$host$url" }
        else -> url
    }
}
