package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.remote.TaskDto
import com.vandoliak.coupleapp.presentation.components.AppCard
import com.vandoliak.coupleapp.presentation.components.EmptyState
import com.vandoliak.coupleapp.presentation.components.PrimaryActionButton
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.components.SelectionChip
import com.vandoliak.coupleapp.presentation.util.formatIsoDateForDisplay
import com.vandoliak.coupleapp.presentation.util.recurrenceLabel
import com.vandoliak.coupleapp.presentation.util.taskStatusLabel
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
            title = stringResource(R.string.challenges_title),
            subtitle = stringResource(R.string.challenges_subtitle)
        )

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.your_points_format, viewModel.currentUserPoints.value),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.challenges_overview_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.my_points_format, viewModel.currentUserPoints.value),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = viewModel.partner.value?.let {
                        val label = it.nickname ?: it.email
                        stringResource(R.string.partner_points_format, label, it.points)
                    } ?: stringResource(R.string.partner_points_unavailable),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(title = stringResource(R.string.create_challenge_title))

                OutlinedTextField(
                    value = viewModel.taskTitle.value,
                    onValueChange = viewModel::onTaskTitleChange,
                    label = { Text(stringResource(R.string.title)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                OutlinedTextField(
                    value = viewModel.taskPoints.value,
                    onValueChange = viewModel::onTaskPointsChange,
                    label = { Text(stringResource(R.string.points)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                OutlinedTextField(
                    value = viewModel.dueDate.value,
                    onValueChange = viewModel::onDueDateChange,
                    label = { Text(stringResource(R.string.due_date_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSubmitting.value
                )

                RecurrenceSelector(
                    recurrenceType = viewModel.recurrenceType.value,
                    recurrenceInterval = viewModel.recurrenceInterval.value,
                    isSubmitting = viewModel.isSubmitting.value,
                    onRecurrenceTypeChange = viewModel::onRecurrenceTypeChange,
                    onRecurrenceIntervalChange = viewModel::onRecurrenceIntervalChange
                )

                PrimaryActionButton(
                    text = if (viewModel.isSubmitting.value) stringResource(R.string.creating) else stringResource(R.string.create_challenge_button),
                    onClick = viewModel::createTask,
                    enabled = !viewModel.isSubmitting.value
                )
            }
        }

        viewModel.error.value?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        viewModel.successMessage.value?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (viewModel.isLoading.value && tasks.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.loading_challenges_title),
                subtitle = stringResource(R.string.loading_challenges_subtitle)
            )
            Spacer(modifier = Modifier.height(8.dp))
            return@Column
        }

        ArenaSection(
            title = stringResource(R.string.assigned_to_me_title),
            subtitle = stringResource(R.string.assigned_to_me_subtitle),
            tasks = assignedToMe,
            emptyTitle = stringResource(R.string.assigned_to_me_empty_title),
            emptySubtitle = stringResource(R.string.assigned_to_me_empty_subtitle),
            currentUserId = currentUserId,
            isSubmitting = viewModel.isSubmitting.value,
            onRequestCompletion = viewModel::requestCompletion,
            onConfirmCompletion = viewModel::confirmCompletion,
            onRejectCompletion = viewModel::rejectCompletion,
            onReturn = viewModel::returnTask,
            onFail = viewModel::failTask
        )

        ArenaSection(
            title = stringResource(R.string.created_by_me_title),
            subtitle = stringResource(R.string.created_by_me_subtitle),
            tasks = createdByMe,
            emptyTitle = stringResource(R.string.created_by_me_empty_title),
            emptySubtitle = stringResource(R.string.created_by_me_empty_subtitle),
            currentUserId = currentUserId,
            isSubmitting = viewModel.isSubmitting.value,
            onRequestCompletion = viewModel::requestCompletion,
            onConfirmCompletion = viewModel::confirmCompletion,
            onRejectCompletion = viewModel::rejectCompletion,
            onReturn = viewModel::returnTask,
            onFail = viewModel::failTask
        )

        ArenaSection(
            title = stringResource(R.string.waiting_for_partner_title),
            subtitle = stringResource(R.string.waiting_for_partner_subtitle),
            tasks = waitingForPartner,
            emptyTitle = stringResource(R.string.waiting_for_partner_empty_title),
            emptySubtitle = stringResource(R.string.waiting_for_partner_empty_subtitle),
            currentUserId = currentUserId,
            isSubmitting = viewModel.isSubmitting.value,
            onRequestCompletion = viewModel::requestCompletion,
            onConfirmCompletion = viewModel::confirmCompletion,
            onRejectCompletion = viewModel::rejectCompletion,
            onReturn = viewModel::returnTask,
            onFail = viewModel::failTask
        )

        ArenaSection(
            title = stringResource(R.string.waiting_for_my_decision_title),
            subtitle = stringResource(R.string.waiting_for_my_decision_subtitle),
            tasks = waitingForMyDecision,
            emptyTitle = stringResource(R.string.waiting_for_my_decision_empty_title),
            emptySubtitle = stringResource(R.string.waiting_for_my_decision_empty_subtitle),
            currentUserId = currentUserId,
            isSubmitting = viewModel.isSubmitting.value,
            onRequestCompletion = viewModel::requestCompletion,
            onConfirmCompletion = viewModel::confirmCompletion,
            onRejectCompletion = viewModel::rejectCompletion,
            onReturn = viewModel::returnTask,
            onFail = viewModel::failTask
        )

        ArenaSection(
            title = stringResource(R.string.challenge_history_title),
            subtitle = stringResource(R.string.challenge_history_subtitle),
            tasks = history,
            emptyTitle = stringResource(R.string.challenge_history_empty_title),
            emptySubtitle = stringResource(R.string.challenge_history_empty_subtitle),
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
    val context = LocalContext.current
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
            Text(text = stringResource(R.string.bank_points_format, task.bank))
            Text(text = stringResource(R.string.status_format, context.taskStatusLabel(task.status, completionRequestedByCurrentUser)))
            Text(text = stringResource(R.string.created_by, task.createdBy.email))
            Text(text = stringResource(R.string.assigned_to_format, task.assignedTo.email))
            task.completionRequestedBy?.let {
                Text(text = stringResource(R.string.completion_requested_by_format, it.email))
            }
            dueDate?.let {
                Text(text = stringResource(R.string.due_date_format, it))
            }
            Text(text = stringResource(R.string.recurrence_format, context.recurrenceLabel(task.recurrenceType, task.recurrenceInterval)))

            when {
                task.status == "ACTIVE" && isAssignedToCurrentUser -> {
                    PrimaryActionButton(
                        text = stringResource(R.string.request_complete),
                        onClick = onRequestCompletion,
                        enabled = !isSubmitting
                    )
                    OutlinedButton(
                        onClick = onReturn,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        Text(stringResource(R.string.return_task))
                    }
                    OutlinedButton(
                        onClick = onFail,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        Text(stringResource(R.string.fail_task))
                    }
                }

                task.status == "WAITING_CONFIRMATION" && !completionRequestedByCurrentUser -> {
                    Text(
                        text = stringResource(R.string.partner_requested_completion),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    PrimaryActionButton(
                        text = stringResource(R.string.confirm),
                        onClick = onConfirmCompletion,
                        enabled = !isSubmitting
                    )
                    OutlinedButton(
                        onClick = onRejectCompletion,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting
                    ) {
                        Text(stringResource(R.string.reject))
                    }
                }

                task.status == "WAITING_CONFIRMATION" && completionRequestedByCurrentUser -> {
                    Text(
                        text = stringResource(R.string.waiting_for_partner_confirmation),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                task.status == "ACTIVE" && isCreatedByCurrentUser -> {
                    Text(
                        text = stringResource(R.string.challenge_active_waiting_partner),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurrenceSelector(
    recurrenceType: String,
    recurrenceInterval: String,
    isSubmitting: Boolean,
    onRecurrenceTypeChange: (String) -> Unit,
    onRecurrenceIntervalChange: (String) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(
            title = stringResource(R.string.recurrence_title),
            subtitle = stringResource(R.string.recurrence_subtitle)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "NONE",
                "EVERY_X_DAYS",
                "WEEKLY",
                "MONTHLY"
            ).forEach { value ->
                SelectionChip(
                    label = context.recurrenceLabel(value),
                    selected = recurrenceType == value,
                    onClick = { onRecurrenceTypeChange(value) },
                    enabled = !isSubmitting
                )
            }
        }

        if (recurrenceType == "EVERY_X_DAYS") {
            OutlinedTextField(
                value = recurrenceInterval,
                onValueChange = onRecurrenceIntervalChange,
                label = { Text(stringResource(R.string.interval_in_days)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            )
        }
    }
}
