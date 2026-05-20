package com.greenrou.kanata.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class AnalyticsManager(context: Context) {

    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

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

    fun recordError(throwable: Throwable, context: String? = null) {
        context?.let { crashlytics.setCustomKey("error_context", it) }
        crashlytics.recordException(throwable)
    }

    fun setScreen(screenName: String) {
        crashlytics.setCustomKey("current_screen", screenName)
    }
}
