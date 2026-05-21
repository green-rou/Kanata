package com.greenrou.kanata.domain.model

data class Translation(
    val id: String,
    val title: String,
    val type: String,
    val mediaId: String,
    val mediaHash: String,
    val mediaType: String,
)
