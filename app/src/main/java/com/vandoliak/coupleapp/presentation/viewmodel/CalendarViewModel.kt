package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.EventCreateRequest
import com.vandoliak.coupleapp.data.remote.EventDto
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.TaskDto
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import com.vandoliak.coupleapp.presentation.util.dateInputToApiDate
import com.vandoliak.coupleapp.presentation.util.isoDateMatchesSelectedDay
import com.vandoliak.coupleapp.presentation.util.parseDateInput
import com.vandoliak.coupleapp.presentation.util.sanitizeDateInput
import com.vandoliak.coupleapp.presentation.util.todayDateInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CalendarItemUi(
    val id: String,
    val title: String,
    val type: String,
    val subtitle: String,
    val dateLabel: String,
    val sortKey: String
)

class CalendarViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    private var allTasks: List<TaskDto> = emptyList()
    private var allEvents: List<EventDto> = emptyList()

    var selectedDate = mutableStateOf(todayDateInput())
        private set

    var eventTitle = mutableStateOf("")
        private set

    var eventDescription = mutableStateOf("")
        private set

    var eventDate = mutableStateOf(todayDateInput())
        private set

    var calendarItems = mutableStateOf<List<CalendarItemUi>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isSubmitting = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    var successMessage = mutableStateOf<String?>(null)
        private set

    fun onSelectedDateChange(value: String) {
        selectedDate.value = sanitizeDateInput(value)
        rebuildCalendarItems()
    }

    fun onEventTitleChange(value: String) {
        eventTitle.value = value
    }

    fun onEventDescriptionChange(value: String) {
        eventDescription.value = value
    }

    fun onEventDateChange(value: String) {
        eventDate.value = sanitizeDateInput(value)
    }

    fun loadCalendar() {
        viewModelScope.launch {
            loadData(showLoader = true)
        }
    }

    fun createEvent() {
        if (eventTitle.value.isBlank()) {
            error.value = "Event title is required"
            return
        }

        val apiDate = dateInputToApiDate(eventDate.value)
        if (apiDate == null) {
            error.value = "Event date must use YYYY-MM-DD format"
            return
        }

        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first()
            if (token.isNullOrBlank()) {
                error.value = "Session expired. Please log in again"
                return@launch
            }

            try {
                isSubmitting.value = true
                error.value = null
                successMessage.value = null

                val response = RetrofitInstance.eventApi.createEvent(
                    authorization = "Bearer $token",
                    request = EventCreateRequest(
                        title = eventTitle.value.trim(),
                        description = eventDescription.value.trim().ifBlank { null },
                        date = apiDate
                    )
                )

                if (!response.isSuccessful) {
                    error.value = response.extractErrorMessage("Failed to create event")
                    return@launch
                }

                successMessage.value = "Event created successfully"
                eventTitle.value = ""
                eventDescription.value = ""
                eventDate.value = selectedDate.value
                loadData(showLoader = false)
            } catch (e: Exception) {
                error.value = e.message ?: "Unknown error"
            } finally {
                isSubmitting.value = false
            }
        }
    }

    private suspend fun loadData(showLoader: Boolean) {
        val token = tokenManager.tokenFlow.first()
        if (token.isNullOrBlank()) {
            error.value = "Session expired. Please log in again"
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
                error.value = tasksResponse.extractErrorMessage("Failed to load tasks")
                return
            }

            val eventsResponse = RetrofitInstance.eventApi.getAllEvents(authorization)
            if (!eventsResponse.isSuccessful) {
                error.value = eventsResponse.extractErrorMessage("Failed to load events")
                return
            }

            allTasks = tasksResponse.body()?.tasks.orEmpty()
            allEvents = eventsResponse.body()?.events.orEmpty()
            rebuildCalendarItems()
        } catch (e: Exception) {
            error.value = e.message ?: "Unknown error"
        } finally {
            if (showLoader) {
                isLoading.value = false
            }
        }
    }

    private fun rebuildCalendarItems() {
        val selectedLocalDate = parseDateInput(selectedDate.value)
        if (selectedLocalDate == null) {
            calendarItems.value = emptyList()
            return
        }

        val taskItems = allTasks
            .filter { isoDateMatchesSelectedDay(it.dueDate, selectedLocalDate) }
            .map {
                CalendarItemUi(
                    id = "task-${it.id}",
                    title = it.title,
                    type = "Task",
                    subtitle = "Bank: ${it.bank} points",
                    dateLabel = "Due: ${selectedDate.value}",
                    sortKey = it.dueDate ?: it.createdAt
                )
            }

        val eventItems = allEvents
            .filter { isoDateMatchesSelectedDay(it.date, selectedLocalDate) }
            .map {
                CalendarItemUi(
                    id = "event-${it.id}",
                    title = it.title,
                    type = "Event",
                    subtitle = it.description ?: "No description",
                    dateLabel = "Date: ${selectedDate.value}",
                    sortKey = it.date
                )
            }

        calendarItems.value = (taskItems + eventItems).sortedBy { it.sortKey }
    }
}
