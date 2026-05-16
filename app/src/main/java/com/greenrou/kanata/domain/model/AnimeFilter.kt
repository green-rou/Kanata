package com.greenrou.kanata.domain.model

data class AnimeFilter(
    val search: String = "",
    val genres: List<String> = emptyList(),
    val formats: List<AnimeFormat> = emptyList(),
) {
    val isEmpty: Boolean get() = search.isBlank() && genres.isEmpty() && formats.isEmpty()
}

enum class AnimeFormat(val displayName: String) {
    TV("TV Series"),
    MOVIE("Movie"),
    OVA("OVA"),
    ONA("ONA"),
    SPECIAL("Special"),
    TV_SHORT("TV Short"),
}
