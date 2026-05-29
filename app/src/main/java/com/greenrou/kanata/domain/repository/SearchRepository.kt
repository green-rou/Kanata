package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.OnlineSearchGroup
import com.greenrou.kanata.domain.model.VideoSource
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun searchAll(titles: List<String>): List<VideoSource>
    fun searchOnline(query: String, isMangaMode: Boolean): Flow<List<OnlineSearchGroup>>
}
