package com.zestyy.struct.util

import com.zestyy.struct.data.db.entities.SavedRouteEntity
import java.util.Calendar
import java.util.TimeZone

data class WeeklySummary(
    val activityCount: Int,
    val totalDistanceMeters: Double,
    val totalDurationMillis: Long,
    val currentStreakDays: Int
)

object ActivityStats {

    /** Sunday-start week containing "now", local time zone. */
    fun weeklySummary(routes: List<SavedRouteEntity>, now: Long = System.currentTimeMillis()): WeeklySummary {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }
        val weekStart = cal.timeInMillis

        val thisWeek = routes.filter { it.createdAtMillis >= weekStart }
        val streak = currentStreakDays(routes, now)

        return WeeklySummary(
            activityCount = thisWeek.size,
            totalDistanceMeters = thisWeek.sumOf { it.distanceMeters },
            totalDurationMillis = thisWeek.sumOf { it.durationMillis },
            currentStreakDays = streak
        )
    }

    /**
     * Consecutive-day streak of "recorded at least one activity", counting back from today.
     * A gap of a full day with nothing recorded breaks the streak. Only counts RECORDED
     * activities (built-but-unwalked routes don't count toward a streak).
     */
    private fun currentStreakDays(routes: List<SavedRouteEntity>, now: Long): Int {
        val tz = TimeZone.getDefault()
        val recordedDays = routes
            .filter { it.type == com.zestyy.struct.data.db.entities.RouteType.RECORDED }
            .map { dayBucket(it.createdAtMillis, tz) }
            .toHashSet()

        if (recordedDays.isEmpty()) return 0

        var streak = 0
        var cursor = dayBucket(now, tz)
        // if nothing recorded today yet, streak can still be "alive" through yesterday
        if (cursor !in recordedDays) cursor -= 1
        while (cursor in recordedDays) {
            streak++
            cursor -= 1
        }
        return streak
    }

    private fun dayBucket(millis: Long, tz: TimeZone): Long {
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis / (24L * 60 * 60 * 1000)
    }
}
