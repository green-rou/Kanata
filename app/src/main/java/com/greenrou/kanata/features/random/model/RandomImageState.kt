package com.greenrou.kanata.features.random.model

import com.greenrou.kanata.domain.model.Anime

data class RandomImageState(
    val isAnimeLoading: Boolean = false,
    val randomAnime: Anime? = null,
    val animeError: String? = null,
    val isAnimeFavorite: Boolean = false,

    val isImageLoading: Boolean = false,
    val imageUrl: String? = null,
    val imageError: String? = null,
)
