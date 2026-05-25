package com.greenrou.kanata.features.webplayer

import android.webkit.WebResourceResponse

internal object AdBlocker {

    private val BLOCKED_HOSTS = setOf(
        // Google ads & tracking
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "analytics.google.com",
        "googletagmanager.com",
        "googletagservices.com",
        // Meta / Facebook
        "connect.facebook.net",
        "facebook.com",
        // Major ad networks
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
        // Adult / streaming site specific ad networks
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
        // Video ad CDNs (these serve the "wrong" video the user sees)
        "imasdk.googleapis.com",
        "pubads.g.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "ads.yahoo.com",
        "vid.springserve.com",
        "ads.adaptv.advertising.com",
        "sync.adaptv.advertising.com",
        "cdn.adnxs.com",
        // Crypto miners
        "coinhive.com",
        "coin-hive.com",
        "cryptoloot.pro",
        "minero.cc",
        // Ambient/background video CDNs (decorative site videos, not episode streams)
        "silent-basis.pro",
    )

    // Path/query patterns that strongly indicate an ad request
    private val BLOCKED_PATH_PATTERNS = listOf(
        // Placeholder/empty video files used by embedded players during initialisation
        Regex("""/_blank\.""", RegexOption.IGNORE_CASE),
        Regex("""/(?:ads?|advert(?:is(?:ement|ing))?|banner|popup|popunder|interstitial)/""", RegexOption.IGNORE_CASE),
        Regex("""[?&](?:ad|adunit|adtype|adzone|adslot|adid)=""", RegexOption.IGNORE_CASE),
        Regex("""/vast[/?]""", RegexOption.IGNORE_CASE),      // VAST ad tags
        Regex("""/vpaid[/?]""", RegexOption.IGNORE_CASE),     // VPAID video ads
        Regex("""[?&]vast=""", RegexOption.IGNORE_CASE),
        Regex("""/ima/""", RegexOption.IGNORE_CASE),          // Google IMA SDK paths
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
