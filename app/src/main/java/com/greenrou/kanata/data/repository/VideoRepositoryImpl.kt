package com.greenrou.kanata.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.greenrou.kanata.domain.model.VideoStream
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
import org.schabi.newpipe.extractor.stream.VideoStream as NewPipeVideoStream
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

    private val kisskhClient: OkHttpClient by lazy { OkHttpClient() }

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
            if (ip != null) {
                return ip
            }
        }
        return null
    }

    override suspend fun getVideoStream(siteUrl: String): Result<VideoStream> = withContext(Dispatchers.IO) {
        runCatching {

            if (isDirectStreamUrl(siteUrl)) {
                return@runCatching VideoStream(siteUrl)
            }

            if (isYouTubeUrl(siteUrl)) {
                return@runCatching VideoStream(extractYouTubeStream(siteUrl))
            }

            if (isHanimeUrl(siteUrl)) {
                return@runCatching VideoStream(extractHanimeStream(siteUrl))
            }

            if (isArchiveOrgDetailsUrl(siteUrl)) {
                return@runCatching VideoStream(extractArchiveOrgStream(siteUrl))
            }

            if (isKodikUrl(siteUrl)) {
                val params = parseQueryParams(siteUrl)
                val yref = params["yref"] ?: siteUrl
                return@runCatching VideoStream(extractKodikStream(yref, siteUrl.substringBefore("?")))
            }

            if (isAnimegongoUrl(siteUrl)) {
                return@runCatching extractAnimegongoStream(siteUrl)
            }

            if (isKisskhUrl(siteUrl)) {
                return@runCatching extractKisskhStream(siteUrl)
            }

            val document = Jsoup.connect(siteUrl.substringBefore("?")).userAgent(userAgent).get()

            val standardIframeUrl = document
                .select("iframe[src*=/serial/], iframe[src*=/video/], iframe[src*=kodik]")
                .firstOrNull()?.attr("src")?.let { resolveUrl(siteUrl, it) }
            if (standardIframeUrl != null) {
                return@runCatching VideoStream(tryExtractStream(siteUrl, standardIframeUrl), mapOf("Referer" to standardIframeUrl))
            }

            val litespeedSrc = document.select("iframe[data-litespeed-src]").firstOrNull()?.attr("data-litespeed-src")
            if (litespeedSrc != null) {
                return@runCatching VideoStream(tryExtractStream(siteUrl, litespeedSrc), mapOf("Referer" to litespeedSrc))
            }

            val videoSrc = document.select("video source, video").firstOrNull()?.attr("src")
            if (videoSrc != null) {
                return@runCatching VideoStream(videoSrc, mapOf("Referer" to siteUrl))
            }

            val inlineStream = Regex("""https?://[^\s"']+\.(m3u8|mp4)[^\s"']*""").find(document.html())?.value
            if (inlineStream != null) {
                return@runCatching VideoStream(inlineStream, mapOf("Referer" to siteUrl))
            }

            val astarEpisode = parseQueryParams(siteUrl)["astarEpisode"]
            val genericIframe = document.select("iframe[src]")
                .map { it.attr("src").trim() }
                .filter { src -> src.isNotBlank() && !src.startsWith("about:") && "adblock" !in src && "banner" !in src }
                .map { src -> resolveUrl(siteUrl, src) }
                .firstOrNull()
            if (genericIframe != null) {
                val iframeWithEpisode = if (astarEpisode != null && ("/player" in genericIframe || "videoas" in genericIframe)) {
                    "$genericIframe&episode=$astarEpisode"
                } else {
                    genericIframe
                }
                return@runCatching VideoStream(tryExtractStream(siteUrl, iframeWithEpisode), mapOf("Referer" to iframeWithEpisode))
            }

            val xfplayer = document
                .select(".tabs-block__content:not(.d-none):not(.hidden) .xfplayer[data-params], .xfplayer[data-params]")
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

            VideoStream(tryExtractStream(siteUrl, playerUrl), mapOf("Referer" to playerUrl))
        }.onFailure { e ->
        }
    }

    private fun tryExtractStream(referer: String, playerUrl: String): String {

        if ("dramavideo.se" in playerUrl) {
            return extractDramaVideoStream(referer, playerUrl)
        }

        if (isYouTubeUrl(playerUrl)) {
            val watchUrl = Regex("""youtube\.com/embed/([a-zA-Z0-9_-]+)""")
                .find(playerUrl)
                ?.let { "https://www.youtube.com/watch?v=${it.groupValues[1]}" }
                ?: playerUrl
            return extractYouTubeStream(watchUrl)
        }

        if ("videoas_p2p" in playerUrl || ("videoas" in playerUrl && "astar.bz" in playerUrl)) {
            val episodeNum = parseQueryParams(playerUrl)["episode"] ?: "1"
            return extractAstarEpisodeStream(playerUrl, referer, episodeNum)
        }

        val hasExplicitEpisode = parseQueryParams(referer).containsKey("kodikEpisode")

        if (isKodikUrl(playerUrl)) {
            val result = runCatching { extractKodikStream(referer, playerUrl) }
            result.onSuccess { stream ->
                return stream
            }
            result.onFailure { e ->
                if (hasExplicitEpisode) throw e
            }
        }

        val playerDoc = runCatching {
            Jsoup.connect(playerUrl)
                .userAgent(userAgent)
                .header("Referer", referer)
                .get()
        }.getOrElse {
            error("Failed to fetch player page $playerUrl: ${it.message}")
        }

        val pageDecoders: List<(Document) -> String?> = if (hasExplicitEpisode) {
            listOf(
                { doc -> decodeFromDataConfigForEpisode(referer, doc) },
                { doc -> decodeFromInputData(referer, doc) },
                { doc -> decodeFromNestedIframe(referer, playerUrl, doc) },
                ::decodeFromDataConfig,
            )
        } else {
            listOf(
                ::decodeFromDataConfig,
                ::decodeFromPageRegex,
                { doc -> decodeFromNestedIframe(referer, playerUrl, doc) },
            )
        }

        val errors = mutableListOf<String>()
        for (decoder in pageDecoders) {
            runCatching { decoder(playerDoc) }
                .onSuccess { result ->
                    if (result != null) {
                        return result
                    }
                }
                .onFailure { errors += it.message ?: "?" }
        }

        error("No playable stream found on $playerUrl. Errors: ${errors.joinToString("; ")}")
    }

    private fun decodeFromDataConfig(doc: Document): String? {
        val dataConfig = doc.select("[data-config]").firstOrNull()?.attr("data-config") ?: return null
        val hls = JSONObject(dataConfig).optString("hls")
        return if (hls.isNotBlank()) hls else null
    }

    private fun decodeFromDataConfigForEpisode(referer: String, doc: Document): String? {
        val kodikMatch = Regex("""//(?:kodik|kodikplayer)\.[a-z]+/(serial|seria|video|film)/(\d+)/([0-9a-zA-Z]+)/\d+p""")
            .find(doc.html())
        if (kodikMatch != null) {
            val kodikUrl = "https:${kodikMatch.value}"
            return runCatching { extractKodikStream(referer, kodikUrl) }.getOrNull()
        }

        val dataConfig = doc.select("[data-config]").firstOrNull()?.attr("data-config") ?: return null
        val json = runCatching { JSONObject(dataConfig) }.getOrNull() ?: return null

        val type = json.optString("type").ifBlank { null }
        val id = json.optString("id").ifBlank { null }
        val hash = json.optString("hash").ifBlank { null }
        if (type == null || id == null || hash == null) return null

        val kodikUrl = "https://kodik.info/$type/$id/$hash/720p"
        return runCatching { extractKodikStream(referer, kodikUrl) }.getOrNull()
    }

    private fun decodeFromPageRegex(doc: Document): String? =
        Regex("""https?://[^\s"']+\.(m3u8|mp4)[^\s"']*""").find(doc.html())?.value

    private fun decodeFromNestedIframe(referer: String, playerUrl: String, doc: Document): String? {
        val nested = doc.select("iframe[src]").firstOrNull()?.attr("src") ?: return null
        return tryExtractStream(referer, resolveUrl(playerUrl, nested))
    }

    private fun decodeFromInputData(referer: String, doc: Document): String? {
        val inputData = doc.getElementById("inputData") ?: return null
        val urlQueryParams = parseQueryParams(referer)
        val season = urlQueryParams["kodikSeason"] ?: "1"
        val episode = urlQueryParams["kodikEpisode"] ?: return null

        return runCatching {
            val playlist = JSONObject(inputData.text())
            val seasonObj = playlist.optJSONObject(season) ?: return null
            val epRaw = seasonObj.opt(episode)

            val epUrl: String? = when {
                epRaw is String && (epRaw.startsWith("//") || epRaw.startsWith("http")) -> epRaw
                epRaw is JSONObject -> {
                    val id = epRaw.optString("id")
                    val hash = epRaw.optString("hash")
                    val type = epRaw.optString("type", "seria")
                    if (id.isNotBlank() && hash.isNotBlank()) "//kodik.info/$type/$id/$hash/720p"
                    else null
                }
                epRaw is org.json.JSONArray && epRaw.length() > 0 -> {
                    val videoId = epRaw.optJSONObject(0)?.optLong("video_id", -1L) ?: -1L
                    if (videoId <= 0) return null
                    getOpravarStream(videoId.toString(), doc.baseUri(), referer)
                }
                else -> null
            }

            if (epUrl == null) return null
            val fullUrl = if (epUrl.startsWith("//")) "https:$epUrl" else epUrl
            if (isKodikUrl(fullUrl)) extractKodikStream(referer, fullUrl) else fullUrl
        }.getOrNull()
    }

    private fun getOpravarStream(videoId: String, playerPageUrl: String, referer: String): String? {
        val host = runCatching { URL(playerPageUrl).let { "${it.protocol}://${it.host}" } }
            .getOrElse { "https://gencit.info" }

        val apiUrl = "$host/player/responce.php?video_id=$videoId"
        return runCatching {
            val body = Jsoup.connect(apiUrl)
                .userAgent(userAgent)
                .header("Referer", playerPageUrl)
                .ignoreContentType(true)
                .execute().body()
            val json = JSONObject(body)
            json.optString("hls").ifBlank { null }
                ?: json.optString("src").ifBlank { null }
                ?: Regex("""https?://[^\s"']+\.m3u8[^\s"']*""").find(body)?.value
        }.getOrNull()
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

        val best: NewPipeVideoStream? =
            info.videoStreams
                .filter { it.content?.isNotBlank() == true }
                .maxByOrNull { parseYouTubeResolution(it.resolution) }
                ?: info.videoOnlyStreams
                    .filter { it.content?.isNotBlank() == true }
                    .maxByOrNull { parseYouTubeResolution(it.resolution) }

        val result = best?.content ?: error("No video streams found for: $videoUrl")
        return result
    }

    private fun parseYouTubeResolution(resolution: String?): Int =
        resolution?.filter { it.isDigit() }?.toIntOrNull() ?: 0

    private fun isHanimeUrl(url: String) =
        runCatching { URL(url).host }.getOrDefault("").let { "hanime.tv" in it }

    private fun isArchiveOrgDetailsUrl(url: String) = "archive.org/details/" in url

    private fun isAnimegongoUrl(url: String) =
        runCatching { URL(url).host }.getOrDefault("").let { "animego.ngo" in it }

    private fun extractAnimegongoStream(episodePageUrl: String): VideoStream {
        val doc = Jsoup.connect(episodePageUrl)
            .userAgent(userAgent)
            .header("Referer", "https://animego.ngo/")
            .get()

        val kodikUrl = doc.selectFirst("a#kodik-tab[data-url]")
            ?.attr("data-url")
            ?.let { if (it.startsWith("//")) "https:$it" else it }
            ?: error("No Kodik player tab found on animego.ngo page: $episodePageUrl")

        val stream = extractKodikStream(episodePageUrl, kodikUrl.substringBefore("?"))
        return VideoStream(stream, mapOf("Referer" to kodikUrl))
    }

    private fun isKisskhUrl(url: String) = "kisskh.co" in url && "/episode/" in url

    private suspend fun extractKisskhStream(episodeUrl: String): VideoStream {
        val epId = Regex("""/episode/(\d+)""").find(episodeUrl)?.groupValues?.get(1)
            ?: error("Cannot extract episode ID from KissKH URL: $episodeUrl")

        val kkey = generateKisskhKey(epId.toInt())
        val apiUrl = "https://kisskh.co/api/DramaList/Episode/$epId.png?err=false&ts=&time=&kkey=$kkey"
        val dramaPageUrl = parseQueryParams(episodeUrl)["drama_page"] ?: "https://kisskh.co/"
        Log.d("KissKH", "epId=$epId dramaPageUrl=$dramaPageUrl")

        val directResult = runCatching {
            val resp = kisskhClient.newCall(
                Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Referer", "https://kisskh.co/")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .build()
            ).execute()
            Log.d("KissKH", "direct HTTP ${resp.code}")
            if (resp.code == 200) resp.body?.string() else null
        }.getOrNull()

        if (directResult != null) return parseKisskhVideoStream(directResult, epId)

        Log.d("KissKH", "direct 403, falling back to WebView (dramaPage=$dramaPageUrl)")
        return extractKisskhStreamViaWebView(epId, apiUrl, dramaPageUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun extractKisskhStreamViaWebView(
        epId: String,
        apiUrl: String,
        dramaPageUrl: String,
    ): VideoStream {
        val deferred = CompletableDeferred<String>()
        var webViewRef: WebView? = null

        withContext(Dispatchers.Main) {
            val webView = WebView(context)
            webViewRef = webView
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            webView.addJavascriptInterface(object : Any() {
                @android.webkit.JavascriptInterface
                fun onResult(body: String) {
                    Log.d("KissKH", "JS fetch body=${body.take(300)}")
                    val videoUrl = parseKisskhVideoUrl(body)
                    if (videoUrl != null && !deferred.isCompleted) {
                        Log.d("KissKH", "JS videoUrl=$videoUrl")
                        deferred.complete(videoUrl)
                    }
                }
                @android.webkit.JavascriptInterface
                fun onError(error: String) {
                    Log.e("KissKH", "JS fetch error: $error")
                }
            }, "KissKHBridge")

            val webViewUA = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

            webView.webViewClient = object : WebViewClient() {
                // Intercept Angular's own episode API call: it carries the correct kkey
                // generated by the site's JS plus Cloudflare cookies already in the jar.
                // Return WebResourceResponse so WebView uses our response and makes no extra request.
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    val reqUrl = request.url.toString()
                    if (!deferred.isCompleted && "/api/DramaList/Episode/" in reqUrl) {
                        Log.d("KissKH", "Intercepted Angular API call: $reqUrl")
                        return runCatching {
                            val cookies = android.webkit.CookieManager.getInstance()
                                .getCookie("https://kisskh.co") ?: ""
                            val okResp = kisskhClient.newCall(
                                Request.Builder()
                                    .url(reqUrl)
                                    .header("User-Agent", webViewUA)
                                    .header("Accept", "application/json, text/plain, */*")
                                    .header("Referer", dramaPageUrl)
                                    .header("X-Requested-With", "XMLHttpRequest")
                                    .header("Cookie", cookies)
                                    .build()
                            ).execute()
                            val body = okResp.body?.string() ?: ""
                            Log.d("KissKH", "Intercepted API response: ${body.take(300)}")
                            val videoUrl = parseKisskhVideoUrl(body)
                            if (videoUrl != null && !deferred.isCompleted) {
                                Log.d("KissKH", "Intercepted videoUrl=$videoUrl")
                                deferred.complete(videoUrl)
                            }
                            WebResourceResponse(
                                okResp.header("Content-Type") ?: "application/json",
                                "UTF-8",
                                okResp.code,
                                okResp.message.ifBlank { "OK" },
                                emptyMap(),
                                body.byteInputStream(),
                            )
                        }.onFailure { e ->
                            Log.e("KissKH", "Intercepted API error: ${e.message}")
                        }.getOrNull()
                    }
                    return null
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (deferred.isCompleted || "kisskh.co" !in url) return
                    Log.d("KissKH", "WebView ready at $url — scheduling fallback fetch in 8s")
                    // Fallback: Angular may have cached the response and not re-fetched.
                    // Wait 8s for Cloudflare session to be fully ready, then try manually.
                    view.evaluateJavascript("""
                        (function() {
                            setTimeout(function() {
                                fetch('$apiUrl', {
                                    headers: {
                                        'Accept': 'application/json, text/plain, */*',
                                        'X-Requested-With': 'XMLHttpRequest'
                                    }
                                })
                                .then(function(r) { return r.text(); })
                                .then(function(b) { if (b && b.length > 2) window.KissKHBridge.onResult(b); })
                                .catch(function(e) { window.KissKHBridge.onError(e.toString()); });
                            }, 8000);
                        })();
                    """.trimIndent(), null)
                }
            }

            Log.d("KissKH", "WebView loading drama page: $dramaPageUrl")
            webView.loadUrl(dramaPageUrl)
        }

        val videoUrl = withTimeoutOrNull(60_000) { deferred.await() }
        withContext(Dispatchers.Main) { webViewRef?.destroy() }

        return VideoStream(
            videoUrl ?: error("KissKH WebView timeout for episode $epId"),
            mapOf("Referer" to "https://kisskh.co/")
        )
    }

    private fun parseKisskhVideoUrl(body: String): String? {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return null
        if (!json.isNull("id")) return null
        return json.optString("Video").ifBlank { null }
            ?: json.optString("ThirdParty").ifBlank { null }
    }

    private fun parseKisskhVideoStream(body: String, epId: String): VideoStream {
        val videoUrl = parseKisskhVideoUrl(body)
            ?: error("No video URL in KissKH response for $epId: ${body.take(200)}")
        Log.d("KissKH", "videoUrl=$videoUrl")
        return VideoStream(videoUrl, mapOf("Referer" to "https://kisskh.co/"))
    }

    private fun generateKisskhKey(episodeId: Int): String {
        val key = hexToBytes("4f6bdaa39e2f8cb07f5e722d9edef314")
        val iv  = hexToBytes("01504af356e619cf2e42bba68c3f70f9")

        val arr = mutableListOf(
            "", episodeId.toString(), "", "mg3c3b04ba", "2.8.10",
            "62f176f3bb1b5b8e70e39932ad34a0c7", "4830201",
            "kisskh", "kisskh", "kisskh", "kisskh", "kisskh", "kisskh",
            "00", ""
        )

        val joined = arr.joinToString("|")
        var hash = 0
        for (c in joined) hash = hash * 31 + c.code

        arr.add(1, hash.toString())
        val plaintext = arr.joinToString("|")
        val padLen = 16 - (plaintext.length % 16)
        val padded = plaintext + padLen.toChar().toString().repeat(padLen)

        val cipher = javax.crypto.Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(
            javax.crypto.Cipher.ENCRYPT_MODE,
            javax.crypto.spec.SecretKeySpec(key, "AES"),
            javax.crypto.spec.IvParameterSpec(iv),
        )
        return cipher.doFinal(padded.toByteArray(Charsets.ISO_8859_1))
            .joinToString("") { "%02X".format(it) }
    }

    private fun hexToBytes(hex: String) =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

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
                    hanimeManifestStream(JSONObject(r.body()), slug)?.let {
                        return it
                    }
                }
            }.onFailure { e ->
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

        if (stream != null) {
        } else {
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

    private fun extractAstarEpisodeStream(playerUrl: String, referer: String, episodeNum: String): String {
        val params = parseQueryParams(playerUrl)
        val id = params["id"] ?: error("No id in astar player URL")
        val hash = params["hash"] ?: error("No hash in astar player URL")
        val basePlayerUrl = "${playerUrl.substringBefore("?")}?id=$id&hash=$hash"

        val playerDoc = Jsoup.connect(basePlayerUrl)
            .userAgent(userAgent)
            .header("Referer", referer)
            .get()
        val ep = (episodeNum.toIntOrNull() ?: 1).coerceAtLeast(1)
        val scriptContent = playerDoc.select("script:not([src])").joinToString("\n") { it.data() }

        val episodeHashes = Regex("""an-media\.org/video/([a-f0-9]{32})/poster""")
            .findAll(scriptContent).map { it.groupValues[1] }.toList()
        if (episodeHashes.isNotEmpty()) {
            val videoHash = episodeHashes.getOrNull(ep - 1)
                ?: error("Episode $ep not available on astar (playlist has ${episodeHashes.size} episodes)")
            return "https://sf2.an-media.org/video/$videoHash/360.mp4/index.m3u8"
        }

        val html = playerDoc.html()
        val hlsUrl = Regex("""https?://[^\s"'\\]+an-media[^\s"'\\]+\.m3u8[^\s"'\\]*""")
            .findAll(html).map { it.value }
            .firstOrNull { "key=" !in it && "media=hls" !in it }
        if (hlsUrl != null) return hlsUrl

        return Regex("""https?://[^\s"'\\]+\.m3u8[^\s"'\\]*""").find(html)?.value
            ?: error("No stream found in astar videoas player for episode $episodeNum")
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
        val match = Regex("""/(serial|seria|video|film)/(\d+)/([0-9a-zA-Z]+)/\d+p""")
            .find(playerUrl) ?: error("Cannot parse Kodik URL: $playerUrl")
        val (typeFromUrl, idFromUrl, hashFromUrl) = match.destructured

        val playerPage = Jsoup.connect(playerUrl)
            .userAgent(userAgent)
            .header("Referer", referer)
            .get()
        val pageHtml = playerPage.html()

        val urlQueryParams = parseQueryParams(referer)
        val season = urlQueryParams["kodikSeason"]
            ?: Regex("""var\s+(?:current)?[Ss]eason\s*=\s*(\d+)""").find(pageHtml)?.groupValues?.get(1)
            ?: "1"
        val episode = urlQueryParams["kodikEpisode"]
            ?: Regex("""var\s+(?:current)?[Ee]pisode\s*=\s*(\d+)""").find(pageHtml)?.groupValues?.get(1)
            ?: "1"

        val hasExplicitEpisode = urlQueryParams.containsKey("kodikEpisode")
        if (!hasExplicitEpisode) {
            Regex("""https?://[^\s"']+\.(m3u8|mp4)[^\s"']*""").find(pageHtml)?.value?.let { return it }
        }

        val urlParamsRaw = Regex("""var\s+urlParams\s*=\s*'(\{[^']+\})'""")
            .find(pageHtml)?.groupValues?.get(1)
        val urlParams = urlParamsRaw?.let { runCatching { JSONObject(it) }.getOrNull() }

        val type = Regex("""vInfo\.type\s*=\s*'([^']+)'""").find(pageHtml)?.groupValues?.get(1) ?: typeFromUrl
        val id   = Regex("""vInfo\.id\s*=\s*'([^']+)'""").find(pageHtml)?.groupValues?.get(1) ?: idFromUrl
        val hash = Regex("""vInfo\.hash\s*=\s*'([^']+)'""").find(pageHtml)?.groupValues?.get(1) ?: hashFromUrl

        if (urlParams != null && !hasExplicitEpisode) {
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

        val finalUrl = if (streamUrl.startsWith("//")) "https:$streamUrl" else streamUrl
        return finalUrl
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
