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
import com.example.reports.data.Subcategory
import com.example.reports.utils.Logger
import kotlinx.coroutines.*

class CategoriesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var categories = listOf<Category>()
    private var subcategories = listOf<Subcategory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)
        recyclerView = findViewById(R.id.recyclerCategories)
        recyclerView.layoutManager = LinearLayoutManager(this)
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAddCategory).setOnClickListener { showAddCategoryDialog() }
        loadData()
    }

    private fun loadData() {
        scope.launch {
            try {
                categories = withContext(Dispatchers.IO) { db.categoryDao().getAll() }
                subcategories = withContext(Dispatchers.IO) { db.subcategoryDao().getAll() }
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
                val btnSub = holder.itemView.findViewById<Button>(R.id.btnSubcategories)

                tvName.text = "${category.name} (${category.id})"
                val subCount = subcategories.count { it.categoryId == category.id }
                tvSubCount.text = "$subCount подкатегорий"

                // Клик по категории - редактирование
                holder.itemView.setOnClickListener {
                    showEditCategoryDialog(category)
                }

                // Долгий клик - удаление
                holder.itemView.setOnLongClickListener {
                    showDeleteCategoryDialog(category)
                    true
                }

                // Кнопка подкатегорий
                btnSub.setOnClickListener {
                    showSubcategoriesDialog(category)
                }
            }

            override fun getItemCount() = categories.size
        }
        recyclerView.adapter = adapter
    }

    // ========== ДИАЛОГ СОЗДАНИЯ КАТЕГОРИИ (С ID) ==========
    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_category, null)
        val etId = dialogView.findViewById<EditText>(R.id.etCategoryId)
        val etName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveCategory)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Новая категория")
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .create()

        btnSave.setOnClickListener {
            val id = etId.text.toString().trim()
            val name = etName.text.toString().trim()

            if (id.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Заполните ID и название", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scope.launch {
                try {
                    val category = Category(id = id, name = name)
                    Logger.writeLog("Создана категория: ${category.name} (ID: ${category.id})")
                    withContext(Dispatchers.IO) { db.categoryDao().insert(category) }
                    loadData()
                    Toast.makeText(this@CategoriesActivity, "Категория создана", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(this@CategoriesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    // ========== РЕДАКТИРОВАНИЕ КАТЕГОРИИ ==========
    private fun showEditCategoryDialog(category: Category) {
        val input = EditText(this)
        input.setText(category.name)

        AlertDialog.Builder(this)
            .setTitle("Редактировать категорию (ID: ${category.id})")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                db.categoryDao().update(category.copy(name = name))
                            }
                            loadData()
                            Toast.makeText(this@CategoriesActivity, "Категория обновлена", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@CategoriesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ========== УДАЛЕНИЕ КАТЕГОРИИ ==========
    private fun showDeleteCategoryDialog(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Удалить категорию?")
            .setMessage("ID: ${category.id}\nВсе подкатегории будут удалены")
            .setPositiveButton("Удалить") { _, _ ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            db.categoryDao().delete(category)
                            // Удаляем все подкатегории этой категории
                            val subs = db.subcategoryDao().getByCategoryId(category.id)
                            subs.forEach { db.subcategoryDao().delete(it) }
                        }
                        loadData()
                        Toast.makeText(this@CategoriesActivity, "Категория удалена", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@CategoriesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ========== ПОДКАТЕГОРИИ ==========
    private fun showSubcategoriesDialog(category: Category) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_subcategories, null)
        val listView = dialogView.findViewById<android.widget.ListView>(R.id.listSubcategories)
        val etSubId = dialogView.findViewById<EditText>(R.id.etSubId)
        val etSubName = dialogView.findViewById<EditText>(R.id.etSubName)
        val btnAdd = dialogView.findViewById<Button>(R.id.btnAddSubcategory)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Подкатегории: ${category.name}")
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .create()

        // Загружаем подкатегории
        fun loadSubs() {
            scope.launch {
                val subs = withContext(Dispatchers.IO) {
                    db.subcategoryDao().getByCategoryId(category.id)
                }
                val names = subs.map { "${it.name} (${it.id})" }
                val adapter = android.widget.ArrayAdapter(
                    this@CategoriesActivity,
                    android.R.layout.simple_list_item_1,
                    names
                )
                listView.adapter = adapter
            }
        }

        // Добавление подкатегории
        btnAdd.setOnClickListener {
            val id = etSubId.text.toString().trim()
            val name = etSubName.text.toString().trim()

            if (id.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Заполните ID и название", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        db.subcategoryDao().insert(
                            Subcategory(
                                id = id,
                                categoryId = category.id,
                                name = name
                            )
                        )
                    }
                    Toast.makeText(this@CategoriesActivity, "Подкатегория создана", Toast.LENGTH_SHORT).show()
                    loadSubs()
                    loadData()
                    etSubId.text.clear()
                    etSubName.text.clear()
                } catch (e: Exception) {
                    Toast.makeText(this@CategoriesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loadSubs()
        dialog.show()
    }
}
