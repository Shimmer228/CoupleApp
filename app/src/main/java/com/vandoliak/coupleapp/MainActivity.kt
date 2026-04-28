package com.vandoliak.coupleapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import com.vandoliak.coupleapp.data.local.AppLanguage
import com.vandoliak.coupleapp.data.local.AppSettingsManager
import com.vandoliak.coupleapp.data.local.ThemeMode
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.presentation.navigation.AppNavigation
import com.vandoliak.coupleapp.ui.theme.CoupleAppTheme
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsManager = AppSettingsManager(applicationContext)
        runBlocking {
            settingsManager.applyLanguage(settingsManager.currentLanguage())
        }
        RetrofitInstance.initialize(applicationContext)

        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val language by settingsManager.languageFlow.collectAsState(initial = AppLanguage.ENGLISH)

            CoupleAppTheme(themeMode = themeMode) {
                LaunchedEffect(language) {
                    settingsManager.applyLanguage(language)
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AppNavigation(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
