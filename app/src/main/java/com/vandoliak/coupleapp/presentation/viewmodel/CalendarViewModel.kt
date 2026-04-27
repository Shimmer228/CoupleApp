package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.EventCreateRequest
import com.vandoliak.coupleapp.data.remote.EventDto
import com.vandoliak.coupleapp.data.remote.EventUpdateRequest
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.TaskCreateRequest
import com.vandoliak.coupleapp.data.remote.TaskDeleteResponse
import com.vandoliak.coupleapp.data.remote.TaskDto
import com.vandoliak.coupleapp.data.remote.TaskUpdateRequest
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import com.vandoliak.coupleapp.presentation.util.appString
import com.vandoliak.coupleapp.presentation.util.dateInputToApiDate
import com.vandoliak.coupleapp.presentation.util.isoDateToLocalDate
import com.vandoliak.coupleapp.presentation.util.localDateToApiDate
import com.vandoliak.coupleapp.presentation.util.toDateInput
import com.vandoliak.coupleapp.presentation.util.todayLocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarItemType {
    TASK,
    EVENT
}

data class CalendarItemUi(
    val id: String,
    val sourceId: String,
    val title: String,
    val type: CalendarItemType,
    val supportingText: String,
    val date: LocalDate,
    val description: String?,
    val defaultPoints: Int?,
    val createdById: String,
    val createdByLabel: String,
    val assignedToId: String? = null,
    val assignedToLabel: String? = null,
    val status: String? = null,
    val completionRequestedById: String? = null,
    val completionRequestedByLabel: String? = null,
    val recurrenceType: String? = null,
    val recurrenceInterval: Int? = null
)

data class CalendarDayUi(
    val date: LocalDate,
    val isInCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val previews: List<CalendarItemUi>,
    val remainingCount: Int
)

class CalendarViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    private var allTasks: List<TaskDto> = emptyList()
    private var allEvents: List<EventDto> = emptyList()

    var visibleMonth = mutableStateOf(YearMonth.now())
        private set

    var selectedDate = mutableStateOf(todayLocalDate())
        private set

    var monthDays = mutableStateOf<List<CalendarDayUi>>(emptyList())
        private set

    var selectedDayItems = mutableStateOf<List<CalendarItemUi>>(emptyList())
        private set

    var currentUserId = mutableStateOf<String?>(null)
        private set

    var isDaySheetVisible = mutableStateOf(false)
        private set

    var eventTitle = mutableStateOf("")
        private set

    var eventDescription = mutableStateOf("")
        private set

    var taskTitle = mutableStateOf("")
        private set

    var taskPoints = mutableStateOf("")
        private set

    var recurrenceType = mutableStateOf("NONE")
        private set

    var recurrenceInterval = mutableStateOf("")
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isSubmitting = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    var successMessage = mutableStateOf<String?>(null)
        private set

    fun loadCalendar() {
        viewModelScope.launch {
            loadData(showLoader = true)
        }
    }

    fun goToPreviousMonth() {
        visibleMonth.value = visibleMonth.value.minusMonths(1)
        rebuildCalendarState()
    }

    fun goToNextMonth() {
        visibleMonth.value = visibleMonth.value.plusMonths(1)
        rebuildCalendarState()
    }

    fun onDaySelected(date: LocalDate) {
        selectedDate.value = date
        visibleMonth.value = YearMonth.from(date)
        isDaySheetVisible.value = true
        successMessage.value = null
        rebuildCalendarState()
    }

    fun dismissDaySheet() {
        isDaySheetVisible.value = false
    }

    fun selectedDateInput(): String = toDateInput(selectedDate.value)

    fun onEventTitleChange(value: String) {
        eventTitle.value = value
    }

    fun onEventDescriptionChange(value: String) {
        eventDescription.value = value
    }

    fun onTaskTitleChange(value: String) {
        taskTitle.value = value
    }

    fun onTaskPointsChange(value: String) {
        taskPoints.value = value.filter { it.isDigit() }
    }

    fun onRecurrenceTypeChange(value: String) {
        recurrenceType.value = value
        if (value != "EVERY_X_DAYS") {
            recurrenceInterval.value = ""
        }
    }

    fun onRecurrenceIntervalChange(value: String) {
        recurrenceInterval.value = value.filter { it.isDigit() }
    }

    fun createEventForSelectedDay() {
        if (eventTitle.value.isBlank()) {
            error.value = appString(R.string.event_title_required)
            return
        }

        viewModelScope.launch {
            mutateWithRefresh(
                successText = appString(R.string.event_added_to_date, selectedDate.value.toString()),
                request = { authorization ->
                    RetrofitInstance.eventApi.createEvent(
                        authorization = authorization,
                        request = EventCreateRequest(
                            title = eventTitle.value.trim(),
                            description = eventDescription.value.trim().ifBlank { null },
                            date = localDateToApiDate(selectedDate.value)
                        )
                    )
                }
            )

            if (error.value == null) {
                eventTitle.value = ""
                eventDescription.value = ""
            }
        }
    }

    fun createTaskForSelectedDay() {
        val points = taskPoints.value.toIntOrNull()
        val recurrenceIntervalValue = recurrenceInterval.value.toIntOrNull()

        if (taskTitle.value.isBlank()) {
            error.value = appString(R.string.task_title_required)
            return
        }

        if (points == null || points <= 0) {
            error.value = appString(R.string.points_positive)
            return
        }

        if (recurrenceType.value == "EVERY_X_DAYS" && (recurrenceIntervalValue == null || recurrenceIntervalValue <= 0)) {
            error.value = appString(R.string.recurring_interval_positive)
            return
        }

        viewModelScope.launch {
            mutateTaskAction(
                successText = appString(R.string.task_scheduled_for_date, selectedDate.value.toString()),
                request = { authorization ->
                    RetrofitInstance.taskApi.createTask(
                        authorization = authorization,
                        request = TaskCreateRequest(
                            title = taskTitle.value.trim(),
                            points = points,
                            dueDate = localDateToApiDate(selectedDate.value),
                            recurrenceType = recurrenceType.value,
                            recurrenceInterval = recurrenceIntervalValue
                        )
                    )
                }
            )

            if (error.value == null) {
                taskTitle.value = ""
                taskPoints.value = ""
                recurrenceType.value = "NONE"
                recurrenceInterval.value = ""
            }
        }
    }

    fun updateEvent(
        eventId: String,
        title: String,
        description: String,
        dateInput: String
    ) {
        val trimmedTitle = title.trim()
        val apiDate = dateInputToApiDate(dateInput)

        if (trimmedTitle.isBlank()) {
            error.value = appString(R.string.event_title_required)
            return
        }

        if (apiDate == null) {
            error.value = appString(R.string.date_format_required)
            return
        }

        viewModelScope.launch {
            mutateWithRefresh(
                successText = appString(R.string.event_updated),
                request = { authorization ->
                    RetrofitInstance.eventApi.updateEvent(
                        authorization = authorization,
                        eventId = eventId,
                        request = EventUpdateRequest(
                            title = trimmedTitle,
                            description = description.trim().ifBlank { null },
                            date = apiDate
                        )
                    )
                }
            )
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            mutateWithRefresh(
                successText = appString(R.string.event_deleted),
                request = { authorization ->
                    RetrofitInstance.eventApi.deleteEvent(
                        authorization = authorization,
                        eventId = eventId
                    )
                }
            )
        }
    }

    fun updateTask(taskId: String, title: String, dateInput: String) {
        val trimmedTitle = title.trim()
        val apiDate = if (dateInput.isBlank()) null else dateInputToApiDate(dateInput)

        if (trimmedTitle.isBlank()) {
            error.value = appString(R.string.task_title_required)
            return
        }

        if (dateInput.isNotBlank() && apiDate == null) {
            error.value = appString(R.string.date_format_required)
            return
        }

        viewModelScope.launch {
            mutateTaskAction(
                successText = appString(R.string.task_updated),
                request = { authorization ->
                    RetrofitInstance.taskApi.updateTask(
                        authorization = authorization,
                        taskId = taskId,
                        request = TaskUpdateRequest(
                            title = trimmedTitle,
                            dueDate = apiDate
                        )
                    )
                }
            )
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = appString(R.string.session_expired_login)
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.taskApi.deleteTask("Bearer $token", taskId)
                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage(appString(R.string.failed_to_delete_task))
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    error.value = appString(R.string.server_empty_response)
                    return@launch
                }

                applyTaskDelete(body)
                successMessage.value = appString(R.string.task_deleted)
                loadData(showLoader = false)
            } catch (e: Exception) {
                error.value = e.message ?: appString(R.string.unknown_error)
            } finally {
                isSubmitting.value = false
            }
        }
    }

    fun requestTaskCompletion(taskId: String) {
        runTaskAction(taskId, appString(R.string.waiting_for_partner_confirmation)) { authorization, id ->
            RetrofitInstance.taskApi.requestCompletion(authorization, id)
        }
    }

    fun confirmTaskCompletion(taskId: String) {
        runTaskAction(taskId, appString(R.string.task_completed_successfully)) { authorization, id ->
            RetrofitInstance.taskApi.confirmCompletion(authorization, id)
        }
    }

    fun rejectTaskCompletion(taskId: String) {
        runTaskAction(taskId, appString(R.string.completion_request_rejected)) { authorization, id ->
            RetrofitInstance.taskApi.rejectCompletion(authorization, id)
        }
    }

    fun returnTask(taskId: String) {
        runTaskAction(taskId, appString(R.string.task_returned_successfully)) { authorization, id ->
            RetrofitInstance.taskApi.returnTask(authorization, id)
        }
    }

    fun failTask(taskId: String) {
        runTaskAction(taskId, appString(R.string.task_failed_success)) { authorization, id ->
            RetrofitInstance.taskApi.failTask(authorization, id)
        }
    }

    private fun runTaskAction(
        taskId: String,
        successText: String,
        action: suspend (String, String) -> Response<com.vandoliak.coupleapp.data.remote.TaskActionResponse>
    ) {
        viewModelScope.launch {
            mutateTaskAction(successText) { authorization ->
                action(authorization, taskId)
            }
        }
    }

    private suspend fun loadData(showLoader: Boolean) {
        val token = tokenManager.tokenFlow.first()
        if (token.isNullOrBlank()) {
            error.value = appString(R.string.session_expired_login)
            return
        }

        try {
            if (showLoader) {
                isLoading.value = true
            }
            error.value = null

            val authorization = "Bearer $token"
            val tasksResponse = RetrofitInstance.taskApi.getTasks(authorization)
            if (!tasksResponse.isSuccessful) {
                error.value = tasksResponse.extractErrorMessage(appString(R.string.failed_to_load_tasks))
                return
            }

            val eventsResponse = RetrofitInstance.eventApi.getAllEvents(authorization)
            if (!eventsResponse.isSuccessful) {
                error.value = eventsResponse.extractErrorMessage(appString(R.string.failed_to_load_events))
                return
            }

            val taskBody = tasksResponse.body()
            if (taskBody == null) {
                error.value = appString(R.string.server_empty_task_list)
                return
            }

            currentUserId.value = taskBody.currentUserId
            allTasks = taskBody.tasks
            allEvents = eventsResponse.body()?.events.orEmpty()
            rebuildCalendarState()
        } catch (e: Exception) {
            error.value = e.message ?: appString(R.string.unknown_error)
        } finally {
            if (showLoader) {
                isLoading.value = false
            }
        }
    }

    private suspend fun <T> mutateWithRefresh(
        successText: String,
        request: suspend (String) -> Response<T>
    ) {
        val token = tokenManager.tokenFlow.first()
        if (token.isNullOrBlank()) {
            error.value = appString(R.string.session_expired_login)
            return
        }

        try {
            isSubmitting.value = true
            error.value = null
            successMessage.value = null

            val response = request("Bearer $token")
            if (!response.isSuccessful) {
                error.value = response.extractErrorMessage(appString(R.string.calendar_action_failed))
                return
            }

            successMessage.value = successText
            loadData(showLoader = false)
        } catch (e: Exception) {
            error.value = e.message ?: appString(R.string.unknown_error)
        } finally {
            isSubmitting.value = false
        }
    }

    private suspend fun mutateTaskAction(
        successText: String,
        request: suspend (String) -> Response<com.vandoliak.coupleapp.data.remote.TaskActionResponse>
    ) {
        val token = tokenManager.tokenFlow.first()
        if (token.isNullOrBlank()) {
            error.value = appString(R.string.session_expired_login)
            return
        }

        try {
            isSubmitting.value = true
            error.value = null
            successMessage.value = null

            val response = request("Bearer $token")
            if (!response.isSuccessful) {
                error.value = response.extractErrorMessage(appString(R.string.task_action_failed))
                return
            }

            successMessage.value = successText
            loadData(showLoader = false)
        } catch (e: Exception) {
            error.value = e.message ?: appString(R.string.unknown_error)
        } finally {
            isSubmitting.value = false
        }
    }

    private fun applyTaskDelete(body: TaskDeleteResponse) {
        val deletedId = body.deletedId
        allTasks = allTasks.filterNot { it.id == deletedId }
    }

    private fun rebuildCalendarState() {
        val itemsByDate = buildItemsByDate()
        val month = visibleMonth.value
        val today = todayLocalDate()
        val firstMonthDate = month.atDay(1)
        val leadingDays = firstMonthDate.dayOfWeek.value - 1
        val startDate = firstMonthDate.minusDays(leadingDays.toLong())

        monthDays.value = (0 until 42).map { index ->
            val cellDate = startDate.plusDays(index.toLong())
            val dayItems = itemsByDate[cellDate].orEmpty()

            CalendarDayUi(
                date = cellDate,
                isInCurrentMonth = YearMonth.from(cellDate) == month,
                isToday = cellDate == today,
                isSelected = cellDate == selectedDate.value,
                previews = dayItems.take(2),
                remainingCount = (dayItems.size - 2).coerceAtLeast(0)
            )
        }

        selectedDayItems.value = itemsByDate[selectedDate.value].orEmpty()
    }

    private fun buildItemsByDate(): Map<LocalDate, List<CalendarItemUi>> {
        val taskItems = allTasks.mapNotNull { task ->
            val taskDate = isoDateToLocalDate(task.dueDate) ?: return@mapNotNull null
            taskDate to CalendarItemUi(
                id = "task-${task.id}",
                sourceId = task.id,
                title = task.title,
                type = CalendarItemType.TASK,
                supportingText = appString(R.string.bank_points_format, task.bank),
                date = taskDate,
                description = null,
                defaultPoints = task.bank,
                createdById = task.createdBy.id,
                createdByLabel = task.createdBy.email,
                assignedToId = task.assignedTo.id,
                assignedToLabel = task.assignedTo.email,
                status = task.status,
                completionRequestedById = task.completionRequestedBy?.id,
                completionRequestedByLabel = task.completionRequestedBy?.email,
                recurrenceType = task.recurrenceType,
                recurrenceInterval = task.recurrenceInterval
            )
        }

        val eventItems = allEvents.mapNotNull { event ->
            val eventDate = isoDateToLocalDate(event.date) ?: return@mapNotNull null
            eventDate to CalendarItemUi(
                id = "event-${event.id}",
                sourceId = event.id,
                title = event.title,
                type = CalendarItemType.EVENT,
                supportingText = event.description ?: appString(R.string.event_label),
                date = eventDate,
                description = event.description,
                defaultPoints = null,
                createdById = event.createdBy.id,
                createdByLabel = event.createdBy.email
            )
        }

        return (taskItems + eventItems)
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
            .mapValues { (_, items) ->
                items.sortedWith(
                    compareBy<CalendarItemUi> { if (it.type == CalendarItemType.TASK) 0 else 1 }
                        .thenBy { it.title.lowercase() }
                )
            }
    }
}
