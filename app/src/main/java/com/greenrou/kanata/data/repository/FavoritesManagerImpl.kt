package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.local.FavoritesDao
import com.greenrou.kanata.data.local.toDomain
import com.greenrou.kanata.data.local.toEntity
import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.repository.FavoritesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesManagerImpl(
    private val favoritesDao: FavoritesDao
) : FavoritesManager {

    override suspend fun addFavorite(anime: Anime): Result<Unit> = runCatching {
        favoritesDao.insertFavorite(anime.toEntity())
    }

    override suspend fun removeFavorite(id: Int): Result<Unit> = runCatching {
        favoritesDao.deleteFavoriteById(id)
    }

    override suspend fun getFavoritesPaged(page: Int, pageSize: Int): Result<List<Anime>> = runCatching {
        val offset = (page - 1) * pageSize
        favoritesDao.getFavoritesPaged(pageSize, offset).map { it.toDomain() }
    }

    override fun getFavoritesPagedFlow(limit: Int): Flow<Result<List<Anime>>> {
        return favoritesDao.getFavoritesPagedFlow(limit).map { entities ->
            Result.success(entities.map { it.toDomain() })
        }
    }

    override fun getAllFavoritesFlow(): Flow<Result<List<Anime>>> {
        return favoritesDao.getAllFavoritesFlow().map { entities ->
            Result.success(entities.map { it.toDomain() })
        }
    }

    override suspend fun getAllFavoriteIds(): Result<List<Int>> = runCatching {
        favoritesDao.getAllFavoriteIds()
    }

    override fun getAllFavoriteIdsFlow(): Flow<Result<List<Int>>> {
        return favoritesDao.getAllFavoriteIdsFlow().map { ids ->
            Result.success(ids)
        }
    }

    override suspend fun isFavorite(id: Int): Result<Boolean> = runCatching {
        favoritesDao.isFavorite(id)
    }

    override fun isFavoriteFlow(id: Int): Flow<Result<Boolean>> {
        return favoritesDao.isFavoriteFlow(id).map { isFavorite ->
            Result.success(isFavorite)
        }
    }
}
