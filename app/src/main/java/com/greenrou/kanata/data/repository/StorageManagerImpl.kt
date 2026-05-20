package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.local.StorageDataSource
import com.greenrou.kanata.domain.repository.StorageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StorageManagerImpl(
    private val storageDataSource: StorageDataSource
) : StorageManager {

    override suspend fun <T> storeData(key: String, data: T): Result<Unit> {
        @Suppress("UNCHECKED_CAST")
        return storageDataSource.storeData(key, data as Any)
    }

    override suspend fun <T> getData(key: String, clazz: Class<T>): Result<T?> {
        return storageDataSource.getData(key, clazz)
    }

    override fun <T> getDataFlow(key: String, clazz: Class<T>): Flow<Result<T?>> {
        return storageDataSource.getDataFlow(key, clazz).map { data ->
            Result.success(data)
        }
    }

    override suspend fun removeData(key: String): Result<Unit> {
        return storageDataSource.removeData(key)
    }

    override suspend fun clearAll(): Result<Unit> {
        return storageDataSource.clearAll()
    }

    override suspend fun hasKey(key: String): Result<Boolean> {
        return storageDataSource.hasKey(key)
    }

    override suspend fun getAllKeys(): Result<List<String>> {
        return storageDataSource.getAllKeys()
    }
}
