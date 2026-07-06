package com.example.reports.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Template
import com.example.reports.utils.ErrorHandler
import kotlinx.coroutines.*

class TemplatesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var templates = listOf<Template>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_templates)
        recyclerView = findViewById(R.id.recyclerTemplates)
        recyclerView.layoutManager = LinearLayoutManager(this)
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAddTemplate).setOnClickListener {
            startActivity(Intent(this, TemplateEditorActivity::class.java))
        }
        loadTemplates()
    }

    private fun loadTemplates() {
        scope.launch {
            try {
                templates = withContext(Dispatchers.IO) { db.templateDao().getAll() }
                updateAdapter()
            } catch (e: Exception) {
                ErrorHandler.showError(this@TemplatesActivity, "Загрузка шаблонов", e)
            }
        }
    }

    private fun updateAdapter() {
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val template = templates[position]
                val tv = holder.itemView as TextView
                tv.text = template.name

                holder.itemView.setOnClickListener {
                    val intent = Intent(this@TemplatesActivity, TemplateEditorActivity::class.java)
                    intent.putExtra("template_id", template.id)
                    startActivity(intent)
                }

                holder.itemView.setOnLongClickListener {
                    showDeleteDialog(template)
                    true
                }
            }

            override fun getItemCount() = templates.size
        }
        recyclerView.adapter = adapter
    }

    private fun showDeleteDialog(template: Template) {
        AlertDialog.Builder(this)
            .setTitle("Удалить шаблон?")
            .setMessage("${template.name}\nПосле удаления восстановить будет невозможно")
            .setPositiveButton("Удалить") { _, _ ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { db.templateDao().delete(template) }
                        loadTemplates()
                        ErrorHandler.showSuccess(this@TemplatesActivity, "Шаблон удален")
                    } catch (e: Exception) {
                        ErrorHandler.showError(this@TemplatesActivity, "Удаление шаблона", e)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadTemplates()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}