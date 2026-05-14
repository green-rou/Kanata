package com.greenrou.kanata.domain.model

data class AnimeListPage(
    val items: List<Anime>,
    val hasNextPage: Boolean,
    val currentPage: Int,
)
