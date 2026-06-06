package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.WatchProgress
import kotlinx.coroutines.flow.Flow

interface WatchProgressRepository {
    suspend fun save(progress: WatchProgress)
    suspend fun getByUrl(url: String): WatchProgress?
    fun observeByUrls(urls: List<String>): Flow<List<WatchProgress>>
    suspend fun getLastWatched(): WatchProgress?
}
