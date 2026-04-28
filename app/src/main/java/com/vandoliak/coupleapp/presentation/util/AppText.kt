package com.vandoliak.coupleapp.presentation.util

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import com.vandoliak.coupleapp.R

fun AndroidViewModel.appString(@StringRes resId: Int, vararg args: Any): String {
    return getApplication<Application>().getString(resId, *args)
}

fun Context.scopeLabel(scope: String): String {
    return getString(
        when (scope.uppercase()) {
            "SELF" -> R.string.scope_self
            "PARTNER" -> R.string.scope_partner
            else -> R.string.scope_shared
        }
    )
}

fun Context.priorityLabel(priority: String): String {
    return getString(
        when (priority.uppercase()) {
            "HIGH" -> R.string.priority_high
            "MEDIUM" -> R.string.priority_medium
            else -> R.string.priority_low
        }
    )
}

fun Context.transactionTypeLabel(type: String): String {
    return getString(
        when (type.uppercase()) {
            "INCOME" -> R.string.transaction_type_income
            else -> R.string.transaction_type_expense
        }
    )
}

fun Context.transactionCategoryLabel(category: String): String {
    return getString(
        when (category.uppercase()) {
            "FOOD" -> R.string.finance_category_food
            "UTILITIES" -> R.string.finance_category_utilities
            "TRANSPORT" -> R.string.finance_category_transport
            "HOME" -> R.string.finance_category_home
            "ENTERTAINMENT" -> R.string.finance_category_entertainment
            "HEALTH" -> R.string.finance_category_health
            "SHOPPING" -> R.string.finance_category_shopping
            "SUBSCRIPTIONS" -> R.string.finance_category_subscriptions
            "SALARY" -> R.string.finance_category_salary
            "BONUS" -> R.string.finance_category_bonus
            "GIFT" -> R.string.finance_category_gift
            "REFUND" -> R.string.finance_category_refund
            "SIDE_JOB" -> R.string.finance_category_side_job
            else -> R.string.finance_category_other
        }
    )
}

fun Context.transactionStatusLabel(status: String): String {
    return getString(
        when (status.uppercase()) {
            "PENDING_CONFIRMATION" -> R.string.transaction_status_pending
            "REJECTED" -> R.string.transaction_status_rejected
            else -> R.string.transaction_status_confirmed
        }
    )
}

fun Context.recurrenceLabel(type: String, interval: Int? = null): String {
    return when (type.uppercase()) {
        "EVERY_X_DAYS" -> getString(R.string.recurrence_every_n_days, interval ?: 1)
        "WEEKLY" -> getString(R.string.recurrence_weekly)
        "MONTHLY" -> getString(R.string.recurrence_monthly)
        else -> getString(R.string.recurrence_none)
    }
}

fun Context.taskStatusLabel(status: String, requestedByCurrentUser: Boolean, taskType: String = "CHALLENGE"): String {
    return when {
        taskType.uppercase() == "SHARED" && status == "WAITING_CONFIRMATION" && requestedByCurrentUser -> {
            getString(R.string.shared_split_waiting_partner)
        }

        taskType.uppercase() == "SHARED" && status == "WAITING_CONFIRMATION" -> {
            getString(R.string.shared_split_partner_proposed)
        }

        status == "WAITING_CONFIRMATION" && requestedByCurrentUser -> {
            getString(R.string.waiting_for_partner_confirmation)
        }

        status == "WAITING_CONFIRMATION" -> {
            getString(R.string.partner_requested_completion)
        }

        status == "COMPLETED" -> {
            getString(R.string.task_status_completed)
        }

        status == "FAILED" -> {
            getString(R.string.task_status_failed)
        }

        else -> {
            getString(R.string.task_status_active)
        }
    }
}

fun Context.blueprintTypeLabel(type: String): String {
    return getString(
        when (type.uppercase()) {
            "EVENT" -> R.string.event_label
            else -> R.string.task_label
        }
    )
}

fun Context.taskTypeLabel(type: String): String {
    return getString(
        when (type.uppercase()) {
            "SHARED" -> R.string.shared_task_type
            else -> R.string.challenge_task_type
        }
    )
}

fun Context.avatarOptionLabel(key: String): String {
    return getString(
        when (key.lowercase()) {
            "cat" -> R.string.avatar_cat
            "fox" -> R.string.avatar_fox
            "bear" -> R.string.avatar_bear
            "star" -> R.string.avatar_star
            "moon" -> R.string.avatar_moon
            else -> R.string.avatar_heart
        }
    )
}
