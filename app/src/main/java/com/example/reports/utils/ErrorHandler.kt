package com.example.reports.utils

import android.content.Context
import android.widget.Toast

object ErrorHandler {
    fun showError(context: Context, message: String, throwable: Throwable? = null) {
        Logger.writeError(message, throwable)
        Toast.makeText(context, "Ошибка: $message", Toast.LENGTH_LONG).show()
    }

    fun showSuccess(context: Context, message: String) {
        Logger.writeLog("SUCCESS: $message")
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
