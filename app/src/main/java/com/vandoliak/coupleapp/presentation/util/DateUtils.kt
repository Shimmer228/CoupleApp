package com.vandoliak.coupleapp.presentation.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val apiDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

fun todayDateInput(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

fun todayLocalDate(): LocalDate = LocalDate.now()

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
    return localDateToApiDate(localDate)
}

fun localDateToApiDate(localDate: LocalDate): String {
    val instant = localDate
        .atTime(12, 0)
        .toInstant(ZoneOffset.UTC)

    return apiDateFormatter.format(instant)
}

fun isoDateMatchesSelectedDay(isoDate: String?, selectedDate: LocalDate): Boolean {
    return isoDateToLocalDate(isoDate) == selectedDate
}

fun formatIsoDateForDisplay(isoDate: String?): String? {
    return isoDateToLocalDate(isoDate)?.format(DateTimeFormatter.ISO_LOCAL_DATE)
}

fun isoDateToLocalDate(isoDate: String?): LocalDate? {
    if (isoDate.isNullOrBlank()) {
        return null
    }

    return try {
        Instant.parse(isoDate)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
    } catch (_: Exception) {
        null
    }
}

fun formatMonthTitle(yearMonth: YearMonth): String {
    return yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
}

fun formatFullDate(localDate: LocalDate): String {
    return localDate.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
}

fun toDateInput(localDate: LocalDate): String {
    return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
