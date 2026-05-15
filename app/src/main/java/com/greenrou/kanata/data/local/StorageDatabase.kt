package com.greenrou.kanata.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [StorageEntity::class, FavoriteEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StorageDatabase : RoomDatabase() {
    
    abstract fun storageDao(): StorageDao
    abstract fun favoritesDao(): FavoritesDao
}
