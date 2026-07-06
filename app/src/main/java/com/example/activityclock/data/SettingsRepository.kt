package com.example.activityclock.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("activity_clock_settings", Context.MODE_PRIVATE)

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _is24HourFormat = MutableStateFlow(prefs.getBoolean("is_24_hour", true))
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    private val _isMondayFirst = MutableStateFlow(prefs.getBoolean("is_monday_first", true))
    val isMondayFirst: StateFlow<Boolean> = _isMondayFirst.asStateFlow()

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
        _isDarkTheme.value = isDark
    }

    fun set24HourFormat(is24h: Boolean) {
        prefs.edit().putBoolean("is_24_hour", is24h).apply()
        _is24HourFormat.value = is24h
    }

    fun setMondayFirst(isMonday: Boolean) {
        prefs.edit().putBoolean("is_monday_first", isMonday).apply()
        _isMondayFirst.value = isMonday
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
