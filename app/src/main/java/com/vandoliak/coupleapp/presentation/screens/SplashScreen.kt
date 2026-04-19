package com.vandoliak.coupleapp.presentation.screens

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.vandoliak.coupleapp.data.local.TokenManager
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(
    onNavigate: (Boolean) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val token = TokenManager(context).tokenFlow.first()
        onNavigate(token != null)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Loading...")
    }
}