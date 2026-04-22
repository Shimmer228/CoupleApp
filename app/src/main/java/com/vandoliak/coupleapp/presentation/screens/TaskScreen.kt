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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.data.remote.TaskDto
import com.vandoliak.coupleapp.presentation.util.formatIsoDateForDisplay
import com.vandoliak.coupleapp.presentation.viewmodel.TaskViewModel

@Composable
fun TaskScreen(
    onNavigateToCalendar: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToFinance: () -> Unit
) {
    val context = LocalContext.current

    val viewModel: TaskViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory(
            context.applicationContext as Application
        )
    )

    LaunchedEffect(Unit) {
        viewModel.loadTasks()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Task Arena",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Your points: ${viewModel.currentUserPoints.value}",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleMedium
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
            onClick = onNavigateToCalendar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calendar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onNavigateToFinance,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Finance")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Create Challenge",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.taskTitle.value,
                    onValueChange = viewModel::onTaskTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.taskPoints.value,
                    onValueChange = viewModel::onTaskPointsChange,
                    label = { Text("Points") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.dueDate.value,
                    onValueChange = viewModel::onDueDateChange,
                    label = { Text("Due date (YYYY-MM-DD, optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = viewModel::createTask,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                ) {
                    Text(if (viewModel.isSubmitting.value) "Loading..." else "Create Task")
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

        if (viewModel.isLoading.value && viewModel.tasks.value.isEmpty()) {
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
                if (viewModel.tasks.value.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No tasks yet. Create the first challenge.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                viewModel.tasks.value.forEach { task ->
                    TaskItem(
                        task = task,
                        currentUserId = viewModel.currentUserId.value,
                        isSubmitting = viewModel.isSubmitting.value,
                        onComplete = { viewModel.completeTask(task.id) },
                        onReturn = { viewModel.returnTask(task.id) },
                        onFail = { viewModel.failTask(task.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TaskItem(
    task: TaskDto,
    currentUserId: String?,
    isSubmitting: Boolean,
    onComplete: () -> Unit,
    onReturn: () -> Unit,
    onFail: () -> Unit
) {
    val isAssignedToCurrentUser = currentUserId == task.assignedTo.id
    val isActive = task.status == "ACTIVE"
    val dueDate = formatIsoDateForDisplay(task.dueDate)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bank: ${task.bank} points",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(text = "Status: ${task.status.lowercase().replaceFirstChar { it.uppercase() }}")
            Text(text = "Created by: ${task.createdBy.email}")
            Text(text = "Assigned to: ${task.assignedTo.email}")
            dueDate?.let {
                Text(text = "Due date: $it")
            }

            if (isActive && isAssignedToCurrentUser) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    Text("Complete")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onReturn,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    Text("Return")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onFail,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    Text("Fail")
                }
            }
        }
    }
}
