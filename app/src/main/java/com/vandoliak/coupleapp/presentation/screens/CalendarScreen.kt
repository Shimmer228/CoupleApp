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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.remote.BlueprintDto
import com.vandoliak.coupleapp.presentation.components.EmptyState
import com.vandoliak.coupleapp.presentation.components.SectionTitle
import com.vandoliak.coupleapp.presentation.components.SelectionChip
import com.vandoliak.coupleapp.presentation.util.blueprintTypeLabel
import com.vandoliak.coupleapp.presentation.util.formatFullDate
import com.vandoliak.coupleapp.presentation.util.formatMonthTitle
import com.vandoliak.coupleapp.presentation.util.recurrenceLabel
import com.vandoliak.coupleapp.presentation.util.scopeLabel
import com.vandoliak.coupleapp.presentation.util.taskStatusLabel
import com.vandoliak.coupleapp.presentation.util.toDateInput
import com.vandoliak.coupleapp.presentation.util.transactionCategoryLabel
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
    val context = LocalContext.current
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
        val currentUserId = calendarViewModel.currentUserId.value
        val weekdayLabels = listOf(
            stringResource(R.string.weekday_mon),
            stringResource(R.string.weekday_tue),
            stringResource(R.string.weekday_wed),
            stringResource(R.string.weekday_thu),
            stringResource(R.string.weekday_fri),
            stringResource(R.string.weekday_sat),
            stringResource(R.string.weekday_sun)
        )

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
                calendarViewModel.error.value?.takeIf { it.isNotBlank() }?.let { message ->
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
                        title = stringResource(R.string.calendar_loading_title),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                items(days, key = { it.date.toString() }) { day ->
                    CalendarDayCell(
                        day = day,
                        currentUserId = currentUserId,
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
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous_month)
            )
        }

        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        TextButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next_month)
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDayUi,
    currentUserId: String?,
    cellHeight: Dp,
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
                CompactPreviewChip(item = item, currentUserId = currentUserId)
            }

            if (day.remainingCount > 0) {
                Text(
                    text = stringResource(R.string.plus_more, day.remainingCount),
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
private fun CompactPreviewChip(
    item: CalendarItemUi,
    currentUserId: String?
) {
    val colors = MaterialTheme.colorScheme
    val isCurrentUserTask = item.type == CalendarItemType.TASK && item.assignedToId == currentUserId
    val backgroundColor = when {
        item.type == CalendarItemType.EVENT -> colors.surfaceVariant.copy(alpha = 0.85f)
        isCurrentUserTask -> colors.primaryContainer.copy(alpha = 0.95f)
        else -> colors.tertiaryContainer.copy(alpha = 0.95f)
    }
    val textColor = when {
        item.type == CalendarItemType.EVENT -> colors.onSurfaceVariant
        isCurrentUserTask -> colors.onPrimaryContainer
        else -> colors.onTertiaryContainer
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
                stringResource(R.string.calendar_task_preview, item.title, item.defaultPoints ?: 0)
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
            subtitle = stringResource(R.string.day_items_subtitle)
        )

        calendarViewModel.successMessage.value?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        calendarViewModel.error.value?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        blueprintViewModel.successMessage.value?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        blueprintViewModel.error.value?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (selectedItems.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.nothing_planned_title),
                subtitle = stringResource(R.string.nothing_planned_subtitle)
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
                title = stringResource(R.string.create_event_title),
                subtitle = stringResource(R.string.create_event_subtitle)
            )

            OutlinedTextField(
                value = calendarViewModel.eventTitle.value,
                onValueChange = calendarViewModel::onEventTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.title)) },
                singleLine = true
            )

            OutlinedTextField(
                value = calendarViewModel.eventDescription.value,
                onValueChange = calendarViewModel::onEventDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.description)) },
                maxLines = 3
            )

            Button(
                onClick = calendarViewModel::createEventForSelectedDay,
                modifier = Modifier.fillMaxWidth(),
                enabled = !calendarViewModel.isSubmitting.value && !blueprintViewModel.isSubmitting.value
            ) {
                Text(stringResource(R.string.create_event_button))
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(
                title = stringResource(R.string.create_task_title),
                subtitle = stringResource(R.string.create_task_subtitle)
            )

            OutlinedTextField(
                value = calendarViewModel.taskTitle.value,
                onValueChange = calendarViewModel::onTaskTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.title)) },
                singleLine = true
            )

            OutlinedTextField(
                value = calendarViewModel.taskPoints.value,
                onValueChange = calendarViewModel::onTaskPointsChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.points)) },
                singleLine = true
            )

            RecurrenceSelector(
                recurrenceType = calendarViewModel.recurrenceType.value,
                recurrenceInterval = calendarViewModel.recurrenceInterval.value,
                isSubmitting = calendarViewModel.isSubmitting.value || blueprintViewModel.isSubmitting.value,
                onRecurrenceTypeChange = calendarViewModel::onRecurrenceTypeChange,
                onRecurrenceIntervalChange = calendarViewModel::onRecurrenceIntervalChange
            )

            Button(
                onClick = calendarViewModel::createTaskForSelectedDay,
                modifier = Modifier.fillMaxWidth(),
                enabled = !calendarViewModel.isSubmitting.value && !blueprintViewModel.isSubmitting.value
            ) {
                Text(stringResource(R.string.create_task_button))
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
                    title = stringResource(R.string.blueprints_title),
                    subtitle = if (showBlueprints) {
                        stringResource(R.string.blueprints_use_subtitle)
                    } else {
                        stringResource(R.string.blueprints_library_subtitle)
                    }
                )

                TextButton(onClick = { showBlueprints = !showBlueprints }) {
                    Text(
                        if (showBlueprints) {
                            stringResource(R.string.hide)
                        } else {
                            stringResource(R.string.use_blueprint)
                        }
                    )
                }
            }

            if (showBlueprints) {
                if (blueprintViewModel.isLoading.value && blueprintViewModel.blueprints.value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.loading_blueprints),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (blueprintViewModel.blueprints.value.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.no_blueprints_title),
                        subtitle = stringResource(R.string.no_blueprints_subtitle)
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
    val context = LocalContext.current
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
                    text = if (item.type == CalendarItemType.TASK) {
                        stringResource(R.string.task_label)
                    } else {
                        stringResource(R.string.event_label)
                    },
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
                text = stringResource(R.string.created_by, item.createdByLabel),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            item.assignedToLabel?.let { assignedTo ->
                Text(
                    text = stringResource(R.string.assigned_to_format, assignedTo),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            item.status?.let { status ->
                Text(
                    text = if (status == "WAITING_CONFIRMATION") {
                        context.taskStatusLabel(status, completionRequestedByCurrentUser)
                    } else {
                        stringResource(R.string.status_format, context.taskStatusLabel(status, completionRequestedByCurrentUser))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == "WAITING_CONFIRMATION") colors.primary else colors.onSurfaceVariant
                )
            }

            item.completionRequestedByLabel?.let { requester ->
                Text(
                    text = stringResource(R.string.completion_requested_by_format, requester),
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

            item.recurrenceType?.let { recurrenceType ->
                if (item.type == CalendarItemType.TASK) {
                    Text(
                        text = stringResource(
                            R.string.recurrence_format,
                            context.recurrenceLabel(recurrenceType, item.recurrenceInterval)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            TextButton(
                onClick = onSaveAsBlueprint,
                enabled = !isBusy,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.save_as_blueprint))
            }

            if (canEdit) {
                OutlinedButton(
                    onClick = onEdit,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.edit))
                }

                OutlinedButton(
                    onClick = onDelete,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.delete))
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
                            Text(stringResource(R.string.request_complete))
                        }

                        OutlinedButton(
                            onClick = onReturn,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.return_task))
                        }

                        OutlinedButton(
                            onClick = onFail,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.fail_task))
                        }
                    }

                    item.status == "WAITING_CONFIRMATION" && !completionRequestedByCurrentUser -> {
                        Button(
                            onClick = onConfirmCompletion,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.confirm))
                        }

                        OutlinedButton(
                            onClick = onRejectCompletion,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.reject))
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
        title = { Text(stringResource(R.string.edit_event_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) }
                )
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text(stringResource(R.string.date_yyyy_mm_dd)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, description, dateInput) }) {
                Text(stringResource(R.string.save))
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
private fun EditTaskDialog(
    item: CalendarItemUi,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember(item.sourceId) { mutableStateOf(item.title) }
    var dateInput by remember(item.sourceId) { mutableStateOf(toDateInput(item.date)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_task_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text(stringResource(R.string.date_yyyy_mm_dd)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, dateInput) }) {
                Text(stringResource(R.string.save))
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
private fun DeleteItemDialog(
    item: CalendarItemUi,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (item.type == CalendarItemType.TASK) {
                    stringResource(R.string.delete_task_title)
                } else {
                    stringResource(R.string.delete_event_title)
                }
            )
        },
        text = {
            Text(stringResource(R.string.delete_named_item_message, item.title))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
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
private fun BlueprintCard(
    blueprint: BlueprintDto,
    onUse: () -> Unit,
    onDelete: () -> Unit,
    isBusy: Boolean
) {
    val context = LocalContext.current

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
                    append(context.blueprintTypeLabel(blueprint.type))
                    blueprint.defaultPoints?.let { points ->
                        append(" • ")
                        append(points)
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
                Text(stringResource(R.string.delete))
            }

            Button(
                onClick = onUse,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.use_blueprint))
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.interval_in_days)) },
                singleLine = true
            )
        }
    }
}
