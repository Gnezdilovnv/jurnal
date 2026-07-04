package com.example.reports.ui
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.R
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_settings)
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }
}
