package com.greenrou.kanata.core.di

import androidx.room.Room
import com.greenrou.kanata.data.local.DownloadDao
import com.greenrou.kanata.data.local.FavoritesDao
import com.greenrou.kanata.data.local.InstalledModDao
import com.greenrou.kanata.data.local.SavedPagesDao
import com.greenrou.kanata.data.local.StorageDao
import com.greenrou.kanata.data.local.StorageDataSource
import com.greenrou.kanata.data.local.StorageDatabase
import com.greenrou.kanata.data.repository.FavoritesManagerImpl
import com.greenrou.kanata.data.repository.SavedPagesManagerImpl
import com.greenrou.kanata.data.repository.StorageManagerImpl
import com.greenrou.kanata.domain.repository.FavoritesManager
import com.greenrou.kanata.domain.repository.SavedPagesManager
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
            StorageDatabase.MIGRATION_5_6,
            StorageDatabase.MIGRATION_6_7,
            StorageDatabase.MIGRATION_7_8,
            StorageDatabase.MIGRATION_8_9,
        ).build()
    }

    single<StorageDao> { get<StorageDatabase>().storageDao() }
    single<FavoritesDao> { get<StorageDatabase>().favoritesDao() }
    single<DownloadDao> { get<StorageDatabase>().downloadDao() }
    single<SavedPagesDao> { get<StorageDatabase>().savedPagesDao() }
    single<InstalledModDao> { get<StorageDatabase>().installedModDao() }

    single { StorageDataSource(get()) }

    single<StorageManager> { StorageManagerImpl(get()) }
    single<FavoritesManager> { FavoritesManagerImpl(get()) }
    single<SavedPagesManager> { SavedPagesManagerImpl(get()) }
}
