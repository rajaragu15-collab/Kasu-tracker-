package com.nanba.financetracker.util

import java.util.Calendar

/**
 * Shared time-range filter used across Home, Transactions, and Cigarette tracking
 * screens, so "Last 7 Days" etc. always means the same thing everywhere in the app.
 */
enum class TimeRangeType {
    TODAY,
    THIS_WEEK,
    LAST_7_DAYS,
    LAST_10_DAYS,
    THIS_MONTH,
    LAST_30_DAYS,
    CUSTOM
}

data class TimeRange(
    val startMillis: Long,
    val endMillis: Long, // exclusive
    val label: String
)

object TimeRangeCalculator {

    private const val ONE_DAY = 24 * 60 * 60 * 1000L

    fun calculate(type: TimeRangeType, customStart: Long? = null, customEnd: Long? = null): TimeRange {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis
        val endOfToday = startOfToday + ONE_DAY

        return when (type) {
            TimeRangeType.TODAY -> TimeRange(startOfToday, endOfToday, "Today")

            TimeRangeType.THIS_WEEK -> {
                val weekCal = cal.clone() as Calendar
                weekCal.set(Calendar.DAY_OF_WEEK, weekCal.firstDayOfWeek)
                TimeRange(weekCal.timeInMillis, endOfToday, "This Week")
            }

            TimeRangeType.LAST_7_DAYS -> TimeRange(
                startOfToday - 6 * ONE_DAY,
                endOfToday,
                "Last 7 Days"
            )

            TimeRangeType.LAST_10_DAYS -> TimeRange(
                startOfToday - 9 * ONE_DAY,
                endOfToday,
                "Last 10 Days"
            )

            TimeRangeType.THIS_MONTH -> {
                val monthCal = cal.clone() as Calendar
                monthCal.set(Calendar.DAY_OF_MONTH, 1)
                TimeRange(monthCal.timeInMillis, endOfToday, "This Month")
            }

            TimeRangeType.LAST_30_DAYS -> TimeRange(
                startOfToday - 29 * ONE_DAY,
                endOfToday,
                "Last 30 Days"
            )

            TimeRangeType.CUSTOM -> TimeRange(
                customStart ?: startOfToday,
                customEnd ?: endOfToday,
                "Custom Range"
            )
        }
    }
}
