package com.vandoliak.coupleapp.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.data.remote.BlueprintDto
import com.vandoliak.coupleapp.presentation.components.EmptyState
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.util.formatFullDate
import com.vandoliak.coupleapp.presentation.util.formatMonthTitle
import com.vandoliak.coupleapp.presentation.util.toDateInput
import com.vandoliak.coupleapp.presentation.viewmodel.BlueprintViewModel
import com.vandoliak.coupleapp.presentation.viewmodel.CalendarDayUi
import com.vandoliak.coupleapp.presentation.viewmodel.CalendarItemType
import com.vandoliak.coupleapp.presentation.viewmodel.CalendarItemUi
import com.vandoliak.coupleapp.presentation.viewmodel.CalendarViewModel

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier
) {
    val calendarViewModel: CalendarViewModel = viewModel()
    val blueprintViewModel: BlueprintViewModel = viewModel()

    LaunchedEffect(Unit) {
        calendarViewModel.loadCalendar()
        blueprintViewModel.loadBlueprints()
    }

    if (calendarViewModel.isDaySheetVisible.value) {
        ModalBottomSheet(
            onDismissRequest = calendarViewModel::dismissDaySheet
        ) {
            SelectedDaySheet(
                calendarViewModel = calendarViewModel,
                blueprintViewModel = blueprintViewModel
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val cellHeight = if (maxWidth > maxHeight || maxWidth >= 600.dp) 120.dp else 96.dp
        val days = calendarViewModel.monthDays.value
        val weekdayLabels = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 16.dp)
        ) {
            item(span = { GridItemSpan(7) }) {
                CalendarHeader(
                    monthTitle = formatMonthTitle(calendarViewModel.visibleMonth.value),
                    onPrevious = calendarViewModel::goToPreviousMonth,
                    onNext = calendarViewModel::goToNextMonth
                )
            }

            item(span = { GridItemSpan(7) }) {
                calendarViewModel.error.value?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            items(weekdayLabels, key = { it }) { label ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (calendarViewModel.isLoading.value && days.isEmpty()) {
                item(span = { GridItemSpan(7) }) {
                    EmptyState(
                        title = "Loading calendar...",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                items(days, key = { it.date.toString() }) { day ->
                    CalendarDayCell(
                        day = day,
                        cellHeight = cellHeight,
                        onClick = { calendarViewModel.onDaySelected(day.date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    monthTitle: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPrevious) {
            Text("<")
        }

        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        TextButton(onClick = onNext) {
            Text(">")
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDayUi,
    cellHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = when {
        day.isToday -> colors.primary.copy(alpha = 0.45f)
        else -> colors.outlineVariant.copy(alpha = 0.75f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cellHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (day.isInCurrentMonth) colors.surface else colors.surfaceVariant.copy(alpha = 0.28f)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DayNumber(day = day)

            day.previews.forEach { item ->
                CompactPreviewChip(item = item)
            }

            if (day.remainingCount > 0) {
                Text(
                    text = "+${day.remainingCount}",
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 8.sp,
                    lineHeight = 8.sp
                )
            }
        }
    }
}

@Composable
private fun DayNumber(day: CalendarDayUi) {
    val colors = MaterialTheme.colorScheme
    val textColor = when {
        day.isSelected -> colors.onPrimary
        day.isToday -> colors.primary
        day.isInCurrentMonth -> colors.onSurface
        else -> colors.onSurfaceVariant
    }

    Box(
        modifier = Modifier.size(22.dp),
        contentAlignment = Alignment.Center
    ) {
        if (day.isSelected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
            )
        }

        Text(
            text = day.date.dayOfMonth.toString(),
            fontSize = 11.sp,
            lineHeight = 11.sp,
            fontWeight = if (day.isToday || day.isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun CompactPreviewChip(item: CalendarItemUi) {
    val colors = MaterialTheme.colorScheme
    val backgroundColor = if (item.type == CalendarItemType.TASK) {
        colors.tertiaryContainer.copy(alpha = 0.95f)
    } else {
        colors.primaryContainer.copy(alpha = 0.85f)
    }
    val textColor = if (item.type == CalendarItemType.TASK) {
        colors.onTertiaryContainer
    } else {
        colors.onPrimaryContainer
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 16.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 3.dp, vertical = 1.dp)
    ) {
        Text(
            text = if (item.type == CalendarItemType.TASK) {
                "${item.title} ${item.defaultPoints ?: 0}"
            } else {
                item.title
            },
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 8.sp,
            lineHeight = 8.sp
        )
    }
}

@Composable
private fun SelectedDaySheet(
    calendarViewModel: CalendarViewModel,
    blueprintViewModel: BlueprintViewModel
) {
    val selectedDate = calendarViewModel.selectedDate.value
    val selectedItems = calendarViewModel.selectedDayItems.value
    val currentUserId = calendarViewModel.currentUserId.value
    val scrollState = rememberScrollState()
    var showBlueprints by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<CalendarItemUi?>(null) }
    var deletingItem by remember { mutableStateOf<CalendarItemUi?>(null) }

    editingItem?.let { item ->
        if (item.type == CalendarItemType.EVENT) {
            EditEventDialog(
                item = item,
                onDismiss = { editingItem = null },
                onSave = { title, description, dateInput ->
                    calendarViewModel.updateEvent(
                        eventId = item.sourceId,
                        title = title,
                        description = description,
                        dateInput = dateInput
                    )
                    editingItem = null
                }
            )
        } else {
            EditTaskDialog(
                item = item,
                onDismiss = { editingItem = null },
                onSave = { title, dateInput ->
                    calendarViewModel.updateTask(
                        taskId = item.sourceId,
                        title = title,
                        dateInput = dateInput
                    )
                    editingItem = null
                }
            )
        }
    }

    deletingItem?.let { item ->
        DeleteItemDialog(
            item = item,
            onDismiss = { deletingItem = null },
            onConfirm = {
                if (item.type == CalendarItemType.EVENT) {
                    calendarViewModel.deleteEvent(item.sourceId)
                } else {
                    calendarViewModel.deleteTask(item.sourceId)
                }
                deletingItem = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle(
            title = formatFullDate(selectedDate),
            subtitle = "Tasks and events planned for this day"
        )

        calendarViewModel.successMessage.value?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        calendarViewModel.error.value?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        blueprintViewModel.successMessage.value?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        blueprintViewModel.error.value?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (selectedItems.isEmpty()) {
            EmptyState(
                title = "Nothing planned yet",
                subtitle = "Create a task, add an event, or use a blueprint"
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                selectedItems.forEach { item ->
                    DayDetailItemCard(
                        item = item,
                        currentUserId = currentUserId,
                        isBusy = calendarViewModel.isSubmitting.value || blueprintViewModel.isSubmitting.value,
                        onSaveAsBlueprint = {
                            blueprintViewModel.createBlueprint(
                                title = item.title,
                                description = item.description,
                                type = if (item.type == CalendarItemType.TASK) "TASK" else "EVENT",
                                defaultPoints = item.defaultPoints,
                                defaultTime = null
                            )
                        },
                        onEdit = { editingItem = item },
                        onDelete = { deletingItem = item },
                        onRequestCompletion = { calendarViewModel.requestTaskCompletion(item.sourceId) },
                        onConfirmCompletion = { calendarViewModel.confirmTaskCompletion(item.sourceId) },
                        onRejectCompletion = { calendarViewModel.rejectTaskCompletion(item.sourceId) },
                        onReturn = { calendarViewModel.returnTask(item.sourceId) },
                        onFail = { calendarViewModel.failTask(item.sourceId) }
                    )
                }
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(
                title = "Create event",
                subtitle = "Quick reminder for this date"
            )

            OutlinedTextField(
                value = calendarViewModel.eventTitle.value,
                onValueChange = calendarViewModel::onEventTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true
            )

            OutlinedTextField(
                value = calendarViewModel.eventDescription.value,
                onValueChange = calendarViewModel::onEventDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description") },
                maxLines = 3
            )

            Button(
                onClick = calendarViewModel::createEventForSelectedDay,
                modifier = Modifier.fillMaxWidth(),
                enabled = !calendarViewModel.isSubmitting.value && !blueprintViewModel.isSubmitting.value
            ) {
                Text("Create Event")
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(
                title = "Create task",
                subtitle = "Challenge your partner on this day"
            )

            OutlinedTextField(
                value = calendarViewModel.taskTitle.value,
                onValueChange = calendarViewModel::onTaskTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true
            )

            OutlinedTextField(
                value = calendarViewModel.taskPoints.value,
                onValueChange = calendarViewModel::onTaskPointsChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Points") },
                singleLine = true
            )

            Button(
                onClick = calendarViewModel::createTaskForSelectedDay,
                modifier = Modifier.fillMaxWidth(),
                enabled = !calendarViewModel.isSubmitting.value && !blueprintViewModel.isSubmitting.value
            ) {
                Text("Create Task")
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionTitle(
                    title = "Blueprints",
                    subtitle = if (showBlueprints) "Tap one to use it on this date" else "Reusable task and event templates"
                )

                TextButton(onClick = { showBlueprints = !showBlueprints }) {
                    Text(if (showBlueprints) "Hide" else "Use Blueprint")
                }
            }

            if (showBlueprints) {
                if (blueprintViewModel.isLoading.value && blueprintViewModel.blueprints.value.isEmpty()) {
                    Text(
                        text = "Loading blueprints...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (blueprintViewModel.blueprints.value.isEmpty()) {
                    EmptyState(
                        title = "No blueprints yet",
                        subtitle = "Use Save as Blueprint on a task or event to build your library"
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        blueprintViewModel.blueprints.value.forEach { blueprint ->
                            BlueprintCard(
                                blueprint = blueprint,
                                onUse = {
                                    blueprintViewModel.useBlueprint(
                                        blueprintId = blueprint.id,
                                        date = calendarViewModel.selectedDateInput(),
                                        onSuccess = { calendarViewModel.loadCalendar() }
                                    )
                                },
                                onDelete = { blueprintViewModel.deleteBlueprint(blueprint.id) },
                                isBusy = blueprintViewModel.isSubmitting.value || calendarViewModel.isSubmitting.value
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailItemCard(
    item: CalendarItemUi,
    currentUserId: String?,
    isBusy: Boolean,
    onSaveAsBlueprint: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRequestCompletion: () -> Unit,
    onConfirmCompletion: () -> Unit,
    onRejectCompletion: () -> Unit,
    onReturn: () -> Unit,
    onFail: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val accent = if (item.type == CalendarItemType.TASK) colors.tertiary else colors.primary
    val canEdit = item.createdById == currentUserId && (item.type == CalendarItemType.EVENT || item.status == "ACTIVE")
    val isAssignedToCurrentUser = item.assignedToId == currentUserId
    val completionRequestedByCurrentUser = item.completionRequestedById == currentUserId

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (item.type == CalendarItemType.TASK) "Task" else "Event",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }

            Text(
                text = item.supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            Text(
                text = "Created by: ${item.createdByLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            item.assignedToLabel?.let { assignedTo ->
                Text(
                    text = "Assigned to: $assignedTo",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            item.status?.let { status ->
                Text(
                    text = when {
                        status == "WAITING_CONFIRMATION" && completionRequestedByCurrentUser -> "Waiting for partner confirmation"
                        status == "WAITING_CONFIRMATION" -> "Partner requested completion"
                        else -> "Status: ${status.lowercase().replaceFirstChar { it.uppercase() }}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == "WAITING_CONFIRMATION") colors.primary else colors.onSurfaceVariant
                )
            }

            item.completionRequestedByLabel?.let { requester ->
                Text(
                    text = "Completion requested by: $requester",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            item.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onSaveAsBlueprint,
                enabled = !isBusy,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save as Blueprint")
            }

            if (canEdit) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit")
                }

                OutlinedButton(
                    onClick = onDelete,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete")
                }
            }

            if (item.type == CalendarItemType.TASK) {
                when {
                    item.status == "ACTIVE" && isAssignedToCurrentUser -> {
                        Button(
                            onClick = onRequestCompletion,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Request Complete")
                        }

                        OutlinedButton(
                            onClick = onReturn,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Return")
                        }

                        OutlinedButton(
                            onClick = onFail,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Fail")
                        }
                    }

                    item.status == "WAITING_CONFIRMATION" && !completionRequestedByCurrentUser -> {
                        Button(
                            onClick = onConfirmCompletion,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm")
                        }

                        OutlinedButton(
                            onClick = onRejectCompletion,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reject")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditEventDialog(
    item: CalendarItemUi,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember(item.sourceId) { mutableStateOf(item.title) }
    var description by remember(item.sourceId) { mutableStateOf(item.description.orEmpty()) }
    var dateInput by remember(item.sourceId) { mutableStateOf(toDateInput(item.date)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, description, dateInput) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditTaskDialog(
    item: CalendarItemUi,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember(item.sourceId) { mutableStateOf(item.title) }
    var dateInput by remember(item.sourceId) { mutableStateOf(toDateInput(item.date)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, dateInput) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteItemDialog(
    item: CalendarItemUi,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${if (item.type == CalendarItemType.TASK) "Task" else "Event"}") },
        text = {
            Text("Are you sure you want to delete \"${item.title}\"?")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BlueprintCard(
    blueprint: BlueprintDto,
    onUse: () -> Unit,
    onDelete: () -> Unit,
    isBusy: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = blueprint.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            blueprint.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = buildString {
                    append(blueprint.type)
                    blueprint.defaultPoints?.let { points ->
                        append(" • ")
                        append(points)
                        append(" pts")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onDelete,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete")
            }

            Button(
                onClick = onUse,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use")
            }
        }
    }
}
