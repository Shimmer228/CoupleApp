package com.vandoliak.coupleapp.presentation.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val apiDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

fun todayDateInput(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

fun sanitizeDateInput(value: String): String {
    return buildString {
        value.forEach { character ->
            if (character.isDigit() || character == '-') {
                append(character)
            }
        }
    }.take(10)
}

fun parseDateInput(value: String): LocalDate? {
    return try {
        LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: Exception) {
        null
    }
}

fun dateInputToApiDate(value: String): String? {
    val localDate = parseDateInput(value) ?: return null
    val instant = localDate
        .atTime(12, 0)
        .toInstant(ZoneOffset.UTC)

    return apiDateFormatter.format(instant)
}

fun isoDateMatchesSelectedDay(isoDate: String?, selectedDate: LocalDate): Boolean {
    if (isoDate.isNullOrBlank()) {
        return false
    }

    return try {
        Instant.parse(isoDate).atZone(ZoneOffset.UTC).toLocalDate() == selectedDate
    } catch (_: Exception) {
        false
    }
}

fun formatIsoDateForDisplay(isoDate: String?): String? {
    if (isoDate.isNullOrBlank()) {
        return null
    }

    return try {
        Instant.parse(isoDate)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (_: Exception) {
        null
    }
}
