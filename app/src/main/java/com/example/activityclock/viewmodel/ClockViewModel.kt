package com.example.activityclock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.activityclock.data.ActivityLog
import com.example.activityclock.data.ActivityRepository
import com.example.activityclock.data.ActivityStats
import com.example.activityclock.data.ActivityType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class ClockViewModel(private val repository: ActivityRepository) : ViewModel() {

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
            1 -> { // Week (last 7 days)
                calendar.add(Calendar.DAY_OF_YEAR, -7)
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
}
