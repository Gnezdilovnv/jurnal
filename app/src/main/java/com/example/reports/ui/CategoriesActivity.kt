package com.example.reports.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Category
import com.example.reports.data.Subcategory
import com.example.reports.ui.adapters.CategoriesAdapter
import com.example.reports.utils.Logger
import kotlinx.coroutines.*

class CategoriesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var categories = listOf<Category>()
    private var subCounts = mapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        Logger.writeLog("CategoriesActivity started")

        recyclerView = findViewById(R.id.recyclerCategories)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAddCategory).setOnClickListener { showAddDialog() }

        loadData()
    }

    private fun loadData() {
        scope.launch {
            try {
                val cats = withContext(Dispatchers.IO) {
                    db.categoryDao().getAll()
                }
                val subs = withContext(Dispatchers.IO) {
                    db.subcategoryDao().getAll()
                }
                val counts = subs.groupBy { it.categoryId }.mapValues { it.value.size }
                categories = cats
                subCounts = counts
                updateAdapter()
                Logger.writeLog("Loaded ${cats.size} categories")
            } catch (e: Exception) {
                Logger.writeError("Load categories error", e)
            }
        }
    }

    private fun updateAdapter() {
        val adapter = CategoriesAdapter(categories, subCounts) { category ->
            showSubcategories(category)
        }
        recyclerView.adapter = adapter
    }

    private fun showSubcategories(category: Category) {
        Toast.makeText(this, "Подкатегории для ${category.name}", Toast.LENGTH_SHORT).show()
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
                            withContext(Dispatchers.IO) {
                                db.categoryDao().insert(Category(name = name))
                            }
                            loadData()
                            Toast.makeText(this@CategoriesActivity, "Категория создана", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Logger.writeError("Create category error", e)
                            Toast.makeText(this@CategoriesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
