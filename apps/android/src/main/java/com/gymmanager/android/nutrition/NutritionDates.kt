package com.gymmanager.android.nutrition

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val isoDay: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

fun todayIso(): String = LocalDate.now().format(isoDay)

fun parseIsoDay(value: String): LocalDate = LocalDate.parse(value, isoDay)

fun LocalDate.toIsoDay(): String = format(isoDay)

fun weekStrip(center: LocalDate): List<LocalDate> {
    val start = center.minusDays(3)
    return (0..6).map { start.plusDays(it.toLong()) }
}

fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String =
    when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d"))
    }
