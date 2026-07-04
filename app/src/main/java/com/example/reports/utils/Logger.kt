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
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dir != null && (dir.exists() || dir.mkdirs())) {
                logFile = File(dir, "reports_app_log.txt")
                logFile?.delete()
                logFile?.createNewFile()
                writeLog("=== APP STARTED ===")
            }
        } catch (_: Exception) {}
    }
    fun writeLog(msg: String) {
        try {
            logFile?.appendText("[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())}] $msg\n")
        } catch (_: Exception) {}
    }
}
