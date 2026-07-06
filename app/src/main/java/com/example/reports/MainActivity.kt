package com.example.reports

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.ui.*
import com.example.reports.utils.Logger
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Logger.init(this)
        Logger.writeLog("MainActivity started")

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Уже на главной
                    true
                }
                R.id.nav_templates -> {
                    startActivity(Intent(this, TemplatesActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, ReportsListActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }

        // FAB для быстрого создания отчета
        val fab = findViewById<FloatingActionButton>(R.id.fab_create_report)
        fab.setOnClickListener {
            startActivity(Intent(this, CreateReportActivity::class.java))
        }
    }
}