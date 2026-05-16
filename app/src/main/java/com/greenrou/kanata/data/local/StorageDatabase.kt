package com.greenrou.kanata.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StorageEntity::class, FavoriteEntity::class, DownloadEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StorageDatabase : RoomDatabase() {

    abstract fun storageDao(): StorageDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN animeId INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN animePageUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS downloads (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        animeTitle TEXT NOT NULL,
                        sourceName TEXT NOT NULL,
                        episodeTitle TEXT NOT NULL,
                        episodePageUrl TEXT NOT NULL,
                        localFilePath TEXT,
                        status TEXT NOT NULL,
                        progressPercent INTEGER NOT NULL DEFAULT 0,
                        queuePosition INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        fileSizeBytes INTEGER NOT NULL DEFAULT 0,
                        errorMessage TEXT
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
