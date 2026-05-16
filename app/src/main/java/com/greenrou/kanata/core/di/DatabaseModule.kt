package com.greenrou.kanata.core.di

import android.content.Context
import androidx.room.Room
import com.greenrou.kanata.data.local.DownloadDao
import com.greenrou.kanata.data.local.FavoritesDao
import com.greenrou.kanata.data.local.StorageDao
import com.greenrou.kanata.data.local.StorageDatabase
import com.greenrou.kanata.data.local.StorageDataSource
import com.greenrou.kanata.data.repository.FavoritesManagerImpl
import com.greenrou.kanata.data.repository.StorageManagerImpl
import com.greenrou.kanata.domain.repository.FavoritesManager
import com.greenrou.kanata.domain.repository.StorageManager
import org.koin.dsl.module

val databaseModule = module {
    
    single<StorageDatabase> {
        Room.databaseBuilder(
            get(),
            StorageDatabase::class.java,
            "storage_database"
        ).addMigrations(
            StorageDatabase.MIGRATION_2_3,
            StorageDatabase.MIGRATION_3_4,
            StorageDatabase.MIGRATION_4_5,
        ).build()
    }
    
    single<StorageDao> { get<StorageDatabase>().storageDao() }
    single<FavoritesDao> { get<StorageDatabase>().favoritesDao() }
    single<DownloadDao> { get<StorageDatabase>().downloadDao() }
    
    single { StorageDataSource(get()) }
    
    single<StorageManager> { StorageManagerImpl(get()) }
    single<FavoritesManager> { FavoritesManagerImpl(get()) }
}
