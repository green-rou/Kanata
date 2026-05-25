package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.SavedPage
import kotlinx.coroutines.flow.Flow

interface SavedPagesManager {
    fun getAll(): Flow<List<SavedPage>>
    suspend fun save(name: String, url: String): Result<Unit>
    suspend fun delete(id: Long): Result<Unit>
}
