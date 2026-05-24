package com.greenrou.kanata.domain.model

data class SavedPage(
    val id: Long,
    val name: String,
    val url: String,
    val savedAt: Long,
)
