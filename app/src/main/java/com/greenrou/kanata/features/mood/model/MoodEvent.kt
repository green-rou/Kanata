package com.greenrou.kanata.features.mood.model

sealed interface MoodEvent {
    data class SelectMood(val mood: Mood) : MoodEvent
    data object ClearMood : MoodEvent
    data class AnimeClicked(val animeId: Int) : MoodEvent

    data class NavigateToDetails(val animeId: Int) : MoodEvent
}
