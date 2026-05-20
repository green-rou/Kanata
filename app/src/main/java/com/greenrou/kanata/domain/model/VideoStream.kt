package com.greenrou.kanata.domain.model

data class VideoStream(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)
