package com.vandoliak.coupleapp.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vandoliak.coupleapp.R
import com.vandoliak.coupleapp.data.local.TokenManager
import com.vandoliak.coupleapp.data.remote.RetrofitInstance
import com.vandoliak.coupleapp.data.remote.SharedSplitRequest
import com.vandoliak.coupleapp.data.remote.TaskActionResponse
import com.vandoliak.coupleapp.data.remote.TaskCreateRequest
import com.vandoliak.coupleapp.data.remote.TaskDto
import com.vandoliak.coupleapp.data.remote.TaskListResponse
import com.vandoliak.coupleapp.data.remote.TaskPartnerDto
import com.vandoliak.coupleapp.data.remote.extractErrorMessage
import com.vandoliak.coupleapp.presentation.util.appString
import com.vandoliak.coupleapp.presentation.util.dateInputToApiDate
import com.vandoliak.coupleapp.presentation.util.sanitizeDateInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Response

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenManager = TokenManager(app)

    var taskTitle = mutableStateOf("")
        private set

    var taskPoints = mutableStateOf("")
        private set

    var dueDate = mutableStateOf("")
        private set

    var taskType = mutableStateOf("CHALLENGE")
        private set

    var recurrenceType = mutableStateOf("NONE")
        private set

    var recurrenceInterval = mutableStateOf("")
        private set

    var tasks = mutableStateOf<List<TaskDto>>(emptyList())
        private set

    var currentUserId = mutableStateOf<String?>(null)
        private set

    var currentUserPoints = mutableStateOf(0)
        private set

    var partner = mutableStateOf<TaskPartnerDto?>(null)
        private set

    var isLoading = mutableStateOf(false)
        private set

    var isSubmitting = mutableStateOf(false)
        private set

    var error = mutableStateOf<String?>(null)
        private set

    var successMessage = mutableStateOf<String?>(null)
        private set

    fun onTaskTitleChange(value: String) {
        taskTitle.value = value
    }

    fun onTaskPointsChange(value: String) {
        taskPoints.value = value.filter { it.isDigit() }
    }

    fun onDueDateChange(value: String) {
        dueDate.value = sanitizeDateInput(value)
    }

    fun onTaskTypeChange(value: String) {
        taskType.value = value
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

    fun loadTasks() {
        viewModelScope.launch {
            fetchTasks(showLoader = true)
        }
    }

    fun createTask(onSuccess: (() -> Unit)? = null) {
        val points = taskPoints.value.toIntOrNull()
        val apiDueDate = if (dueDate.value.isBlank()) null else dateInputToApiDate(dueDate.value)
        val recurrenceIntervalValue = recurrenceInterval.value.toIntOrNull()

        successMessage.value = null

        if (taskTitle.value.isBlank()) {
            error.value = appString(R.string.task_title_required)
            return
        }

        if (points == null || points <= 0) {
            error.value = appString(R.string.points_positive)
            return
        }

        if (dueDate.value.isNotBlank() && apiDueDate == null) {
            error.value = appString(R.string.due_date_format_required)
            return
        }

        if (recurrenceType.value != "NONE" && apiDueDate == null) {
            error.value = appString(R.string.recurring_tasks_require_due_date)
            return
        }

        if (recurrenceType.value == "EVERY_X_DAYS" && (recurrenceIntervalValue == null || recurrenceIntervalValue <= 0)) {
            error.value = appString(R.string.recurring_interval_positive)
            return
        }

        viewModelScope.launch {
            executeTaskMutation(
                successText = if (taskType.value == "SHARED") {
                    appString(R.string.shared_task_created_successfully)
                } else {
                    appString(R.string.challenge_created_successfully)
                }
            ) { authorization ->
                RetrofitInstance.taskApi.createTask(
                    authorization = authorization,
                    request = TaskCreateRequest(
                        title = taskTitle.value.trim(),
                        type = taskType.value,
                        points = points,
                        dueDate = apiDueDate,
                        recurrenceType = recurrenceType.value,
                        recurrenceInterval = recurrenceIntervalValue
                    )
                )
            }

            if (error.value == null) {
                resetCreateForm()
                onSuccess?.invoke()
            }
        }
    }

    fun resetCreateForm() {
        taskTitle.value = ""
        taskPoints.value = ""
        dueDate.value = ""
        taskType.value = "CHALLENGE"
        recurrenceType.value = "NONE"
        recurrenceInterval.value = ""
        error.value = null
    }

    fun requestCompletion(taskId: String) {
        runTaskAction(taskId, appString(R.string.waiting_for_partner_confirmation)) { authorization, id ->
            RetrofitInstance.taskApi.requestCompletion(authorization, id)
        }
    }

    fun confirmCompletion(taskId: String) {
        runTaskAction(taskId, appString(R.string.task_completed_successfully)) { authorization, id ->
            RetrofitInstance.taskApi.confirmCompletion(authorization, id)
        }
    }

    fun rejectCompletion(taskId: String) {
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

    fun proposeSharedSplit(taskId: String, myPoints: Int, partnerPoints: Int, onSuccess: (() -> Unit)? = null) {
        runTaskAction(taskId, appString(R.string.shared_split_proposed), onSuccess) { authorization, id ->
            RetrofitInstance.taskApi.proposeSharedSplit(
                authorization = authorization,
                taskId = id,
                request = SharedSplitRequest(myPoints = myPoints, partnerPoints = partnerPoints)
            )
        }
    }

    fun acceptSharedSplit(taskId: String, onSuccess: (() -> Unit)? = null) {
        runTaskAction(taskId, appString(R.string.shared_split_accepted), onSuccess) { authorization, id ->
            RetrofitInstance.taskApi.acceptSharedSplit(authorization, id)
        }
    }

    fun counterSharedSplit(taskId: String, myPoints: Int, partnerPoints: Int, onSuccess: (() -> Unit)? = null) {
        runTaskAction(taskId, appString(R.string.shared_split_countered), onSuccess) { authorization, id ->
            RetrofitInstance.taskApi.counterSharedSplit(
                authorization = authorization,
                taskId = id,
                request = SharedSplitRequest(myPoints = myPoints, partnerPoints = partnerPoints)
            )
        }
    }

    private fun runTaskAction(
        taskId: String,
        successText: String,
        onSuccess: (() -> Unit)? = null,
        action: suspend (String, String) -> Response<TaskActionResponse>
    ) {
        viewModelScope.launch {
            executeTaskMutation(successText) { authorization ->
                action(authorization, taskId)
            }
            if (error.value == null) {
                onSuccess?.invoke()
            }
        }
    }

    private suspend fun fetchTasks(showLoader: Boolean) {
        val token = tokenManager.tokenFlow.first()

        if (token.isNullOrBlank()) {
            error.value = appString(R.string.session_expired_login)
            return
        }

        try {
            if (showLoader) {
                isLoading.value = true
            }

            val response = RetrofitInstance.taskApi.getTasks("Bearer $token")

            if (response.isSuccessful) {
                val body = response.body()

                if (body == null) {
                    error.value = appString(R.string.server_empty_task_list)
                    return
                }

                applyTaskState(body)
            } else {
                error.value = response.extractErrorMessage(appString(R.string.failed_to_load_tasks))
            }
        } catch (e: Exception) {
            error.value = e.message ?: appString(R.string.unknown_error)
        } finally {
            if (showLoader) {
                isLoading.value = false
            }
        }
    }

    private suspend fun executeTaskMutation(
        successText: String,
        request: suspend (String) -> Response<TaskActionResponse>
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

            val body = response.body()
            if (body == null) {
                error.value = appString(R.string.server_empty_response)
                return
            }

            currentUserPoints.value = body.currentUserPoints
            successMessage.value = successText
            fetchTasks(showLoader = false)
        } catch (e: Exception) {
            error.value = e.message ?: appString(R.string.unknown_error)
        } finally {
            isSubmitting.value = false
        }
    }

    private fun applyTaskState(body: TaskListResponse) {
        currentUserId.value = body.currentUserId
        currentUserPoints.value = body.currentUserPoints
        partner.value = body.partner
        tasks.value = body.tasks
    }
}
