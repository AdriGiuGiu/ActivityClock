package com.example.activityclock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityclock.data.ActivityLog
import com.example.activityclock.data.ActivityRepository
import com.example.activityclock.data.ActivityStats
import com.example.activityclock.data.ActivityType
import com.example.activityclock.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class ClockViewModel(
    private val repository: ActivityRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    // List of all activity types
    private val _activities = MutableStateFlow<List<ActivityType>>(emptyList())
    val activities: StateFlow<List<ActivityType>> = _activities.asStateFlow()

    // Currently running activity log (if any)
    private val _activeLog = MutableStateFlow<ActivityLog?>(null)
    val activeLog: StateFlow<ActivityLog?> = _activeLog.asStateFlow()

    // Elapsed seconds for the active activity
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    // Recent activity logs (for the timeline view)
    private val _recentLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val recentLogs: StateFlow<List<ActivityLog>> = _recentLogs.asStateFlow()

    // Stats states
    private val _stats = MutableStateFlow<List<ActivityStats>>(emptyList())
    val stats: StateFlow<List<ActivityStats>> = _stats.asStateFlow()

    // Selected stats filter: 0 = Today, 1 = Week, 2 = Month, 3 = All-time
    private val _selectedFilter = MutableStateFlow(0)
    val selectedFilter: StateFlow<Int> = _selectedFilter.asStateFlow()

    // Expose 24-hour setting for UI formatting
    val is24HourFormat: StateFlow<Boolean> = settings.is24HourFormat

    private var timerJob: Job? = null

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch { loadActivities() }
        viewModelScope.launch { loadActiveLog() }
        viewModelScope.launch { loadRecentLogs() }
        loadStats()
    }

    fun refreshForTab(tabIndex: Int) {
        when (tabIndex) {
            0 -> {
                viewModelScope.launch { loadActiveLog() }
                viewModelScope.launch { loadActivities() }
            }
            1 -> {
                viewModelScope.launch { loadRecentLogs() }
                loadStats()
            }
            2 -> {
                viewModelScope.launch { loadActivities() }
            }
        }
    }

    private suspend fun loadActivities() {
        _activities.value = repository.getAllActivityTypes()
    }

    private suspend fun loadActiveLog() {
        val active = repository.getActiveLog()
        _activeLog.value = active
        if (active != null) {
            startTimer(active.startTimeMs)
        } else {
            stopTimer()
        }
    }

    private suspend fun loadRecentLogs() {
        _recentLogs.value = repository.getRecentLogs(30)
    }

    fun loadStats() {
        viewModelScope.launch {
            val (start, end) = getFilterTimeRange(_selectedFilter.value)
            _stats.value = repository.getStatsForPeriod(start, end)
        }
    }

    suspend fun generateCsvExport(): String {
        val allLogs = repository.getRecentLogs(Int.MAX_VALUE)
        val sb = java.lang.StringBuilder()
        sb.append("Activity Name,Start Date,Start Time,End Date,End Time,Duration (Seconds)\n")
        val dateFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val timeFmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        allLogs.forEach { log ->
            val startD = dateFmt.format(java.util.Date(log.startTimeMs))
            val startT = timeFmt.format(java.util.Date(log.startTimeMs))
            val endD = if (log.endTimeMs != null) dateFmt.format(java.util.Date(log.endTimeMs)) else "Ongoing"
            val endT = if (log.endTimeMs != null) timeFmt.format(java.util.Date(log.endTimeMs)) else ""
            // Escape activity name to avoid CSV breaking on commas
            val escapedName = if (log.activityName.contains(",")) "\"${log.activityName}\"" else log.activityName
            sb.append("${escapedName},${startD},${startT},${endD},${endT},${log.durationSeconds}\n")
        }
        return sb.toString()
    }

    fun setFilter(filterType: Int) {
        _selectedFilter.value = filterType
        loadStats()
    }

    // Action Handlers
    fun startActivity(activityId: Int) {
        viewModelScope.launch {
            repository.startActivity(activityId)
            launch { loadActiveLog() }
            launch { loadRecentLogs() }
            loadStats()
        }
    }

    fun stopActiveActivity() {
        viewModelScope.launch {
            repository.stopActiveActivity()
            launch { loadActiveLog() }
            launch { loadRecentLogs() }
            loadStats()
        }
    }

    fun addNewActivity(name: String, colorHex: String) {
        viewModelScope.launch {
            val id = repository.insertActivityType(name, colorHex)
            if (id != -1L) {
                loadActivities()
            }
        }
    }

    // Timer Logic
    private fun startTimer(startTimeMs: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _elapsedSeconds.value = maxOf(0L, (now - startTimeMs) / 1000)
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _elapsedSeconds.value = 0L
    }

    private fun getFilterTimeRange(filterType: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val nowMs = calendar.timeInMillis

        return when (filterType) {
            0 -> { // Today (from 00:00:00)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, nowMs)
            }
            1 -> { // Week (start of current week)
                val isMondayFirst = settings.isMondayFirst.value
                calendar.firstDayOfWeek = if (isMondayFirst) Calendar.MONDAY else Calendar.SUNDAY
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, nowMs)
            }
            2 -> { // Month (last 30 days)
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                Pair(calendar.timeInMillis, nowMs)
            }
            else -> { // All-time
                Pair(0L, nowMs)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    fun deleteActivity(activityId: Int) {
        viewModelScope.launch {
            repository.deleteActivity(activityId)
            refreshAll()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
            refreshAll()
        }
    }

    fun clearAllActivities() {
        viewModelScope.launch {
            repository.clearAllActivities()
            refreshAll()
        }
    }
}
