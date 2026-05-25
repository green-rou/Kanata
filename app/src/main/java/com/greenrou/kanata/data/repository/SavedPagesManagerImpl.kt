package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.local.SavedPageEntity
import com.greenrou.kanata.data.local.SavedPagesDao
import com.greenrou.kanata.data.local.toDomain
import com.greenrou.kanata.domain.model.SavedPage
import com.greenrou.kanata.domain.repository.SavedPagesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedPagesManagerImpl(private val dao: SavedPagesDao) : SavedPagesManager {

    override fun getAll(): Flow<List<SavedPage>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun save(name: String, url: String): Result<Unit> = runCatching {
        dao.insert(SavedPageEntity(name = name, url = url))
    }

    override suspend fun delete(id: Long): Result<Unit> = runCatching {
        dao.deleteById(id)
    }
}
