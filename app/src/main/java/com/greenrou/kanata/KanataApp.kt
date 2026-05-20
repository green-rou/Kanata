package com.greenrou.kanata

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.greenrou.kanata.BuildConfig
import com.greenrou.kanata.core.di.analyticsModule
import com.greenrou.kanata.core.di.aniListModule
import com.greenrou.kanata.core.di.databaseModule
import com.greenrou.kanata.core.di.repositoryModule
import com.greenrou.kanata.core.di.useCaseModule
import com.greenrou.kanata.core.di.viewModelModule
import com.greenrou.kanata.core.di.workManagerModule
import com.greenrou.kanata.core.network.networkModule
import com.greenrou.kanata.data.youtube.NewPipeDownloader
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class KanataApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        NewPipe.init(
            NewPipeDownloader(OkHttpClient()),
            Localization.DEFAULT,
            ContentCountry.DEFAULT
        )
        startKoin {
            androidLogger()
            androidContext(this@KanataApp)
            modules(networkModule, databaseModule, repositoryModule, useCaseModule, viewModelModule, aniListModule, workManagerModule, analyticsModule)
        }
    }
}
