package com.greenrou.kanata.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.greenrou.kanata.domain.repository.VideoRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Dns
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
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
            if (isDirectStreamUrl(siteUrl)) return@runCatching siteUrl

            if (isYouTubeUrl(siteUrl)) return@runCatching extractYouTubeStream(siteUrl)

            if (isHanimeUrl(siteUrl)) return@runCatching extractHanimeStream(siteUrl)

            if (isArchiveOrgDetailsUrl(siteUrl)) return@runCatching extractArchiveOrgStream(siteUrl)

            if (isKodikUrl(siteUrl)) {
                val params = parseQueryParams(siteUrl)
                val yref = params["yref"] ?: siteUrl
                return@runCatching extractKodikStream(yref, siteUrl.substringBefore("?"))
            }

            val document = Jsoup.connect(siteUrl.substringBefore("?")).userAgent(userAgent).get()

            val standardIframe = document
                .select("iframe[src*=/serial/], iframe[src*=/video/], iframe[src*=kodik]")
                .firstOrNull()?.attr("src")
            if (standardIframe != null)
                return@runCatching tryExtractStream(siteUrl, resolveUrl(siteUrl, standardIframe))

            val litespeedIframe = document.select("iframe[data-litespeed-src]").firstOrNull()
            if (litespeedIframe != null)
                return@runCatching tryExtractStream(siteUrl, litespeedIframe.attr("data-litespeed-src"))

            val videoSrc = document.select("video source, video").firstOrNull()?.attr("src")
            if (videoSrc != null) return@runCatching videoSrc

            val inlineStream = Regex("""https?://[^\s"']+\.(m3u8|mp4)[^\s"']*""").find(document.html())?.value
            if (inlineStream != null) return@runCatching inlineStream

            val genericIframe = document.select("iframe[src^=http]").firstOrNull()?.attr("src")
            if (genericIframe != null)
                return@runCatching tryExtractStream(siteUrl, genericIframe)

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
        if ("dramavideo.se" in playerUrl) return extractDramaVideoStream(referer, playerUrl)

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

    private fun isYouTubeUrl(url: String): Boolean {
        val host = runCatching { URL(url).host }.getOrElse { return false }
        return "youtube" in host || "youtu.be" in host
    }

    private fun extractYouTubeStream(videoUrl: String): String {
        val info = runCatching { StreamInfo.getInfo(ServiceList.YouTube, videoUrl) }
            .getOrThrow()

        val best: VideoStream? =
            info.videoStreams
                .filter { it.content?.isNotBlank() == true }
                .maxByOrNull { parseYouTubeResolution(it.resolution) }
                ?: info.videoOnlyStreams
                    .filter { it.content?.isNotBlank() == true }
                    .maxByOrNull { parseYouTubeResolution(it.resolution) }

        return best?.content ?: error("No video streams found for: $videoUrl")
    }

    private fun parseYouTubeResolution(resolution: String?): Int =
        resolution?.filter { it.isDigit() }?.toIntOrNull() ?: 0

    private fun isHanimeUrl(url: String) =
        runCatching { URL(url).host }.getOrDefault("").let { "hanime.tv" in it }

    private fun isArchiveOrgDetailsUrl(url: String) = "archive.org/details/" in url

    private fun extractArchiveOrgStream(detailsUrl: String): String {
        val identifier = detailsUrl.trimEnd('/').substringAfterLast("/")

        val response = Jsoup.connect("https://archive.org/metadata/$identifier")
            .userAgent(userAgent)
            .ignoreContentType(true)
            .execute()

        val files = JSONObject(response.body()).optJSONArray("files")
            ?: error("No files in Archive.org metadata for $identifier")

        for (i in 0 until files.length()) {
            val file = files.getJSONObject(i)
            val name = file.optString("name")
            val nameLower = name.lowercase()
            if (nameLower.endsWith(".mp4") && ARCHIVE_DERIVATIVE_SUFFIXES.none { nameLower.contains(it) }) {
                val streamUrl = "https://archive.org/download/$identifier/$name"
                return streamUrl
            }
        }
        for (i in 0 until files.length()) {
            val name = files.getJSONObject(i).optString("name")
            if (name.lowercase().endsWith(".mp4")) {
                val streamUrl = "https://archive.org/download/$identifier/$name"
                return streamUrl
            }
        }
        error("No MP4 found in Archive.org item $identifier")
    }

    private suspend fun extractHanimeStream(pageUrl: String): String {
        val cleanUrl = pageUrl.substringBefore("?")
        val slug = cleanUrl.trimEnd('/').substringAfterLast("/")
        val hid = parseQueryParams(pageUrl)["hid"]

        val apiCandidates = buildList {
            if (hid != null) {
                add("https://cached.freeanimehentai.net/api/v8/hentai-video?id=$hid")
                add("https://hanime.tv/api/v8/hentai-video?id=$hid")
            }
            add("https://cached.freeanimehentai.net/api/v8/hentai-video?slug=$slug")
            add("https://hanime.tv/api/v8/hentai-video?slug=$slug")
        }
        for (apiUrl in apiCandidates) {
            runCatching {
                val r = Jsoup.connect(apiUrl)
                    .userAgent(userAgent)
                    .header("Referer", "https://hanime.tv/")
                    .ignoreContentType(true).execute()
                if (r.statusCode() == 200) {
                    hanimeManifestStream(JSONObject(r.body()), slug)?.let { return it }
                }
            }
        }

        runCatching {
            val doc = Jsoup.connect(cleanUrl).userAgent(userAgent).get()
            val nextScript = doc.getElementById("__NEXT_DATA__")
            if (nextScript != null) {
                val pageProps = JSONObject(nextScript.data())
                    .optJSONObject("props")?.optJSONObject("pageProps")
                val videoObj = pageProps?.optJSONObject("video") ?: pageProps?.optJSONObject("hentai_video")
                if (videoObj != null) {
                    hanimeManifestStream(videoObj, slug)?.let { return it }
                }
            }
            Regex("""https?://[^\s"'\\]+\.(m3u8|mp4)[^\s"'\\]*""").find(doc.html())?.value
                ?.let { return it }
        }

        return extractHanimeStreamViaWebView(cleanUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun extractHanimeStreamViaWebView(pageUrl: String): String {
        val deferred = CompletableDeferred<String>()
        var webViewRef: WebView? = null

        withContext(Dispatchers.Main) {
            val webView = WebView(context)
            webViewRef = webView
            with(webView.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            val slug = pageUrl.trimEnd('/').substringAfterLast("/")
            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    view.evaluateJavascript(
                        """
                        Object.defineProperty(navigator,'webdriver',{get:()=>undefined});
                        Object.defineProperty(navigator,'plugins',{get:()=>({length:5})});
                        Object.defineProperty(navigator,'languages',{get:()=>['en-US','en']});
                        """.trimIndent(), null
                    )
                }

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    val url = request.url.toString()
                    if (deferred.isCompleted) return null

                    if ("freeanimehentai.net" in url && "hentai-video" in url) {
                        Thread {
                            runCatching {
                                val body = Jsoup.connect(url)
                                    .userAgent(userAgent)
                                    .header("Referer", "https://hanime.tv/")
                                    .ignoreContentType(true).execute().body()
                                hanimeManifestStream(JSONObject(body), slug)?.let {
                                    deferred.complete(it)
                                }
                            }
                        }.start()
                    }

                    val isStream = ".m3u8" in url
                        || (".mp4" in url && ("hanime" in url || "cdn" in url))
                        || ("hanime-cdn.com/videos" in url)
                    if (isStream) {
                        deferred.complete(url)
                    }
                    return null
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (deferred.isCompleted) return

                    val cm = android.webkit.CookieManager.getInstance()
                    val cookieHanime = cm.getCookie("https://hanime.tv") ?: ""
                    val cookieCdn = cm.getCookie("https://cached.freeanimehentai.net") ?: ""
                    val allCookies = listOf(cookieHanime, cookieCdn).filter { it.isNotBlank() }.joinToString("; ")

                    val hid = parseQueryParams(pageUrl)["hid"]
                    val apiUrls = buildList {
                        if (hid != null) add("https://cached.freeanimehentai.net/api/v8/hentai-video?id=$hid")
                        add("https://cached.freeanimehentai.net/api/v8/hentai-video?slug=$slug")
                    }
                    Thread {
                        for (apiUrl in apiUrls) {
                            if (deferred.isCompleted) break
                            runCatching {
                                val body = Jsoup.connect(apiUrl)
                                    .userAgent(userAgent)
                                    .header("Referer", "https://hanime.tv/")
                                    .header("Cookie", allCookies)
                                    .ignoreContentType(true).execute().body()
                                hanimeManifestStream(JSONObject(body), slug)?.let {
                                    deferred.complete(it)
                                }
                            }
                        }

                        if (!deferred.isCompleted) {
                            Thread.sleep(5_000)
                            view.post {
                                view.evaluateJavascript(
                                    """(function(){var v=document.querySelector('video');if(!v)return'no-video';if(v.currentSrc&&v.currentSrc.length>10)return v.currentSrc;return'empty';})()""",
                                ) { result ->
                                    val src = result?.trim('"')?.takeIf { it.length > 10 && it != "no-video" && it != "empty" }
                                    if (src != null && !deferred.isCompleted) {
                                        deferred.complete(src)
                                    }
                                }
                            }
                        }
                    }.start()
                }
            }

            webView.webChromeClient = android.webkit.WebChromeClient()

            webView.loadUrl(pageUrl)
        }

        val stream = withTimeoutOrNull(90_000) { deferred.await() }

        withContext(Dispatchers.Main) {
            webViewRef?.destroy()
        }

        return stream ?: error("WebView timeout: no stream URL intercepted for $pageUrl")
    }

    private fun hanimeManifestStream(json: JSONObject, slug: String): String? {
        val servers = json.optJSONObject("videos_manifest")?.optJSONArray("servers")
        if (servers == null) {
            return null
        }
        var bestUrl = ""; var bestHeight = 0
        for (i in 0 until servers.length()) {
            val streams = servers.getJSONObject(i).optJSONArray("streams") ?: continue
            for (j in 0 until streams.length()) {
                val s = streams.getJSONObject(j)
                val url = s.optString("url"); val h = s.optInt("height", 0)
                if (url.isNotBlank() && h > bestHeight) { bestHeight = h; bestUrl = url }
            }
        }
        return bestUrl.ifBlank { null }
    }

    private fun isDirectStreamUrl(url: String): Boolean {
        val path = url.substringBefore("?").substringBefore("#").lowercase()
        return path.endsWith(".m3u8") || path.endsWith(".mp4") || path.endsWith(".mpd") || path.endsWith(".mkv")
    }

    private fun extractDramaVideoStream(referer: String, pageUrl: String): String {
        val doc = Jsoup.connect(pageUrl)
            .userAgent(userAgent)
            .header("Referer", referer)
            .get()

        val server = doc.select("li.linkserver[data-video][data-provider]").firstOrNull()
            ?: error("dramavideo.se: no server element found on $pageUrl")

        val code = server.attr("data-video")
        val sv = server.attr("data-provider")
        val playerUrl = "https://player.dramavideo.se/?id=${java.net.URLEncoder.encode(code, "UTF-8")}&sv=${java.net.URLEncoder.encode(sv, "UTF-8")}"

        val playerDoc = Jsoup.connect(playerUrl)
            .userAgent(userAgent)
            .header("Referer", pageUrl)
            .header("Origin", "https://dramavideo.se")
            .get()

        val html = playerDoc.html()
        Regex("""https?://[^\s"']+\.m3u8[^\s"']*""").find(html)?.value?.let { return it }
        Regex("""https?://[^\s"']+\.mp4[^\s"']*""").find(html)?.value?.let { return it }

        val nestedIframe = playerDoc.select("iframe[src^=http]").firstOrNull()?.attr("src")
        if (nestedIframe != null) return tryExtractStream(playerUrl, nestedIframe)

        error("dramavideo.se: no stream found in player page")
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

    companion object {
        private val ARCHIVE_DERIVATIVE_SUFFIXES = listOf("_512kb", "_128kb", "_256kb", "_64kb", "_h264", "_hq", "_lq")
    }
}
