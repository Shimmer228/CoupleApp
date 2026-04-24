package com.vandoliak.coupleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.vandoliak.coupleapp.presentation.navigation.AppNavigation
import com.vandoliak.coupleapp.ui.theme.CoupleAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoupleAppTheme {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
