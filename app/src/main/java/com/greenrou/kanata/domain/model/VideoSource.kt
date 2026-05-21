package com.greenrou.kanata.domain.model

enum class VideoSourceType {
    ARCHIVE_ORG,
    HANIME,
    YOUTUBE,
    ANITUBE,
    ANIWAVE,
    MIKAI,
    YUMMY_ANIME,
    HENTAI_HUB,
    HENTASIS,
    HENTAIZ,
    ASTAR,
    ANIMEGO_NGO,
    KISSKH,
    UNKNOWN,
}

data class VideoSource(
    val label: String,
    val animePageUrl: String,
    val type: VideoSourceType = VideoSourceType.UNKNOWN,
)
