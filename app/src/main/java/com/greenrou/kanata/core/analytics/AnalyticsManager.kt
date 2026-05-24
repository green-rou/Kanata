package com.greenrou.kanata.core.analytics

import android.content.Context
import android.os.Build
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.greenrou.kanata.BuildConfig

class AnalyticsManager(context: Context) {

    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    init {
        setupDeviceProperties(context)
    }

    private fun setupDeviceProperties(context: Context) {
        val appVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        }.getOrDefault("unknown")

        crashlytics.setCustomKey("device_model", Build.MODEL)
        crashlytics.setCustomKey("device_manufacturer", Build.MANUFACTURER)
        crashlytics.setCustomKey("android_version", Build.VERSION.RELEASE)
        crashlytics.setCustomKey("android_sdk", Build.VERSION.SDK_INT)
        crashlytics.setCustomKey("app_version", appVersion)

        // User properties for audience segmentation in Analytics console
        analytics.setUserProperty("device_manufacturer", Build.MANUFACTURER.take(36))
        analytics.setUserProperty("android_version", Build.VERSION.RELEASE.take(36))
    }

    fun setScreen(screenName: String) {
        crashlytics.setCustomKey("current_screen", screenName)
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        })
    }

    fun recordError(
        throwable: Throwable,
        context: String,
        extras: Map<String, String> = emptyMap(),
    ) {
        crashlytics.apply {
            setCustomKey("error_context", context)
            setCustomKey("error_type", throwable::class.simpleName ?: "Unknown")
            setCustomKey("error_message", (throwable.message ?: "").take(200))
            setCustomKey("error_location", throwable.findAppFrame())
            extras.forEach { (k, v) -> setCustomKey(k, v.take(200)) }
            recordException(throwable)
        }
    }

    fun logAnimeOpened(animeId: Int, title: String) {
        analytics.logEvent("anime_opened", Bundle().apply {
            putLong("anime_id", animeId.toLong())
            putString("anime_title", title)
        })
    }

    fun logEpisodeListOpened(animeTitle: String, sourceName: String) {
        analytics.logEvent("episode_list_opened", Bundle().apply {
            putString("anime_title", animeTitle)
            putString("source_name", sourceName)
        })
    }

    fun logEpisodePlayed(animeTitle: String, episodeNumber: Int, sourceName: String) {
        analytics.logEvent("episode_played", Bundle().apply {
            putString("anime_title", animeTitle)
            putLong("episode_number", episodeNumber.toLong())
            putString("source_name", sourceName)
        })
    }

    fun logSearch(query: String) {
        analytics.logEvent("search_performed", Bundle().apply {
            putString("search_term", query)
        })
    }

    fun logFavoriteToggled(animeId: Int, animeTitle: String, added: Boolean) {
        analytics.logEvent(if (added) "favorite_added" else "favorite_removed", Bundle().apply {
            putLong("anime_id", animeId.toLong())
            putString("anime_title", animeTitle)
        })
    }

    fun logMoodSelected(moodName: String) {
        analytics.logEvent("mood_selected", Bundle().apply {
            putString("mood_name", moodName)
        })
    }

    fun logDownloadStarted(animeTitle: String, episodeNumber: Int) {
        analytics.logEvent("download_started", Bundle().apply {
            putString("anime_title", animeTitle)
            putLong("episode_number", episodeNumber.toLong())
        })
    }

    fun setCollectionEnabled(enabled: Boolean) {
        if (BuildConfig.DEBUG) return
        analytics.setAnalyticsCollectionEnabled(enabled)
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }
}

private fun Throwable.findAppFrame(): String =
    stackTrace
        .firstOrNull { it.className.startsWith("com.greenrou.kanata") }
        ?.let { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
        ?: stackTrace.firstOrNull()
            ?.let { "${it.className.substringAfterLast('.')}.${it.methodName}" }
        ?: "unknown"
