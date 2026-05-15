package com.greenrou.kanata.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_items")
data class StorageEntity(
    @PrimaryKey
    val key: String,
    val data: String,
    val dataType: String,
    val timestamp: Long = System.currentTimeMillis()
)
