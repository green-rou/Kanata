package com.greenrou.kanata.features.webplayer

import android.webkit.WebResourceResponse

internal object AdBlocker {

    private val BLOCKED_HOSTS = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "analytics.google.com",
        "googletagmanager.com",
        "googletagservices.com",
        "connect.facebook.net",
        "facebook.com",
        "adnxs.com",
        "advertising.com",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "adsrvr.org",
        "3lift.com",
        "smartadserver.com",
        "amazon-adsystem.com",
        "exoclick.com",
        "juicyads.com",
        "trafficjunky.net",
        "trafficfactory.biz",
        "ero-advertising.com",
        "plugrush.com",
        "propellerads.com",
        "popads.net",
        "popcash.net",
        "adsterra.com",
        "hilltopads.net",
        "valueimpression.com",
        "traffic-media.co",
        "clickadu.com",
        "richpush.co",
        "evadav.com",
        "adcash.com",
        "zeropark.com",
        "pushground.com",
        "mgid.com",
        "revcontent.com",
        "imasdk.googleapis.com",
        "pubads.g.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "ads.yahoo.com",
        "vid.springserve.com",
        "ads.adaptv.advertising.com",
        "sync.adaptv.advertising.com",
        "cdn.adnxs.com",
        "coinhive.com",
        "coin-hive.com",
        "cryptoloot.pro",
        "minero.cc",
        "silent-basis.pro",
    )

    private val BLOCKED_PATH_PATTERNS = listOf(
        Regex("""/_blank\.""", RegexOption.IGNORE_CASE),
        Regex("""/(?:ads?|advert(?:is(?:ement|ing))?|banner|popup|popunder|interstitial)/""", RegexOption.IGNORE_CASE),
        Regex("""[?&](?:ad|adunit|adtype|adzone|adslot|adid)=""", RegexOption.IGNORE_CASE),
        Regex("""/vast[/?]""", RegexOption.IGNORE_CASE),
        Regex("""/vpaid[/?]""", RegexOption.IGNORE_CASE),
        Regex("""[?&]vast=""", RegexOption.IGNORE_CASE),
        Regex("""/ima/""", RegexOption.IGNORE_CASE),
        Regex("""googlesyndication""", RegexOption.IGNORE_CASE),
        Regex("""doubleclick""", RegexOption.IGNORE_CASE),
    )

    fun shouldBlock(url: String): Boolean {
        val host = extractHost(url) ?: return false
        return isBlockedHost(host) || BLOCKED_PATH_PATTERNS.any { it.containsMatchIn(url) }
    }

    fun isAdStream(url: String): Boolean = shouldBlock(url)

    fun emptyResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "utf-8", ByteArray(0).inputStream())

    private fun isBlockedHost(host: String): Boolean =
        BLOCKED_HOSTS.any { blocked -> host == blocked || host.endsWith(".$blocked") }

    private fun extractHost(url: String): String? =
        runCatching { java.net.URI(url).host?.lowercase() }.getOrNull()
}
