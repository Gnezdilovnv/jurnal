package com.example.reports.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Template
import com.example.reports.utils.Logger
import kotlinx.coroutines.*

class TemplatesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var templates = listOf<Template>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_templates)

        Logger.writeLog("TemplatesActivity started")

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
                templates = withContext(Dispatchers.IO) {
                    db.templateDao().getAll()
                }
                val items = templates.map { it.name }
                val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                        object : RecyclerView.ViewHolder(
                            LayoutInflater.from(parent.context)
                                .inflate(android.R.layout.simple_list_item_1, parent, false)
                        ) {}

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                        val tv = holder.itemView as android.widget.TextView
                        tv.text = items[position]
                    }

                    override fun getItemCount() = items.size
                }
                recyclerView.adapter = adapter
                Logger.writeLog("Loaded ${templates.size} templates")
            } catch (e: Exception) {
                Logger.writeError("Load templates error", e)
            }
        }
    }
}
