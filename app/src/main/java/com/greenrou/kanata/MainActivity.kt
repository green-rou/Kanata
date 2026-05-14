package com.greenrou.kanata

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    override fun onCreate(savedInstanceState: Bundle?) {
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

            KanataTheme(darkTheme = state.isDarkTheme) {
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
