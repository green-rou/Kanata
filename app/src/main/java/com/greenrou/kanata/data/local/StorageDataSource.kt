package com.greenrou.kanata.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

class StorageDataSource(
    private val storageDao: StorageDao
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun <T : Any> storeData(key: String, data: T): Result<Unit> = runCatching {
        val jsonString = if (data is String) {
            data
        } else {
            val serializer = json.serializersModule.serializer(data::class.java)
            json.encodeToString(serializer, data)
        }
        val dataType = data::class.java.simpleName
        val entity = StorageEntity(
            key = key,
            data = jsonString,
            dataType = dataType
        )
        storageDao.insert(entity)
    }
    
    suspend fun <T> getData(key: String, clazz: Class<T>): Result<T?> = runCatching {
        val entity = storageDao.getByKey(key)
        entity?.let { decodeEntity(it, clazz) }
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun <T> decodeEntity(entity: StorageEntity, clazz: Class<T>): T {
        return when (clazz) {
            String::class.java -> entity.data as T
            Int::class.java, java.lang.Integer::class.java -> entity.data.toInt() as T
            Boolean::class.java, java.lang.Boolean::class.java -> entity.data.toBoolean() as T
            Float::class.java -> entity.data.toFloat() as T
            Long::class.java -> entity.data.toLong() as T
            else -> {
                val serializer = json.serializersModule.serializer(clazz)
                json.decodeFromString(serializer, entity.data) as T
            }
        }
    }
    
    fun <T> getDataFlow(key: String, clazz: Class<T>): Flow<T?> {
        return storageDao.getByKeyFlow(key).map { entity ->
            entity?.let { decodeEntity(it, clazz) }
        }
    }
    
    suspend fun removeData(key: String): Result<Unit> = runCatching {
        storageDao.deleteByKey(key)
    }
    
    suspend fun clearAll(): Result<Unit> = runCatching {
        storageDao.deleteAll()
    }
    
    suspend fun hasKey(key: String): Result<Boolean> = runCatching {
        storageDao.hasKey(key)
    }
    
    suspend fun getAllKeys(): Result<List<String>> = runCatching {
        storageDao.getAllKeys()
    }
}
