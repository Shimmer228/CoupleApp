package com.vandoliak.coupleapp.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNavigateToTasks: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFinance: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Couple Hub",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Choose what you want to manage right now.",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToTasks,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Tasks")
                }

                Button(
                    onClick = onNavigateToCalendar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Calendar")
                }

                Button(
                    onClick = onNavigateToFinance,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Finance")
                }
            }
        }
    }
}
