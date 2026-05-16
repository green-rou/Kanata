package com.greenrou.kanata

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.greenrou.kanata.core.util.LanguagePrefs
import java.util.Locale
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.features.main.MainViewModel
import com.greenrou.kanata.navigation.MainRoute
import com.greenrou.kanata.navigation.NavGraph
import com.greenrou.kanata.ui.theme.KanataTheme
import org.koin.androidx.compose.koinViewModel

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
            }
        }
    }
}
