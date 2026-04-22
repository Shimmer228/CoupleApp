package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.presentation.viewmodel.CalendarItemUi
import com.vandoliak.coupleapp.presentation.viewmodel.CalendarViewModel

@Composable
fun CalendarScreen(
    onNavigateToTasks: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToFinance: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: CalendarViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )

    LaunchedEffect(Unit) {
        viewModel.loadCalendar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Calendar",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateHome,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Home")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateToTasks,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tasks")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateToFinance,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Finance")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.selectedDate.value,
            onValueChange = viewModel::onSelectedDateChange,
            label = { Text("Selected date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Create Event",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.eventTitle.value,
                    onValueChange = viewModel::onEventTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.eventDescription.value,
                    onValueChange = viewModel::onEventDescriptionChange,
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.eventDate.value,
                    onValueChange = viewModel::onEventDateChange,
                    label = { Text("Event date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = viewModel::createEvent,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                ) {
                    Text(if (viewModel.isSubmitting.value) "Loading..." else "Create Event")
                }
            }
        }

        viewModel.error.value?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        viewModel.successMessage.value?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isLoading.value && viewModel.calendarItems.value.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (viewModel.calendarItems.value.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No tasks or events for the selected day.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                viewModel.calendarItems.value.forEach { item ->
                    CalendarItemCard(item)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CalendarItemCard(item: CalendarItemUi) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.type,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = item.subtitle,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = item.dateLabel,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
