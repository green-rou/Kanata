package com.greenrou.kanata.core.di

import com.greenrou.kanata.data.parsers.AniwaveSiteParser
// import com.greenrou.kanata.data.parsers.AnitubeSiteParser
import com.greenrou.kanata.data.parsers.MikaiSiteParser
import com.greenrou.kanata.data.parsers.YummyAnimeSiteParser
import com.greenrou.kanata.data.repository.AnimeRepositoryImpl
import com.greenrou.kanata.data.repository.EpisodeListRepositoryImpl
import com.greenrou.kanata.data.repository.RandomRepositoryImpl
import com.greenrou.kanata.data.repository.SearchRepositoryImpl
import com.greenrou.kanata.data.repository.SettingsManagerImpl
import com.greenrou.kanata.data.repository.VideoRepositoryImpl
import com.greenrou.kanata.domain.parser.SiteParser
import com.greenrou.kanata.domain.repository.AnimeRepository
import com.greenrou.kanata.domain.repository.EpisodeListRepository
import com.greenrou.kanata.domain.repository.RandomRepository
import com.greenrou.kanata.domain.repository.SearchRepository
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.repository.VideoRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<List<SiteParser>> {
        listOf(
            YummyAnimeSiteParser(),
            AniwaveSiteParser(),
            MikaiSiteParser(),
            // TODO: Re-enable once AnitubeSiteParser can reliably find the DLE player and extract episodes
            // AnitubeSiteParser(),
        )
    }
    single<AnimeRepository> { AnimeRepositoryImpl(get()) }
    single<SettingsManager> { SettingsManagerImpl(get()) }
    single<RandomRepository> { RandomRepositoryImpl(get(), get()) }
    single<VideoRepository> { VideoRepositoryImpl(get()) }
    single<SearchRepository> { SearchRepositoryImpl(get()) }
    single<EpisodeListRepository> { EpisodeListRepositoryImpl(get()) }
}
