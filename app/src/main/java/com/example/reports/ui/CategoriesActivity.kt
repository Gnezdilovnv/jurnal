package com.example.reports.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Category
import com.example.reports.utils.Logger
import kotlinx.coroutines.*

class CategoriesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var categories = listOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)
        recyclerView = findViewById(R.id.recyclerCategories)
        recyclerView.layoutManager = LinearLayoutManager(this)
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAddCategory).setOnClickListener { showAddDialog() }
        loadCategories()
    }

    private fun loadCategories() {
        scope.launch {
            try {
                categories = withContext(Dispatchers.IO) { 
                    db.categoryDao().getAll() 
                }
                updateAdapter()
            } catch (e: Exception) {
                Toast.makeText(this@CategoriesActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateAdapter() {
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val category = categories[position]
                val tvName = holder.itemView.findViewById<TextView>(R.id.tvName)
                val tvSubCount = holder.itemView.findViewById<TextView>(R.id.tvSubCount)
                tvName.text = category.name
                tvSubCount.text = "ID: ${category.id}"
            }
            override fun getItemCount() = categories.size
        }
        recyclerView.adapter = adapter
    }

    private fun showAddDialog() {
        val input = EditText(this)
        input.hint = "Название категории"
        AlertDialog.Builder(this)
            .setTitle("Новая категория")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    scope.launch {
                        try {
                            val category = Category(name = name)
                            withContext(Dispatchers.IO) { db.categoryDao().insert(category) }
                            loadCategories()
                            Toast.makeText(this@CategoriesActivity, "Категория создана", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@CategoriesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
