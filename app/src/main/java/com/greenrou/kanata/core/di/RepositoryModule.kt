package com.greenrou.kanata.core.di

import com.greenrou.kanata.BuildConfig
import com.greenrou.kanata.data.mod.ChapterParserRegistry
import com.greenrou.kanata.data.mod.DownloadFeatureRegistry
import com.greenrou.kanata.data.mod.InfoProviderRegistry
import com.greenrou.kanata.data.mod.MangaModRegistry
import com.greenrou.kanata.data.mod.ModLoader
import com.greenrou.kanata.data.mod.ParserRegistry
import com.greenrou.kanata.data.repository.AnimeRepositoryImpl
import com.greenrou.kanata.data.repository.ChapterListRepositoryImpl
import com.greenrou.kanata.data.repository.ContentPagesRepositoryImpl
import com.greenrou.kanata.data.repository.ContentSearchRepositoryImpl
import com.greenrou.kanata.data.repository.DownloadRepositoryImpl
import com.greenrou.kanata.data.repository.EpisodeListRepositoryImpl
import com.greenrou.kanata.data.repository.ModRepositoryImpl
import com.greenrou.kanata.data.repository.RandomRepositoryImpl
import com.greenrou.kanata.data.repository.SearchRepositoryImpl
import com.greenrou.kanata.data.repository.SettingsManagerImpl
import com.greenrou.kanata.data.repository.UpdateRepositoryImpl
import com.greenrou.kanata.data.repository.VideoRepositoryImpl
import com.greenrou.kanata.domain.repository.AnimeRepository
import com.greenrou.kanata.domain.repository.ChapterListRepository
import com.greenrou.kanata.domain.repository.ContentPagesRepository
import com.greenrou.kanata.domain.repository.ContentSearchRepository
import com.greenrou.kanata.domain.repository.DownloadRepository
import com.greenrou.kanata.domain.repository.EpisodeListRepository
import com.greenrou.kanata.domain.repository.ModRepository
import com.greenrou.kanata.domain.repository.RandomRepository
import com.greenrou.kanata.domain.repository.SearchRepository
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.repository.UpdateRepository
import com.greenrou.kanata.domain.repository.VideoRepository
import org.koin.dsl.module

val repositoryModule = module {
    single { ModLoader(get()) }

    single {
        ParserRegistry(emptyList(), get(), get())
    }
    single { InfoProviderRegistry(get(), get()) }
    single { DownloadFeatureRegistry(get(), get()) }
    single { MangaModRegistry(get(), get()) }
    single { ChapterParserRegistry(get(), get()) }
    single<ModRepository> {
        ModRepositoryImpl(
            dao = get(),
            api = get(),
            okHttpClient = get(),
            modsDir = get<ModLoader>().modsDir,
            modIndexUrl = BuildConfig.MOD_INDEX_URL,
        )
    }
    single<AnimeRepository> { AnimeRepositoryImpl(get()) }
    single<SettingsManager> { SettingsManagerImpl(get()) }
    single<RandomRepository> { RandomRepositoryImpl(get(), get()) }
    single<VideoRepository> { VideoRepositoryImpl(get()) }
    single<SearchRepository> { SearchRepositoryImpl(get(), get(), get<ChapterParserRegistry>()) }
    single<EpisodeListRepository> { EpisodeListRepositoryImpl(get()) }
    single<ChapterListRepository> { ChapterListRepositoryImpl(get()) }
    single<ContentPagesRepository> { ContentPagesRepositoryImpl(get()) }
    single<ContentSearchRepository> { ContentSearchRepositoryImpl(get()) }
    single<DownloadRepository> { DownloadRepositoryImpl(get(), get(), get()) }
    single<UpdateRepository> { UpdateRepositoryImpl(get()) }
}
