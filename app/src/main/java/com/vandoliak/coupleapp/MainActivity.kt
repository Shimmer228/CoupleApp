package com.vandoliak.coupleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.vandoliak.coupleapp.data.local.AppSettingsManager
import com.vandoliak.coupleapp.data.local.ThemeMode
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.presentation.navigation.AppNavigation
import com.vandoliak.coupleapp.ui.theme.CoupleAppTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsManager = AppSettingsManager(applicationContext)
        runBlocking {
            settingsManager.applyLanguage(settingsManager.currentLanguage())
        }
        RetrofitInstance.initialize(applicationContext)

        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)

            CoupleAppTheme(themeMode = themeMode) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
