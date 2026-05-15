package com.greenrou.kanata.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
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

private const val TAG = "Kanata:Scraper"

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
                        Log.d(TAG, "DoH resolving $hostname")
                        val ip = resolveViaDoH(hostname)
                        if (ip != null) {
                            Log.d(TAG, "DoH: $hostname → $ip")
                            return listOf(InetAddress.getByName(ip))
                        }
                        Log.w(TAG, "DoH failed for $hostname, falling back to system DNS")
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
                Log.d(TAG, "DoH [$url] response: ${body.take(300)}")
                val json = JSONObject(body)
                val status = json.optInt("Status", -1)
                if (status != 0) {
                    Log.w(TAG, "DoH [$url] status=$status (not NOERROR)")
                    return@runCatching null
                }
                val answers = json.optJSONArray("Answer") ?: return@runCatching null
                for (i in 0 until answers.length()) {
                    val record = answers.getJSONObject(i)
                    if (record.optInt("type") == 1) {
                        val data = record.optString("data")
                        if (data.isNotBlank() && !data.contains(":")) return@runCatching data
                    }
                }
                null
            }.onFailure { Log.w(TAG, "DoH [$url] exception: ${it.javaClass.simpleName}: ${it.message}") }.getOrNull()
            if (ip != null) return ip
        }
        return null
    }

    override suspend fun getVideoStream(siteUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (isKodikUrl(siteUrl)) {
                val params = parseQueryParams(siteUrl)
                val yref = params["yref"] ?: siteUrl
                val cleanUrl = siteUrl.substringBefore("?")
                Log.d(TAG, "Direct kodik URL: $cleanUrl (ref=$yref)")
                return@runCatching extractKodikStream(yref, cleanUrl)
            }

            val cleanPageUrl = siteUrl.substringBefore("?")
            Log.d(TAG, "Fetching episode page: $cleanPageUrl")
            val document = Jsoup.connect(cleanPageUrl).userAgent(userAgent).get()

            val standardIframe = document
                .select("iframe[src*=/serial/], iframe[src*=/video/], iframe[src*=kodik]")
                .firstOrNull()?.attr("src")
            if (standardIframe != null) {
                val resolved = resolveUrl(siteUrl, standardIframe)
                Log.d(TAG, "Found standard iframe → $resolved")
                return@runCatching tryExtractStream(siteUrl, resolved)
            }

            val litespeedIframe = document.select("iframe[data-litespeed-src]").firstOrNull()
            if (litespeedIframe != null) {
                val iframeUrl = litespeedIframe.attr("data-litespeed-src")
                Log.d(TAG, "Found LiteSpeed iframe → $iframeUrl")
                return@runCatching tryExtractStream(siteUrl, iframeUrl)
            }

            val videoSrc = document.select("video source, video").firstOrNull()?.attr("src")
            if (videoSrc != null) {
                Log.d(TAG, "Found inline <video> → $videoSrc")
                return@runCatching videoSrc
            }

            val xfplayer = document
                .select(".tabs-block__content:not(.d-none):not(.hidden) .xfplayer[data-params]")
                .firstOrNull() ?: error("No video player found on page")

            val dataParams = xfplayer.attr("data-params")
            val base = URL(siteUrl)
            val ajaxUrl = "${base.protocol}://${base.host}/engine/ajax/controller.php?$dataParams"
            Log.d(TAG, "Found xfplayer, AJAX → $ajaxUrl")

            val json = Jsoup.connect(ajaxUrl)
                .userAgent(userAgent)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", siteUrl)
                .ignoreContentType(true)
                .execute().body()

            val playerUrl = JSONObject(json).optString("data")
            if (playerUrl.isBlank()) error("Player returned empty URL")
            Log.d(TAG, "xfplayer data URL → $playerUrl")

            tryExtractStream(siteUrl, playerUrl)
        }.onFailure {
            Log.e(TAG, "getVideoStream failed for $siteUrl", it)
        }
    }

    private fun tryExtractStream(referer: String, playerUrl: String): String {
        Log.d(TAG, "Decoder chain start: $playerUrl")

        if (isKodikUrl(playerUrl)) {
            runCatching { extractKodikStream(referer, playerUrl) }
                .onSuccess { return it }
                .onFailure { Log.w(TAG, "Decoder[kodik] failed: ${it.message}") }
        }

        val playerDoc = runCatching {
            Jsoup.connect(playerUrl)
                .userAgent(userAgent)
                .header("Referer", referer)
                .get()
        }.getOrElse {
            error("Failed to fetch player page $playerUrl: ${it.message}")
        }

        val pageDecoders: List<Pair<String, (Document) -> String?>> = listOf(
            "data-config" to ::decodeFromDataConfig,
            "page-regex" to ::decodeFromPageRegex,
            "nested-iframe" to { doc -> decodeFromNestedIframe(referer, playerUrl, doc) },
        )

        val errors = mutableListOf<String>()
        for ((name, decoder) in pageDecoders) {
            runCatching { decoder(playerDoc) }
                .onSuccess { result -> if (result != null) return result }
                .onFailure { errors += "$name: ${it.message}"; Log.w(TAG, "Decoder[$name] failed: ${it.message}") }
        }

        Log.e(TAG, "All decoders failed for $playerUrl. Page snippet:\n${playerDoc.html().take(1500)}")
        error("No playable stream found on $playerUrl. Errors: ${errors.joinToString("; ")}")
    }

    private fun decodeFromDataConfig(doc: Document): String? {
        val dataConfig = doc.select("[data-config]").firstOrNull()?.attr("data-config") ?: return null
        val hls = JSONObject(dataConfig).optString("hls")
        return if (hls.isNotBlank()) { Log.d(TAG, "Decoder[data-config] → $hls"); hls } else null
    }

    private fun decodeFromPageRegex(doc: Document): String? =
        Regex("""https?://[^\s"']+\.(m3u8|mp4)[^\s"']*""")
            .find(doc.html())?.value
            ?.also { Log.d(TAG, "Decoder[page-regex] → $it") }

    private fun decodeFromNestedIframe(referer: String, playerUrl: String, doc: Document): String? {
        val nested = doc.select("iframe[src]").firstOrNull()?.attr("src") ?: return null
        val resolved = resolveUrl(playerUrl, nested)
        Log.d(TAG, "Decoder[nested-iframe] → $resolved")
        return tryExtractStream(playerUrl, resolved)
    }

    private fun isKodikUrl(url: String): Boolean {
        val host = runCatching { URL(url).host }.getOrElse { return false }
        return host.contains("kodik") || host.contains("kodikplayer")
    }

    private fun extractKodikStream(referer: String, playerUrl: String): String {
        val match = Regex("""/(serial|seria|video|film)/(\d+)/([a-f0-9]+)/\d+p""")
            .find(playerUrl) ?: error("Cannot parse Kodik URL: $playerUrl")
        val (typeFromUrl, idFromUrl, hashFromUrl) = match.destructured
        Log.d(TAG, "Kodik URL: type=$typeFromUrl id=$idFromUrl hash=$hashFromUrl")

        val playerPage = Jsoup.connect(playerUrl)
            .userAgent(userAgent)
            .header("Referer", referer)
            .get()
        val pageHtml = playerPage.html()

        Log.d(TAG, "Kodik page 0-4000:\n${pageHtml.take(4000)}")
        Log.d(TAG, "Kodik page 4000-8000:\n${pageHtml.substring(minOf(4000, pageHtml.length), minOf(8000, pageHtml.length))}")

        Regex("""https?://[^\s"']+\.(m3u8|mp4)[^\s"']*""").find(pageHtml)?.value?.let { url ->
            Log.d(TAG, "Kodik: found stream directly → $url")
            return url
        }

        val urlParamsRaw = Regex("""var\s+urlParams\s*=\s*'(\{[^']+\})'""")
            .find(pageHtml)?.groupValues?.get(1)
        val urlParams = urlParamsRaw?.let { runCatching { JSONObject(it) }.getOrNull() }

        val vInfoType = Regex("""vInfo\.type\s*=\s*'([^']+)'""").find(pageHtml)?.groupValues?.get(1)
        val vInfoId = Regex("""vInfo\.id\s*=\s*'([^']+)'""").find(pageHtml)?.groupValues?.get(1)
        val vInfoHash = Regex("""vInfo\.hash\s*=\s*'([^']+)'""").find(pageHtml)?.groupValues?.get(1)
        Log.d(TAG, "Kodik vInfo: type=$vInfoType id=$vInfoId hash=$vInfoHash")

        val type = vInfoType ?: typeFromUrl
        val id = vInfoId ?: idFromUrl
        val hash = vInfoHash ?: hashFromUrl
        Log.d(TAG, "Kodik using: type=$type id=$id hash=$hash")

        val urlQueryParams = parseQueryParams(referer)
        val season = urlQueryParams["kodikSeason"]
            ?: Regex("""var\s+(?:current)?[Ss]eason\s*=\s*(\d+)""").find(pageHtml)?.groupValues?.get(1)
            ?: "1"
        val episode = urlQueryParams["kodikEpisode"]
            ?: Regex("""var\s+(?:current)?[Ee]pisode\s*=\s*(\d+)""").find(pageHtml)?.groupValues?.get(1)
            ?: "1"
        Log.d(TAG, "Kodik season=$season episode=$episode")

        val jsScriptSrc = playerPage.select("script[src*=app.player_single], script[src*=app.serial]")
            .attr("src").takeIf { it.isNotBlank() }
        val jsContent = jsScriptSrc?.let { src ->
            runCatching {
                Jsoup.connect(resolveUrl(playerUrl, src))
                    .userAgent(userAgent).header("Referer", playerUrl)
                    .ignoreContentType(true).execute().body()
            }.onFailure { Log.e(TAG, "Failed to fetch Kodik JS: ${it.message}") }.getOrNull()
        }
        if (jsContent != null) {
            Log.d(TAG, "Kodik JS size=${jsContent.length}")
            val urlsInJs = Regex("""["']((?:https?:)?//[^"']{5,100})["']""")
                .findAll(jsContent).map { it.groupValues[1] }.distinct().toList()
            Log.d(TAG, "Kodik JS all URLs:\n${urlsInJs.joinToString("\n")}")
            val ajaxPatterns = Regex("""(?:ajax|post|fetch|xhr)\s*\(\s*["'`]([^"'`]+)["'`]""", RegexOption.IGNORE_CASE)
                .findAll(jsContent).map { it.groupValues[1] }.distinct().toList()
            Log.d(TAG, "Kodik JS ajax patterns: $ajaxPatterns")

            val gviIndex = jsContent.indexOf("gvi")
            if (gviIndex >= 0) {
                Log.d(TAG, "Kodik JS 'gvi' context: ${jsContent.substring(maxOf(0, gviIndex - 100), minOf(jsContent.length, gviIndex + 200))}")
            } else {
                Log.d(TAG, "Kodik JS: 'gvi' not found")
            }

            val kodikresIndex = jsContent.indexOf("kodikres")
            if (kodikresIndex >= 0) {
                Log.d(TAG, "Kodik JS 'kodikres' context: ${jsContent.substring(maxOf(0, kodikresIndex - 50), minOf(jsContent.length, kodikresIndex + 300))}")
            }

            val pathsInJs = Regex("""'(/[a-z][^']{1,40})'""")
                .findAll(jsContent).map { it.groupValues[1] }.filter { !it.contains("svg") && !it.contains("html") }.distinct().toList()
            Log.d(TAG, "Kodik JS paths in JS: $pathsInJs")

            val xmlhrPattern = Regex("""XMLHttpRequest|\.open\s*\(|sendBeacon|\.post\s*\(|axios""", RegexOption.IGNORE_CASE)
            val xmlhrIndex = jsContent.indexOfFirst { xmlhrPattern.containsMatchIn(it.toString()) }.let {
                xmlhrPattern.find(jsContent)?.range?.first ?: -1
            }
            if (xmlhrIndex >= 0) {
                Log.d(TAG, "Kodik JS XHR context: ${jsContent.substring(maxOf(0, xmlhrIndex - 50), minOf(jsContent.length, xmlhrIndex + 400))}")
            } else {
                Log.d(TAG, "Kodik JS: no XHR/fetch patterns found")
            }
        }

        Log.d(TAG, "Kodik page 8000-14000:\n${pageHtml.substring(minOf(8000, pageHtml.length), minOf(14000, pageHtml.length))}")

        if (urlParams != null) {
            val paramsUrl = buildString {
                append(playerUrl)
                append("?")
                for (key in listOf("d", "d_sign", "pd", "pd_sign", "ref", "ref_sign")) {
                    val raw = urlParams.optString(key)
                    if (raw.isNotBlank()) {
                        append("&$key=${java.net.URLEncoder.encode(raw, "UTF-8")}")
                    }
                }
            }.replace("?&", "?")
            Log.d(TAG, "Trying URL with signed params: $paramsUrl")
            runCatching {
                val resp = Jsoup.connect(paramsUrl).userAgent(userAgent).header("Referer", referer).get()
                val html = resp.html()
                Log.d(TAG, "Kodik paramURL response (3000):\n${html.take(3000)}")
                Regex("""https?://[^\s"']+\.(m3u8|mp4)[^\s"']*""").find(html)?.value?.let { url ->
                    Log.d(TAG, "Found stream in paramURL → $url")
                    return url
                }
            }.onFailure { Log.w(TAG, "paramURL failed: ${it.message}") }
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
        Log.d(TAG, "Kodik response: $responseBody")

        val streamUrl = parseKodikResponse(responseBody)
            ?: error("Kodik: no playable URL in response: $responseBody")

        val fullUrl = if (streamUrl.startsWith("//")) "https:$streamUrl" else streamUrl
        Log.d(TAG, "Kodik stream: $fullUrl")
        return fullUrl
    }

    private fun parseKodikResponse(body: String): String? {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null

        val links = json.optJSONObject("links")
        if (links != null) {
            val preferredQualities = listOf("720", "1080", "480", "360")
            val allKeys = preferredQualities + buildList {
                val names = links.names() ?: return@buildList
                for (i in 0 until names.length()) add(names.getString(i))
            }
            for (key in allKeys.distinct()) {
                val sources = links.optJSONArray(key) ?: continue
                for (i in 0 until sources.length()) {
                    val src = sources.optJSONObject(i)?.optString("src") ?: continue
                    if (src.isNotBlank()) {
                        val decoded = decodeKodikFtorSrc(src)
                        if (decoded.contains("//")) {
                            Log.d(TAG, "Kodik links[$key][$i] → $decoded")
                            return decoded
                        }
                    }
                }
            }
        }

        val link = json.optString("link")
        if (link.isNotBlank()) {
            Log.d(TAG, "Kodik link → $link")
            return link
        }

        val src = json.optString("src")
        if (src.isNotBlank()) {
            Log.d(TAG, "Kodik src (legacy) → $src")
            return decodeKodikSrc(src)
        }

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
            Log.d(TAG, "Trying gvi: $gviUrl")
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
                Log.d(TAG, "  → status=${response.code} body=${responseBody?.take(200)}")
                if (response.isSuccessful && responseBody?.trimStart()?.startsWith("{") == true) {
                    responseBody
                } else {
                    errors += "$gviUrl: HTTP ${response.code}"
                    null
                }
            }.onFailure { e ->
                Log.w(TAG, "  → ${e.javaClass.simpleName}: ${e.message}")
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
            Log.w(TAG, "Kodik decode failed, using raw src: $src")
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
