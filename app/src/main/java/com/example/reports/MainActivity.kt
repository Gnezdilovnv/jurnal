package com.example.reports

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.ui.*
import com.example.reports.utils.Logger

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Logger.init(this)
        Logger.writeLog("MainActivity started")

        findViewById<Button>(R.id.btnCreateReport).setOnClickListener {
            startActivity(Intent(this, TemplatesActivity::class.java))
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
