Вот исправленный код с устранением всех ошибок:

package com.example.reports.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Template
import com.example.reports.data.Variable
import com.example.reports.utils.Logger
import kotlinx.coroutines.*

class TemplateEditorActivity : AppCompatActivity() {
    private lateinit var etName: EditText
    private lateinit var etText: EditText
    private lateinit var tvVars: TextView
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerSubcategory: Spinner
    private lateinit var recyclerVariables: RecyclerView

    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var allVariables = listOf<Variable>()
    private var filteredVariables = listOf<Variable>()
    private var categories = listOf<com.example.reports.data.Category>()
    private var subcategories = listOf<com.example.reports.data.Subcategory>()
    private var editingTemplateId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_editor)

        editingTemplateId = intent.getStringExtra("template_id")

        initViews()
        loadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun initViews() {
        etName = findViewById(R.id.etTemplateName)
        etText = findViewById(R.id.etTemplateText)
        tvVars = findViewById(R.id.tvAvailableVars)
        spinnerCategory = findViewById(R.id.spinnerCategoryFilter)
        spinnerSubcategory = findViewById(R.id.spinnerSubcategoryFilter)
        recyclerVariables = findViewById(R.id.recyclerVariables)
        recyclerVariables.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSaveTemplate).setOnClickListener { saveTemplate() }
    }

    private fun loadData() {
        scope.launch {
            try {
                allVariables = withContext(Dispatchers.IO) { db.variableDao().getAll() }
                categories = withContext(Dispatchers.IO) { db.categoryDao().getAll() }
                subcategories = withContext(Dispatchers.IO) { db.subcategoryDao().getAll() }

                // Если редактируем, загружаем шаблон
                if (editingTemplateId != null) {
                    val template = withContext(Dispatchers.IO) {
                        db.templateDao().getById(editingTemplateId!!)
                    }
                    template?.let {
                        etName.setText(it.name)
                        etText.setText(it.text)
                    }
                }

                setupSpinners()
                filterVariables()
                updateVariablesList()
            } catch (e: Exception) {
                Logger.e("TemplateEditorActivity", "Ошибка загрузки данных", e)
                Toast.makeText(this@TemplateEditorActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpinners() {
        val catNames = listOf("Все категории") + categories.map { it.name }
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, catNames)
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = catAdapter

        fun updateSubcategories(categoryId: String?) {
            val subs = if (categoryId == null) {
                listOf("Все подкатегории")
            } else {
                listOf("Все подкатегории") + subcategories.filter { it.categoryId == categoryId }.map { it.name }
            }
            val subAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subs)
            subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSubcategory.adapter = subAdapter
        }

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = parent.getItemAtPosition(position).toString()
                val catId = if (selected == "Все категории") null else categories.find { it.name == selected }?.id
                updateSubcategories(catId)
                filterVariables()
                updateVariablesList()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerSubcategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterVariables()
                updateVariablesList()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        if (categories.isNotEmpty()) {
            updateSubcategories(categories[0].id)
        }
    }

    private fun filterVariables() {
        val catName = spinnerCategory.selectedItem?.toString() ?: "Все категории"
        val subName = spinnerSubcategory.selectedItem?.toString() ?: "Все подкатегории"

        filteredVariables = allVariables.filter { variable ->
            var matches = true

            if (variable.showInAll) return@filter true

            if (catName != "Все категории") {
                val category = categories.find { it.name == catName }
                if (category != null) {
                    if (variable.subcategoryId != null) {
                        val sub = subcategories.find { it.id == variable.subcategoryId }
                        if (sub?.categoryId != category.id) matches = false
                    } else {
                        matches = false
                    }
                }
            }

            if (matches && subName != "Все подкатегории") {
                val subcategory = subcategories.find { it.name == subName }
                if (subcategory != null) {
                    if (variable.subcategoryId != subcategory.id) matches = false
                }
            }

            matches
        }

        val globalVars = allVariables.filter { it.showInAll }
        filteredVariables = (filteredVariables + globalVars).distinctBy { it.id }
    }

    private fun updateVariablesList() {
        val varNames = filteredVariables.map { "${it.name} (${it.type}) - ${it.displayName}" }
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val tv = holder.itemView as TextView
                tv.text = varNames[position]
                tv.setOnClickListener {
                    val varName = filteredVariables[position].name
                    val currentText = etText.text.toString()
                    val cursorPosition = etText.selectionStart
                    val newText = currentText.substring(0, cursorPosition) +
                            "{$varName}" +
                            currentText.substring(cursorPosition)
                    etText.setText(newText)
                    etText.setSelection(cursorPosition + varName.length + 2)
                }
            }

            override fun getItemCount() = varNames.size
        }
        recyclerVariables.adapter = adapter
        tvVars.text = "Доступно: ${filteredVariables.size} переменных (нажмите для вставки)"
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
                if (editingTemplateId != null) {
                    // Обновление
                    val existing = withContext(Dispatchers.IO) {
                        db.templateDao().getById(editingTemplateId!!)
                    }
                    existing?.let {
                        withContext(Dispatchers.IO) {
                            db.templateDao().update(it.copy(name = name, text = text))
                        }
                        Toast.makeText(this@TemplateEditorActivity, "Шаблон обновлен", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Toast.makeText(this@TemplateEditorActivity, "Шаблон не найден", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Создание
                    withContext(Dispatchers.IO) {
                        db.templateDao().insert(Template(name = name, text = text))
                    }
                    Toast.makeText(this@TemplateEditorActivity, "Шаблон сохранен", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: Exception) {
                Logger.e("TemplateEditorActivity", "Ошибка сохранения шаблона", e)
                Toast.makeText(this@TemplateEditorActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
**Основные исправления:**

1. **Добавлен SupervisorJob()** в CoroutineScope для правильной обработки ошибок в корутинах
2. **Добавлен onDestroy()** с отменой scope для предотвращения утечек памяти
3. **Добавлено логирование ошибок** через Logger.e() для лучшей отладки
4. **Добавлена обработка случая**, когда шаблон не найден при редактировании
5. **Улучшены сообщения об ошибках** - теперь показывается текст ошибки
6. **Добавлен импорт Logger** для использования логирования

Код теперь более безопасный, с правильной обработкой жизненного цикла и ошибок.