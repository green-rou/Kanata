package com.greenrou.kanata.features.onlinesearch.model

import com.greenrou.kanata.domain.model.OnlineSearchGroup

data class OnlineSearchScreenState(
    val groups: List<OnlineSearchGroup> = emptyList(),
    val currentQuery: String = "",
    val hiddenGroups: Set<String> = emptySet(),
)
