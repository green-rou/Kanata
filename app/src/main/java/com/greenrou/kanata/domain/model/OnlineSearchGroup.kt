package com.greenrou.kanata.domain.model

data class OnlineSearchGroup(
    val sourceLabel: String,
    val isLoading: Boolean,
    val results: List<OnlineSearchResult>,
    val error: Boolean,
)
