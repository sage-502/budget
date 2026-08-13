package com.sage502.budgettracker.util

import java.time.YearMonth
import java.time.ZoneId

fun currentMonthKey(): String = YearMonth.now().toString()

fun monthKeyToRange(monthKey: String): Pair<Long, Long> {
    val ym = YearMonth.parse(monthKey)
    val zone = ZoneId.systemDefault()
    val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = ym.atEndOfMonth().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return start to end
}

fun formatMonthKey(monthKey: String): String {
    val ym = YearMonth.parse(monthKey)
    return "${ym.year}년 ${ym.monthValue}월"
}

fun formatAmount(amount: Long): String = "%,d원".format(amount)

fun formatDate(timestamp: Long): String {
    val date = java.time.Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    return "%d.%02d.%02d".format(date.year, date.monthValue, date.dayOfMonth)
}
