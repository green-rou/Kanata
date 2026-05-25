package com.greenrou.kanata.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greenrou.kanata.domain.model.SavedPage

@Entity(tableName = "saved_pages")
data class SavedPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val savedAt: Long = System.currentTimeMillis(),
)

fun SavedPageEntity.toDomain() = SavedPage(id = id, name = name, url = url, savedAt = savedAt)
