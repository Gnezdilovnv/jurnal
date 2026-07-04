package com.example.reports.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Template
import com.example.reports.utils.Logger
import kotlinx.coroutines.*

class TemplateEditorActivity : AppCompatActivity() {
    private lateinit var etName: EditText
    private lateinit var etText: EditText
    private lateinit var tvVars: TextView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_editor)
        etName = findViewById(R.id.etTemplateName)
        etText = findViewById(R.id.etTemplateText)
        tvVars = findViewById(R.id.tvAvailableVars)
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSaveTemplate).setOnClickListener { saveTemplate() }
        loadVariables()
    }

    private fun loadVariables() {
        scope.launch {
            try {
                val vars = withContext(Dispatchers.IO) { db.variableDao().getAll() }
                tvVars.text = "Доступны: ${vars.joinToString(", ") { it.name }}"
            } catch (e: Exception) {
                Toast.makeText(this@TemplateEditorActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveTemplate() {
        val name = etName.text.toString().trim()
        val text = etText.text.toString().trim()
        if (name.isEmpty() || text.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.templateDao().insert(Template(name = name, text = text))
                }
                Toast.makeText(this@TemplateEditorActivity, "Шаблон сохранен", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@TemplateEditorActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
