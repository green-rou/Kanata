package com.greenrou.kanata.domain.repository

import kotlinx.coroutines.flow.Flow

interface StorageManager {
    suspend fun <T> storeData(key: String, data: T): Result<Unit>
    suspend fun <T> getData(key: String, clazz: Class<T>): Result<T?>
    fun <T> getDataFlow(key: String, clazz: Class<T>): Flow<Result<T?>>
    suspend fun removeData(key: String): Result<Unit>
    suspend fun clearAll(): Result<Unit>
    suspend fun hasKey(key: String): Result<Boolean>
    suspend fun getAllKeys(): Result<List<String>>
}
