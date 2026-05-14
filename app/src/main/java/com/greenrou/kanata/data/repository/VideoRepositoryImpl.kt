package com.greenrou.kanata.data.repository

import android.content.Context
import com.greenrou.kanata.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URL

class VideoRepositoryImpl(
    private val context: Context
) : VideoRepository {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    override suspend fun getVideoStream(siteUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val document = Jsoup.connect(siteUrl).userAgent(userAgent).get()

            val standardIframe = document
                .select("iframe[src*=/serial/], iframe[src*=/video/], iframe[src*=kodik]")
                .firstOrNull()?.attr("src")
            if (standardIframe != null) {
                return@runCatching resolveUrl(siteUrl, standardIframe)
            }

            val litespeedIframe = document.select("iframe[data-litespeed-src]").firstOrNull()
            if (litespeedIframe != null) {
                val iframeUrl = litespeedIframe.attr("data-litespeed-src")
                return@runCatching tryExtractHlsOrReturn(siteUrl, iframeUrl)
            }

            val videoSrc = document.select("video source, video").firstOrNull()?.attr("src")
            if (videoSrc != null) return@runCatching videoSrc

            val xfplayer = document
                .select(".tabs-block__content:not(.d-none):not(.hidden) .xfplayer[data-params]")
                .firstOrNull() ?: error("Could not find any video player on this page")

            val dataParams = xfplayer.attr("data-params")
            val base = URL(siteUrl)
            val ajaxUrl = "${base.protocol}://${base.host}/engine/ajax/controller.php?$dataParams"

            val json = Jsoup.connect(ajaxUrl)
                .userAgent(userAgent)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", siteUrl)
                .ignoreContentType(true)
                .execute().body()

            val playerUrl = JSONObject(json).optString("data")
            if (playerUrl.isBlank()) error("Player returned empty URL")

            tryExtractHlsOrReturn(siteUrl, playerUrl)
        }
    }

    private fun tryExtractHlsOrReturn(referer: String, playerUrl: String): String {
        return runCatching {
            val playerDoc = Jsoup.connect(playerUrl)
                .userAgent(userAgent)
                .header("Referer", referer)
                .get()

            val dataConfig = playerDoc.select("[data-config]").firstOrNull()?.attr("data-config")
            if (dataConfig != null) {
                val hls = JSONObject(dataConfig).optString("hls")
                if (hls.isNotBlank()) return hls
            }

            val m3u8 = Regex("""https?://[^\s"']+\.m3u8[^\s"']*""").find(playerDoc.html())?.value
            if (m3u8 != null) return m3u8

            playerUrl // return player page URL as last resort
        }.getOrDefault(playerUrl)
    }

    private fun resolveUrl(base: String, url: String): String = when {
        url.startsWith("//") -> "https:$url"
        !url.startsWith("http") -> URL(base).run { "$protocol://$host$url" }
        else -> url
    }
}
