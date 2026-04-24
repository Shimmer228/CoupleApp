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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.data.remote.TaskDto
import com.vandoliak.coupleapp.presentation.components.AppCard
import com.vandoliak.coupleapp.presentation.components.EmptyState
import com.vandoliak.coupleapp.presentation.components.PrimaryActionButton
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.util.formatIsoDateForDisplay
import com.vandoliak.coupleapp.presentation.viewmodel.TaskViewModel

@Composable
fun TaskScreen(
    modifier: Modifier = Modifier
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

    val currentUserId = viewModel.currentUserId.value
    val tasks = viewModel.tasks.value
    val assignedToMe = tasks.filter { it.status == "ACTIVE" && it.assignedTo.id == currentUserId }
    val createdByMe = tasks.filter { it.status == "ACTIVE" && it.createdBy.id == currentUserId }
    val waitingForPartner = tasks.filter { it.status == "WAITING_CONFIRMATION" && it.completionRequestedBy?.id == currentUserId }
    val waitingForMyDecision = tasks.filter { it.status == "WAITING_CONFIRMATION" && it.completionRequestedBy?.id != currentUserId }
    val history = tasks.filter { it.status == "COMPLETED" || it.status == "FAILED" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(
            title = "Arena",
            subtitle = "Manage live challenges while Calendar stays your planning view."
        )

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Your points: ${viewModel.currentUserPoints.value}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Create a challenge, track what is assigned to you, and confirm partner wins.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(title = "Create Challenge")

                OutlinedTextField(
                    value = viewModel.taskTitle.value,
                    onValueChange = viewModel::onTaskTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                OutlinedTextField(
                    value = viewModel.taskPoints.value,
                    onValueChange = viewModel::onTaskPointsChange,
                    label = { Text("Points") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                OutlinedTextField(
                    value = viewModel.dueDate.value,
                    onValueChange = viewModel::onDueDateChange,
                    label = { Text("Due date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                PrimaryActionButton(
                    text = if (viewModel.isSubmitting.value) "Creating..." else "Create Challenge",
                    onClick = viewModel::createTask,
                    enabled = !viewModel.isSubmitting.value
                )
            }
        }

        viewModel.error.value?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        viewModel.successMessage.value?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (viewModel.isLoading.value && tasks.isEmpty()) {
            EmptyState(
                title = "Loading arena",
                subtitle = "Fetching the latest challenges for your pair."
            )
            Spacer(modifier = Modifier.height(8.dp))
            return@Column
        }

        ArenaSection(
            title = "Assigned to Me",
            subtitle = "Challenges I can finish, return, or fail.",
            tasks = assignedToMe,
            emptyTitle = "Nothing assigned right now",
            emptySubtitle = "New challenges from your partner will appear here.",
            currentUserId = currentUserId,
            isSubmitting = viewModel.isSubmitting.value,
            onRequestCompletion = viewModel::requestCompletion,
            onConfirmCompletion = viewModel::confirmCompletion,
            onRejectCompletion = viewModel::rejectCompletion,
            onReturn = viewModel::returnTask,
            onFail = viewModel::failTask
        )

        ArenaSection(
            title = "Created by Me",
            subtitle = "Live challenges waiting on my partner.",
            tasks = createdByMe,
            emptyTitle = "No active challenges from you",
            emptySubtitle = "Create a new one above when you're ready.",
            currentUserId = currentUserId,
            isSubmitting = viewModel.isSubmitting.value,
            onRequestCompletion = viewModel::requestCompletion,
            onConfirmCompletion = viewModel::confirmCompletion,
            onRejectCompletion = viewModel::rejectCompletion,
            onReturn = viewModel::returnTask,
            onFail = viewModel::failTask
        )

        ArenaSection(
            title = "Waiting for Partner Confirmation",
            subtitle = "I already requested completion.",
            tasks = waitingForPartner,
            emptyTitle = "No pending confirmations from you",
            emptySubtitle = "Request completion from an active challenge to see it here.",
            currentUserId = currentUserId,
            isSubmitting = viewModel.isSubmitting.value,
            onRequestCompletion = viewModel::requestCompletion,
            onConfirmCompletion = viewModel::confirmCompletion,
            onRejectCompletion = viewModel::rejectCompletion,
            onReturn = viewModel::returnTask,
            onFail = viewModel::failTask
        )

        ArenaSection(
            title = "Waiting for My Decision",
            subtitle = "Partner asked me to confirm completion.",
            tasks = waitingForMyDecision,
            emptyTitle = "No partner confirmations pending",
            emptySubtitle = "When your partner requests completion, it will show up here.",
            currentUserId = currentUserId,
            isSubmitting = viewModel.isSubmitting.value,
            onRequestCompletion = viewModel::requestCompletion,
            onConfirmCompletion = viewModel::confirmCompletion,
            onRejectCompletion = viewModel::rejectCompletion,
            onReturn = viewModel::returnTask,
            onFail = viewModel::failTask
        )

        ArenaSection(
            title = "History",
            subtitle = "Completed and failed challenges.",
            tasks = history,
            emptyTitle = "No history yet",
            emptySubtitle = "Resolved challenges will appear here.",
            currentUserId = currentUserId,
            isSubmitting = viewModel.isSubmitting.value,
            onRequestCompletion = viewModel::requestCompletion,
            onConfirmCompletion = viewModel::confirmCompletion,
            onRejectCompletion = viewModel::rejectCompletion,
            onReturn = viewModel::returnTask,
            onFail = viewModel::failTask
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ArenaSection(
    title: String,
    subtitle: String,
    tasks: List<TaskDto>,
    emptyTitle: String,
    emptySubtitle: String,
    currentUserId: String?,
    isSubmitting: Boolean,
    onRequestCompletion: (String) -> Unit,
    onConfirmCompletion: (String) -> Unit,
    onRejectCompletion: (String) -> Unit,
    onReturn: (String) -> Unit,
    onFail: (String) -> Unit
) {
    SectionTitle(title = title, subtitle = subtitle)

    if (tasks.isEmpty()) {
        EmptyState(
            title = emptyTitle,
            subtitle = emptySubtitle
        )
    } else {
        tasks.forEach { task ->
            TaskItem(
                task = task,
                currentUserId = currentUserId,
                isSubmitting = isSubmitting,
                onRequestCompletion = { onRequestCompletion(task.id) },
                onConfirmCompletion = { onConfirmCompletion(task.id) },
                onRejectCompletion = { onRejectCompletion(task.id) },
                onReturn = { onReturn(task.id) },
                onFail = { onFail(task.id) }
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: TaskDto,
    currentUserId: String?,
    isSubmitting: Boolean,
    onRequestCompletion: () -> Unit,
    onConfirmCompletion: () -> Unit,
    onRejectCompletion: () -> Unit,
    onReturn: () -> Unit,
    onFail: () -> Unit
) {
    val isAssignedToCurrentUser = currentUserId == task.assignedTo.id
    val isCreatedByCurrentUser = currentUserId == task.createdBy.id
    val completionRequestedByCurrentUser = currentUserId == task.completionRequestedBy?.id
    val dueDate = formatIsoDateForDisplay(task.dueDate)

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = "Bank: ${task.bank} points")
            Text(text = "Status: ${formatTaskStatus(task.status, completionRequestedByCurrentUser)}")
            Text(text = "Created by: ${task.createdBy.email}")
            Text(text = "Assigned to: ${task.assignedTo.email}")
            task.completionRequestedBy?.let {
                Text(text = "Completion requested by: ${it.email}")
            }
            dueDate?.let {
                Text(text = "Due date: $it")
            }

            when {
                task.status == "ACTIVE" && isAssignedToCurrentUser -> {
                    PrimaryActionButton(
                        text = "Request Complete",
                        onClick = onRequestCompletion,
                        enabled = !isSubmitting
                    )
                    OutlinedButton(
                        onClick = onReturn,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        Text("Return")
                    }
                    OutlinedButton(
                        onClick = onFail,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        Text("Fail")
                    }
                }

                task.status == "WAITING_CONFIRMATION" && !completionRequestedByCurrentUser -> {
                    Text(
                        text = "Partner requested completion",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    PrimaryActionButton(
                        text = "Confirm",
                        onClick = onConfirmCompletion,
                        enabled = !isSubmitting
                    )
                    OutlinedButton(
                        onClick = onRejectCompletion,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        Text("Reject")
                    }
                }

                task.status == "WAITING_CONFIRMATION" && completionRequestedByCurrentUser -> {
                    Text(
                        text = "Waiting for partner confirmation",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                task.status == "ACTIVE" && isCreatedByCurrentUser -> {
                    Text(
                        text = "Challenge is active and waiting on your partner.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun formatTaskStatus(status: String, requestedByCurrentUser: Boolean): String {
    return when {
        status == "WAITING_CONFIRMATION" && requestedByCurrentUser -> "Waiting for partner confirmation"
        status == "WAITING_CONFIRMATION" -> "Partner requested completion"
        else -> status.lowercase().replaceFirstChar { it.uppercase() }
    }
}
