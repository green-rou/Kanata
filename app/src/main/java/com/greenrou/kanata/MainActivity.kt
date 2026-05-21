package com.greenrou.kanata

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.core.util.LanguagePrefs
import com.greenrou.kanata.features.main.MainViewModel
import com.greenrou.kanata.features.update.UpdateDialog
import com.greenrou.kanata.features.update.UpdateViewModel
import com.greenrou.kanata.features.update.model.UpdateEvent
import com.greenrou.kanata.navigation.MainRoute
import com.greenrou.kanata.navigation.NavGraph
import com.greenrou.kanata.ui.theme.KanataTheme
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) {
        val lang = LanguagePrefs.get(base)
        val ctx = if (lang == LanguagePrefs.SYSTEM) base else {
            val config = Configuration(base.resources.configuration)
            config.setLocale(Locale(lang))
            base.createConfigurationContext(config)
        }
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT,
            ),
        )
        setContent {
            val mainViewModel: MainViewModel = koinViewModel()
            val state by mainViewModel.state.collectAsStateWithLifecycle()
            val view = LocalView.current

            val updateViewModel: UpdateViewModel = koinViewModel()
            val updateState by updateViewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                updateViewModel.handleEvent(UpdateEvent.CheckUpdate)
            }

            LaunchedEffect(Unit) {
                updateViewModel.installFile.collect { file ->
                    val uri = FileProvider.getUriForFile(
                        this@MainActivity,
                        "${packageName}.fileprovider",
                        file,
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            }

            LaunchedEffect(Unit) {
                updateViewModel.needInstallPermission.collect {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            }

            KanataTheme(darkTheme = state.isDarkTheme, accentColor = state.accentColor) {
                if (!view.isInEditMode) {
                    val isDark = state.isDarkTheme
                    SideEffect {
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !isDark
                            isAppearanceLightNavigationBars = !isDark
                        }
                    }
                }

                val backStack = remember { mutableStateListOf<Any>(MainRoute) }
                NavGraph(backStack = backStack)

                updateState.pendingRelease?.let { release ->
                    UpdateDialog(
                        release = release,
                        isDownloading = updateState.isDownloading,
                        downloadProgress = updateState.downloadProgress,
                        onUpdate = { updateViewModel.handleEvent(UpdateEvent.StartDownload) },
                        onSkip = { updateViewModel.handleEvent(UpdateEvent.SkipUpdate) },
                        onDismiss = { updateViewModel.handleEvent(UpdateEvent.DismissDialog) },
                    )
                }
            }
        }
    }
}
