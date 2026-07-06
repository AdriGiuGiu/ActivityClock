package com.example.activityclock.data

data class ActivityType(
    val id: Int,
    val name: String,
    val colorHex: String
)

data class ActivityLog(
    val id: Int,
    val activityId: Int,
    val activityName: String,
    val activityColorHex: String,
    val startTimeMs: Long,
    val endTimeMs: Long? // null if currently running
) {
    val durationSeconds: Long
        get() {
            val end = endTimeMs ?: System.currentTimeMillis()
            return (end - startTimeMs) / 1000
        }
}

data class ActivityStats(
    val activityId: Int,
    val activityName: String,
    val activityColorHex: String,
    val totalDurationSeconds: Long,
    val percentage: Float = 0f
)
