package com.example.reports.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.R

class ReportsListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports_list)
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        Toast.makeText(this, "Список отчетов", Toast.LENGTH_SHORT).show()
    }
}
