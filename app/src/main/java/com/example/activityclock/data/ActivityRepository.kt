package com.example.activityclock.data

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActivityRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    suspend fun getAllActivityTypes(): List<ActivityType> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ActivityType>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_ACTIVITIES,
            arrayOf(DatabaseHelper.COL_ACT_ID, DatabaseHelper.COL_ACT_NAME, DatabaseHelper.COL_ACT_COLOR),
            null, null, null, null, "${DatabaseHelper.COL_ACT_NAME} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_ID))
                val name = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_NAME))
                val color = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_COLOR))
                list.add(ActivityType(id, name, color))
            }
        }
        list
    }

    suspend fun insertActivityType(name: String, colorHex: String): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_ACT_NAME, name)
            put(DatabaseHelper.COL_ACT_COLOR, colorHex)
        }
        try {
            db.insertWithOnConflict(
                DatabaseHelper.TABLE_ACTIVITIES,
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
            )
        } catch (e: Exception) {
            -1L
        }
    }

    suspend fun getActiveLog(): ActivityLog? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT l.${DatabaseHelper.COL_LOG_ID}, l.${DatabaseHelper.COL_LOG_ACT_ID}, 
                   a.${DatabaseHelper.COL_ACT_NAME}, a.${DatabaseHelper.COL_ACT_COLOR},
                   l.${DatabaseHelper.COL_LOG_START_TIME}, l.${DatabaseHelper.COL_LOG_END_TIME}
            FROM ${DatabaseHelper.TABLE_LOGS} l
            INNER JOIN ${DatabaseHelper.TABLE_ACTIVITIES} a ON l.${DatabaseHelper.COL_LOG_ACT_ID} = a.${DatabaseHelper.COL_ACT_ID}
            WHERE l.${DatabaseHelper.COL_LOG_END_TIME} IS NULL
            LIMIT 1
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            if (it.moveToFirst()) {
                val id = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_ID))
                val actId = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_ACT_ID))
                val name = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_NAME))
                val color = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_COLOR))
                val startTime = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_START_TIME))
                val endTime = if (it.isNull(it.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_END_TIME))) {
                    null
                } else {
                    it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_END_TIME))
                }
                return@withContext ActivityLog(id, actId, name, color, startTime, endTime)
            }
        }
        null
    }

    suspend fun startActivity(activityId: Int): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            // 1. Close any currently running activity logs
            val valuesClose = ContentValues().apply {
                put(DatabaseHelper.COL_LOG_END_TIME, now)
            }
            db.update(
                DatabaseHelper.TABLE_LOGS,
                valuesClose,
                "${DatabaseHelper.COL_LOG_END_TIME} IS NULL",
                null
            )

            // 2. Insert new activity log
            val valuesNew = ContentValues().apply {
                put(DatabaseHelper.COL_LOG_ACT_ID, activityId)
                put(DatabaseHelper.COL_LOG_START_TIME, now)
                // end_time remains null
            }
            db.insert(DatabaseHelper.TABLE_LOGS, null, valuesNew)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    suspend fun stopActiveActivity(): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_LOG_END_TIME, now)
        }
        db.update(
            DatabaseHelper.TABLE_LOGS,
            values,
            "${DatabaseHelper.COL_LOG_END_TIME} IS NULL",
            null
        )
    }

    suspend fun getRecentLogs(limit: Int = 50): List<ActivityLog> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ActivityLog>()
        val db = dbHelper.readableDatabase
        val query = """
            SELECT l.${DatabaseHelper.COL_LOG_ID}, l.${DatabaseHelper.COL_LOG_ACT_ID}, 
                   a.${DatabaseHelper.COL_ACT_NAME}, a.${DatabaseHelper.COL_ACT_COLOR},
                   l.${DatabaseHelper.COL_LOG_START_TIME}, l.${DatabaseHelper.COL_LOG_END_TIME}
            FROM ${DatabaseHelper.TABLE_LOGS} l
            INNER JOIN ${DatabaseHelper.TABLE_ACTIVITIES} a ON l.${DatabaseHelper.COL_LOG_ACT_ID} = a.${DatabaseHelper.COL_ACT_ID}
            ORDER BY l.${DatabaseHelper.COL_LOG_START_TIME} DESC
            LIMIT ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(limit.toString()))
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_ID))
                val actId = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_ACT_ID))
                val name = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_NAME))
                val color = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_COLOR))
                val startTime = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_START_TIME))
                val endTime = if (it.isNull(it.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_END_TIME))) {
                    null
                } else {
                    it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COL_LOG_END_TIME))
                }
                list.add(ActivityLog(id, actId, name, color, startTime, endTime))
            }
        }
        list
    }

    suspend fun getStatsForPeriod(startTimeMs: Long, endTimeMs: Long): List<ActivityStats> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val now = System.currentTimeMillis()
        
        val query = """
            SELECT a.${DatabaseHelper.COL_ACT_ID}, a.${DatabaseHelper.COL_ACT_NAME}, a.${DatabaseHelper.COL_ACT_COLOR},
                   SUM(
                       MAX(0, MIN(IFNULL(l.${DatabaseHelper.COL_LOG_END_TIME}, ?), ?) - MAX(l.${DatabaseHelper.COL_LOG_START_TIME}, ?))
                   ) / 1000 AS duration_sec
            FROM ${DatabaseHelper.TABLE_LOGS} l
            INNER JOIN ${DatabaseHelper.TABLE_ACTIVITIES} a ON l.${DatabaseHelper.COL_LOG_ACT_ID} = a.${DatabaseHelper.COL_ACT_ID}
            WHERE l.${DatabaseHelper.COL_LOG_START_TIME} < ? 
              AND (l.${DatabaseHelper.COL_LOG_END_TIME} IS NULL OR l.${DatabaseHelper.COL_LOG_END_TIME} > ?)
            GROUP BY a.${DatabaseHelper.COL_ACT_ID}, a.${DatabaseHelper.COL_ACT_NAME}, a.${DatabaseHelper.COL_ACT_COLOR}
            HAVING duration_sec > 0
            ORDER BY duration_sec DESC
        """.trimIndent()

        val cursor = db.rawQuery(
            query, 
            arrayOf(now.toString(), endTimeMs.toString(), startTimeMs.toString(), endTimeMs.toString(), startTimeMs.toString())
        )
        
        val statsList = mutableListOf<ActivityStats>()
        var totalSec = 0L
        
        cursor.use {
            while (it.moveToNext()) {
                val actId = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_ID))
                val name = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_NAME))
                val color = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COL_ACT_COLOR))
                val durationSec = it.getLong(it.getColumnIndexOrThrow("duration_sec"))
                
                totalSec += durationSec
                statsList.add(ActivityStats(actId, name, color, durationSec, 0f))
            }
        }

        val totalSecFloat = totalSec.toFloat()
        if (totalSecFloat > 0) {
            statsList.map { it.copy(percentage = it.totalDurationSeconds / totalSecFloat) }
        } else {
            statsList
        }
    }

    suspend fun deleteActivity(activityId: Int): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            DatabaseHelper.TABLE_ACTIVITIES,
            "${DatabaseHelper.COL_ACT_ID} = ?",
            arrayOf(activityId.toString())
        )
    }

    suspend fun clearAllLogs(): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_LOGS, null, null)
    }

    suspend fun clearAllActivities(): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_ACTIVITIES, null, null)
    }
}
