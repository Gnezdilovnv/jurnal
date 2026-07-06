package com.example.reports.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private var logFile: File? = null

    fun init(context: Context) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val reportsDir = File(downloadsDir, "Reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            logFile = File(reportsDir, "app_log.txt")
            logFile?.createNewFile()
            writeLog("=== APP STARTED ===")
        } catch (_: Exception) {}
    }

    fun writeLog(msg: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            logFile?.appendText("[$timestamp] $msg\n")
        } catch (_: Exception) {}
    }

    fun writeError(msg: String, e: Throwable? = null) {
        val errorMsg = if (e != null) "$msg: ${e.message}" else msg
        writeLog("ERROR: $errorMsg")
    }
}
