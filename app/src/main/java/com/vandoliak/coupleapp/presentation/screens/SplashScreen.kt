package com.vandoliak.coupleapp.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.TokenManager
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val tokenManager = TokenManager(context)
        val token = tokenManager.tokenFlow.first()
        val pairId = tokenManager.pairIdFlow.first()

        val destination = when {
            token.isNullOrBlank() -> "login"
            pairId.isNullOrBlank() -> "pair"
            else -> "home"
        }

        onNavigate(destination)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.loading_splash))
        }
    }
}
