package com.example.reports

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.ui.*
import com.example.reports.utils.Logger
import com.example.reports.utils.ErrorHandler
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Logger.init(this)
        Logger.writeLog("MainActivity started")

        findViewById<Button>(R.id.btnCreateReport).setOnClickListener {
            startActivity(Intent(this, CreateReportActivity::class.java))
        }

        findViewById<Button>(R.id.btnReportsList).setOnClickListener {
            startActivity(Intent(this, ReportsListActivity::class.java))
        }

        findViewById<Button>(R.id.btnTemplates).setOnClickListener {
            startActivity(Intent(this, TemplatesActivity::class.java))
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
