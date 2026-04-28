package com.vandoliak.coupleapp.presentation.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.vandoliak.coupleapp.presentation.components.CouplePointsHeader
import com.vandoliak.coupleapp.presentation.components.EmptyState
import com.vandoliak.coupleapp.presentation.components.PointBadge
import com.vandoliak.coupleapp.presentation.components.PrimaryActionButton
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.components.SelectionChip
import com.vandoliak.coupleapp.presentation.util.formatIsoDateForDisplay
import com.vandoliak.coupleapp.presentation.util.recurrenceLabel
import com.vandoliak.coupleapp.presentation.util.taskTypeLabel
import com.vandoliak.coupleapp.presentation.util.taskStatusLabel
import com.vandoliak.coupleapp.presentation.viewmodel.TaskViewModel

private enum class SharedSplitMode {
    PROPOSE,
    COUNTER
}

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
    var showCreateDialog by remember { mutableStateOf(false) }
    var splitDialogTask by remember { mutableStateOf<TaskDto?>(null) }
    var splitDialogMode by remember { mutableStateOf(SharedSplitMode.PROPOSE) }

    LaunchedEffect(Unit) {
        viewModel.loadTasks()
    }

    val currentUserId = viewModel.currentUserId.value
    val partnerId = viewModel.partner.value?.id
    val tasks = viewModel.tasks.value
    val challengeTasks = tasks.filter { it.type == "CHALLENGE" }
    val sharedTasks = tasks.filter { it.type == "SHARED" && it.status != "COMPLETED" && it.status != "FAILED" }
    val assignedToMe = challengeTasks.filter { it.status == "ACTIVE" && it.assignedTo?.id == currentUserId }
    val createdByMe = challengeTasks.filter { it.status == "ACTIVE" && it.createdBy.id == currentUserId }
    val waitingForPartner = challengeTasks.filter { it.status == "WAITING_CONFIRMATION" && it.completionRequestedBy?.id == currentUserId }
    val waitingForMyDecision = challengeTasks.filter { it.status == "WAITING_CONFIRMATION" && it.completionRequestedBy?.id != currentUserId }
    val history = tasks.filter { it.status == "COMPLETED" || it.status == "FAILED" }

    if (showCreateDialog) {
        CreateChallengeDialog(
            viewModel = viewModel,
            onDismiss = {
                showCreateDialog = false
                viewModel.resetCreateForm()
            },
            onCreate = {
                viewModel.createTask {
                    showCreateDialog = false
                }
            }
        )
    }

    splitDialogTask?.let { task ->
        SharedSplitDialog(
            task = task,
            currentUserId = currentUserId,
            partnerId = partnerId,
            mode = splitDialogMode,
            errorMessage = viewModel.error.value,
            isSubmitting = viewModel.isSubmitting.value,
            onDismiss = {
                splitDialogTask = null
                viewModel.error.value = null
            },
            onSubmit = { myPoints, partnerPoints ->
                when (splitDialogMode) {
                    SharedSplitMode.PROPOSE -> viewModel.proposeSharedSplit(
                        taskId = task.id,
                        myPoints = myPoints,
                        partnerPoints = partnerPoints,
                        onSuccess = { splitDialogTask = null }
                    )
                    SharedSplitMode.COUNTER -> viewModel.counterSharedSplit(
                        taskId = task.id,
                        myPoints = myPoints,
                        partnerPoints = partnerPoints,
                        onSuccess = { splitDialogTask = null }
                    )
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.resetCreateForm()
                showCreateDialog = true
            }) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.create_challenge_button))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(
                title = stringResource(R.string.challenges_title),
                subtitle = stringResource(R.string.challenges_subtitle)
            )

            CouplePointsHeader(
                myLabel = stringResource(R.string.my_points_label),
                myPoints = viewModel.currentUserPoints.value,
                partnerLabel = viewModel.partner.value?.let { partner ->
                    partner.nickname ?: stringResource(R.string.partner_points_label)
                },
                partnerPoints = viewModel.partner.value?.points,
                subtitle = stringResource(R.string.points_header_subtitle)
            )

            Text(
                text = stringResource(R.string.challenges_overview_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            viewModel.error.value?.takeIf { it.isNotBlank() && !showCreateDialog && splitDialogTask == null }?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            viewModel.successMessage.value?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
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
                title = stringResource(R.string.shared_tasks_title),
                subtitle = stringResource(R.string.shared_tasks_subtitle),
                tasks = sharedTasks,
                emptyTitle = stringResource(R.string.shared_tasks_empty_title),
                emptySubtitle = stringResource(R.string.shared_tasks_empty_subtitle),
                currentUserId = currentUserId,
                partnerId = partnerId,
                isSubmitting = viewModel.isSubmitting.value,
                onRequestCompletion = viewModel::requestCompletion,
                onConfirmCompletion = viewModel::confirmCompletion,
                onRejectCompletion = viewModel::rejectCompletion,
                onReturn = viewModel::returnTask,
                onFail = viewModel::failTask,
                onOpenSharedSplit = { task, mode ->
                    splitDialogTask = task
                    splitDialogMode = mode
                },
                onAcceptSharedSplit = viewModel::acceptSharedSplit
            )

            ArenaSection(
                title = stringResource(R.string.assigned_to_me_title),
                subtitle = stringResource(R.string.assigned_to_me_subtitle),
                tasks = assignedToMe,
                emptyTitle = stringResource(R.string.assigned_to_me_empty_title),
                emptySubtitle = stringResource(R.string.assigned_to_me_empty_subtitle),
                currentUserId = currentUserId,
                partnerId = partnerId,
                isSubmitting = viewModel.isSubmitting.value,
                onRequestCompletion = viewModel::requestCompletion,
                onConfirmCompletion = viewModel::confirmCompletion,
                onRejectCompletion = viewModel::rejectCompletion,
                onReturn = viewModel::returnTask,
                onFail = viewModel::failTask,
                onOpenSharedSplit = { task, mode ->
                    splitDialogTask = task
                    splitDialogMode = mode
                },
                onAcceptSharedSplit = viewModel::acceptSharedSplit
            )

            ArenaSection(
                title = stringResource(R.string.created_by_me_title),
                subtitle = stringResource(R.string.created_by_me_subtitle),
                tasks = createdByMe,
                emptyTitle = stringResource(R.string.created_by_me_empty_title),
                emptySubtitle = stringResource(R.string.created_by_me_empty_subtitle),
                currentUserId = currentUserId,
                partnerId = partnerId,
                isSubmitting = viewModel.isSubmitting.value,
                onRequestCompletion = viewModel::requestCompletion,
                onConfirmCompletion = viewModel::confirmCompletion,
                onRejectCompletion = viewModel::rejectCompletion,
                onReturn = viewModel::returnTask,
                onFail = viewModel::failTask,
                onOpenSharedSplit = { task, mode ->
                    splitDialogTask = task
                    splitDialogMode = mode
                },
                onAcceptSharedSplit = viewModel::acceptSharedSplit
            )

            ArenaSection(
                title = stringResource(R.string.waiting_for_partner_title),
                subtitle = stringResource(R.string.waiting_for_partner_subtitle),
                tasks = waitingForPartner,
                emptyTitle = stringResource(R.string.waiting_for_partner_empty_title),
                emptySubtitle = stringResource(R.string.waiting_for_partner_empty_subtitle),
                currentUserId = currentUserId,
                partnerId = partnerId,
                isSubmitting = viewModel.isSubmitting.value,
                onRequestCompletion = viewModel::requestCompletion,
                onConfirmCompletion = viewModel::confirmCompletion,
                onRejectCompletion = viewModel::rejectCompletion,
                onReturn = viewModel::returnTask,
                onFail = viewModel::failTask,
                onOpenSharedSplit = { task, mode ->
                    splitDialogTask = task
                    splitDialogMode = mode
                },
                onAcceptSharedSplit = viewModel::acceptSharedSplit
            )

            ArenaSection(
                title = stringResource(R.string.waiting_for_my_decision_title),
                subtitle = stringResource(R.string.waiting_for_my_decision_subtitle),
                tasks = waitingForMyDecision,
                emptyTitle = stringResource(R.string.waiting_for_my_decision_empty_title),
                emptySubtitle = stringResource(R.string.waiting_for_my_decision_empty_subtitle),
                currentUserId = currentUserId,
                partnerId = partnerId,
                isSubmitting = viewModel.isSubmitting.value,
                onRequestCompletion = viewModel::requestCompletion,
                onConfirmCompletion = viewModel::confirmCompletion,
                onRejectCompletion = viewModel::rejectCompletion,
                onReturn = viewModel::returnTask,
                onFail = viewModel::failTask,
                onOpenSharedSplit = { task, mode ->
                    splitDialogTask = task
                    splitDialogMode = mode
                },
                onAcceptSharedSplit = viewModel::acceptSharedSplit
            )

            ArenaSection(
                title = stringResource(R.string.challenge_history_title),
                subtitle = stringResource(R.string.challenge_history_subtitle),
                tasks = history,
                emptyTitle = stringResource(R.string.challenge_history_empty_title),
                emptySubtitle = stringResource(R.string.challenge_history_empty_subtitle),
                currentUserId = currentUserId,
                partnerId = partnerId,
                isSubmitting = viewModel.isSubmitting.value,
                onRequestCompletion = viewModel::requestCompletion,
                onConfirmCompletion = viewModel::confirmCompletion,
                onRejectCompletion = viewModel::rejectCompletion,
                onReturn = viewModel::returnTask,
                onFail = viewModel::failTask,
                onOpenSharedSplit = { task, mode ->
                    splitDialogTask = task
                    splitDialogMode = mode
                },
                onAcceptSharedSplit = viewModel::acceptSharedSplit
            )

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun CreateChallengeDialog(
    viewModel: TaskViewModel,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_challenge_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                viewModel.error.value?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("CHALLENGE", "SHARED").forEach { value ->
                        SelectionChip(
                            label = LocalContext.current.taskTypeLabel(value),
                            selected = viewModel.taskType.value == value,
                            onClick = { viewModel.onTaskTypeChange(value) },
                            enabled = !viewModel.isSubmitting.value
                        )
                    }
                }

                Text(
                    text = if (viewModel.taskType.value == "SHARED") {
                        stringResource(R.string.shared_task_explainer)
                    } else {
                        stringResource(R.string.challenge_task_explainer)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCreate,
                enabled = !viewModel.isSubmitting.value
            ) {
                Text(if (viewModel.isSubmitting.value) stringResource(R.string.creating) else stringResource(R.string.create_challenge_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ArenaSection(
    title: String,
    subtitle: String,
    tasks: List<TaskDto>,
    emptyTitle: String,
    emptySubtitle: String,
    currentUserId: String?,
    partnerId: String?,
    isSubmitting: Boolean,
    onRequestCompletion: (String) -> Unit,
    onConfirmCompletion: (String) -> Unit,
    onRejectCompletion: (String) -> Unit,
    onReturn: (String) -> Unit,
    onFail: (String) -> Unit,
    onOpenSharedSplit: (TaskDto, SharedSplitMode) -> Unit,
    onAcceptSharedSplit: (String) -> Unit
) {
    SectionTitle(title = title, subtitle = subtitle)

    if (tasks.isEmpty()) {
        EmptyState(title = emptyTitle, subtitle = emptySubtitle)
    } else {
        tasks.forEach { task ->
            TaskItem(
                task = task,
                currentUserId = currentUserId,
                partnerId = partnerId,
                isSubmitting = isSubmitting,
                onRequestCompletion = { onRequestCompletion(task.id) },
                onConfirmCompletion = { onConfirmCompletion(task.id) },
                onRejectCompletion = { onRejectCompletion(task.id) },
                onReturn = { onReturn(task.id) },
                onFail = { onFail(task.id) },
                onOpenSharedSplit = { mode -> onOpenSharedSplit(task, mode) },
                onAcceptSharedSplit = { onAcceptSharedSplit(task.id) }
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: TaskDto,
    currentUserId: String?,
    partnerId: String?,
    isSubmitting: Boolean,
    onRequestCompletion: () -> Unit,
    onConfirmCompletion: () -> Unit,
    onRejectCompletion: () -> Unit,
    onReturn: () -> Unit,
    onFail: () -> Unit,
    onOpenSharedSplit: (SharedSplitMode) -> Unit,
    onAcceptSharedSplit: () -> Unit
) {
    val context = LocalContext.current
    val isAssignedToCurrentUser = currentUserId == task.assignedTo?.id
    val isCreatedByCurrentUser = currentUserId == task.createdBy.id
    val waitingActionRequestedByCurrentUser = if (task.type == "SHARED") {
        currentUserId == task.proposedBy?.id
    } else {
        currentUserId == task.completionRequestedBy?.id
    }
    val dueDate = formatIsoDateForDisplay(task.dueDate)
    val sharedSplit = task.sharedSplitForViewer(currentUserId, partnerId)

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = context.taskTypeLabel(task.type),
                color = if (task.type == "SHARED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
            )
            PointBadge(points = task.bank)
            Text(text = stringResource(R.string.status_format, context.taskStatusLabel(task.status, waitingActionRequestedByCurrentUser, task.type)))
            Text(text = stringResource(R.string.created_by, task.createdBy.email))
            task.assignedTo?.let {
                Text(text = stringResource(R.string.assigned_to_format, it.email))
            }
            task.completionRequestedBy?.let {
                Text(text = stringResource(R.string.completion_requested_by_format, it.email))
            }
            dueDate?.let {
                Text(text = stringResource(R.string.due_date_format, it))
            }
            Text(text = stringResource(R.string.recurrence_format, context.recurrenceLabel(task.recurrenceType, task.recurrenceInterval)))

            if (task.type == "SHARED") {
                sharedSplit?.let { (myPoints, partnerPoints) ->
                    Text(
                        text = if (task.proposedBy?.id == currentUserId) {
                            stringResource(R.string.shared_split_waiting_breakdown, myPoints, partnerPoints)
                        } else {
                            stringResource(R.string.shared_split_partner_breakdown, myPoints, partnerPoints)
                        },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                when {
                    task.status == "ACTIVE" -> {
                        PrimaryActionButton(
                            text = stringResource(R.string.propose_completion_split),
                            onClick = { onOpenSharedSplit(SharedSplitMode.PROPOSE) },
                            enabled = !isSubmitting
                        )
                    }

                    task.status == "WAITING_CONFIRMATION" && task.proposedBy?.id != currentUserId -> {
                        PrimaryActionButton(
                            text = stringResource(R.string.accept_split),
                            onClick = onAcceptSharedSplit,
                            enabled = !isSubmitting
                        )
                        OutlinedButton(
                            onClick = { onOpenSharedSplit(SharedSplitMode.COUNTER) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSubmitting
                        ) {
                            Text(stringResource(R.string.counter_split))
                        }
                    }

                    task.status == "WAITING_CONFIRMATION" -> {
                        Text(
                            text = stringResource(R.string.shared_split_waiting_partner),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                return@Column
            }

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

                task.status == "WAITING_CONFIRMATION" && !waitingActionRequestedByCurrentUser -> {
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

                task.status == "WAITING_CONFIRMATION" && waitingActionRequestedByCurrentUser -> {
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
private fun SharedSplitDialog(
    task: TaskDto,
    currentUserId: String?,
    partnerId: String?,
    mode: SharedSplitMode,
    errorMessage: String?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int) -> Unit
) {
    val initialSplit = task.sharedSplitForViewer(currentUserId, partnerId)
        ?: defaultSharedSplit(task.bank)
    var myPoints by remember(task.id, mode) { mutableStateOf(initialSplit.first.toString()) }
    var partnerPoints by remember(task.id, mode) { mutableStateOf(initialSplit.second.toString()) }
    val localError = validateSharedSplitInput(task.bank, myPoints, partnerPoints)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shared_split_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.shared_split_subtitle, task.bank))

                errorMessage?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                localError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = myPoints,
                    onValueChange = { myPoints = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.my_reward_points)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                )

                OutlinedTextField(
                    value = partnerPoints,
                    onValueChange = { partnerPoints = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.partner_reward_points)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val myValue = myPoints.toIntOrNull() ?: return@TextButton
                    val partnerValue = partnerPoints.toIntOrNull() ?: return@TextButton
                    onSubmit(myValue, partnerValue)
                },
                enabled = !isSubmitting && localError == null
            ) {
                Text(
                    if (mode == SharedSplitMode.COUNTER) {
                        stringResource(R.string.counter_split)
                    } else {
                        stringResource(R.string.propose_completion_split)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun TaskDto.sharedSplitForViewer(currentUserId: String?, partnerId: String?): Pair<Int, Int>? {
    val myId = currentUserId ?: return null
    val otherId = partnerId ?: return null
    val first = proposedUser1Points ?: return null
    val second = proposedUser2Points ?: return null
    val ordered = listOf(myId, otherId).sorted()
    return if (ordered.first() == myId) {
        first to second
    } else {
        second to first
    }
}

private fun defaultSharedSplit(bank: Int): Pair<Int, Int> {
    val myPoints = (bank + 1) / 2
    return myPoints to (bank - myPoints)
}

@Composable
private fun validateSharedSplitInput(bank: Int, myPoints: String, partnerPoints: String): String? {
    val myValue = myPoints.toIntOrNull()
    val partnerValue = partnerPoints.toIntOrNull()
    if (myValue == null || partnerValue == null || myValue < 0 || partnerValue < 0) {
        return stringResource(R.string.points_positive)
    }
    if (myValue + partnerValue != bank) {
        return stringResource(R.string.shared_split_sum_mismatch)
    }
    return null
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
            listOf("NONE", "EVERY_X_DAYS", "WEEKLY", "MONTHLY").forEach { value ->
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
