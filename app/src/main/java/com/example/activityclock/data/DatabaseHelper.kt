package com.example.activityclock.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DatabaseHelper"
        private const val DATABASE_NAME = "activity_clock.db"
        private const val DATABASE_VERSION = 2

        // Table Activities
        const val TABLE_ACTIVITIES = "activities"
        const val COL_ACT_ID = "id"
        const val COL_ACT_NAME = "name"
        const val COL_ACT_COLOR = "color"

        // Table Logs
        const val TABLE_LOGS = "activity_logs"
        const val COL_LOG_ID = "id"
        const val COL_LOG_ACT_ID = "activity_id"
        const val COL_LOG_START_TIME = "start_time"
        const val COL_LOG_END_TIME = "end_time"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createActivitiesTable = """
            CREATE TABLE $TABLE_ACTIVITIES (
                $COL_ACT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ACT_NAME TEXT UNIQUE NOT NULL,
                $COL_ACT_COLOR TEXT NOT NULL
            )
        """.trimIndent()

        val createLogsTable = """
            CREATE TABLE $TABLE_LOGS (
                $COL_LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_LOG_ACT_ID INTEGER NOT NULL,
                $COL_LOG_START_TIME INTEGER NOT NULL,
                $COL_LOG_END_TIME INTEGER,
                FOREIGN KEY ($COL_LOG_ACT_ID) REFERENCES $TABLE_ACTIVITIES ($COL_ACT_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createActivitiesTable)
        db.execSQL(createLogsTable)

        // Create indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_start_time ON $TABLE_LOGS ($COL_LOG_START_TIME)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_end_time ON $TABLE_LOGS ($COL_LOG_END_TIME)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_act_id ON $TABLE_LOGS ($COL_LOG_ACT_ID)")

        // Seed default activities
        val defaultActivities = listOf(
            Pair("Working", "#00E5FF"), // Cyan
            Pair("Gym", "#9D4EDD"),     // Violet
            Pair("Gaming", "#00F5D4"),  // Emerald
            Pair("Relaxing", "#FFB703"),// Amber
            Pair("Eating", "#FF4D6D")    // Rose
        )

        for (activity in defaultActivities) {
            val values = ContentValues().apply {
                put(COL_ACT_NAME, activity.first)
                put(COL_ACT_COLOR, activity.second)
            }
            db.insert(TABLE_ACTIVITIES, null, values)
        }
        Log.d(TAG, "Database tables created and seeded successfully.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_start_time ON $TABLE_LOGS ($COL_LOG_START_TIME)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_end_time ON $TABLE_LOGS ($COL_LOG_END_TIME)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_act_id ON $TABLE_LOGS ($COL_LOG_ACT_ID)")
        } else {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_LOGS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_ACTIVITIES")
            onCreate(db)
        }
    }
}
