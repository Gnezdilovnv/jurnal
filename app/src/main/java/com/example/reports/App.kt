package com.example.reports

import android.app.Application
import com.example.reports.utils.Logger

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Logger.init(this)
        Logger.writeLog("=== ПРИЛОЖЕНИЕ ЗАПУЩЕНО ===")
    }
}
