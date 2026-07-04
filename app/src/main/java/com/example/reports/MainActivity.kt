package com.example.reports

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.reports.data.AppDatabase
import com.example.reports.data.Category
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            lifecycleScope.launch {
                db.categoryDao().insert(Category(name = "Тестовая категория"))
                Toast.makeText(this@MainActivity, "Категория сохранена", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
