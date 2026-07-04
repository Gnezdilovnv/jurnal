package com.example.reports.ui

import android.app.AlertDialog
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
import com.example.reports.data.Variable
import com.example.reports.utils.Logger
import kotlinx.coroutines.*

class VariablesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var variables = listOf<Variable>()
    private var categories = listOf<com.example.reports.data.Category>()
    private var subcategories = listOf<com.example.reports.data.Subcategory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_variables)
        recyclerView = findViewById(R.id.recyclerVariables)
        recyclerView.layoutManager = LinearLayoutManager(this)
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAddVariable).setOnClickListener { showAddDialog() }
        loadVariables()
    }

    private fun loadVariables() {
        scope.launch {
            try {
                variables = withContext(Dispatchers.IO) { db.variableDao().getAll() }
                categories = withContext(Dispatchers.IO) { db.categoryDao().getAll() }
                subcategories = withContext(Dispatchers.IO) { db.subcategoryDao().getAll() }
                updateAdapter()
            } catch (e: Exception) {
                Toast.makeText(this@VariablesActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateAdapter() {
        val items = variables.map { 
            val scope = when {
                it.subcategoryId != null -> "Подкатегория"
                it.showInAll -> "Глобальная"
                else -> "Корневая"
            }
            "${it.name} (${it.type}) - ${it.displayName} [$scope]"
        }
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as TextView).text = items[position]
            }
            override fun getItemCount() = items.size
        }
        recyclerView.adapter = adapter
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_variable, null)
        val etName = dialogView.findViewById<EditText>(R.id.etVarName)
        val etDisplayName = dialogView.findViewById<EditText>(R.id.etVarDisplayName)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerVarType)
        val rgScope = dialogView.findViewById<RadioGroup>(R.id.rgScope)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerSubcategory = dialogView.findViewById<Spinner>(R.id.spinnerSubcategory)
        val chkRequired = dialogView.findViewById<CheckBox>(R.id.chkRequired)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveVar)

        // Типы
        val types = arrayOf("TEXT", "NUMBER", "DATE", "TIME", "BOOLEAN", "SELECT", "LOCATION")
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter

        // Категории
        val catNames = categories.map { it.name }.toTypedArray()
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, catNames)
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = catAdapter

        // Подкатегории (загружаем после выбора категории)
        fun updateSubcategories(categoryId: String) {
            val subs = subcategories.filter { it.categoryId == categoryId }
            val subNames = subs.map { it.name }.toTypedArray()
            val subAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subNames)
            subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSubcategory.adapter = subAdapter
        }

        // Обновляем подкатегории при выборе категории
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val catId = categories[position].id
                updateSubcategories(catId)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Первоначальная загрузка подкатегорий
        if (categories.isNotEmpty()) {
            updateSubcategories(categories[0].id)
        }

        // Обновляем доступность спиннеров в зависимости от выбора RadioButton
        rgScope.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbGlobal -> {
                    spinnerCategory.isEnabled = false
                    spinnerSubcategory.isEnabled = false
                }
                R.id.rbRoot -> {
                    spinnerCategory.isEnabled = true
                    spinnerSubcategory.isEnabled = false
                }
                R.id.rbSub -> {
                    spinnerCategory.isEnabled = true
                    spinnerSubcategory.isEnabled = true
                }
            }
        }

        // Устанавливаем начальное состояние
        spinnerCategory.isEnabled = false
        spinnerSubcategory.isEnabled = false

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val displayName = etDisplayName.text.toString().trim()
            val type = spinnerType.selectedItem.toString()
            val required = chkRequired.isChecked

            if (name.isEmpty() || displayName.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Определяем scope
            val checkedId = rgScope.checkedRadioButtonId
            val subcategoryId = when (checkedId) {
                R.id.rbGlobal -> null
                R.id.rbRoot -> null
                R.id.rbSub -> {
                    val subPos = spinnerSubcategory.selectedItemPosition
                    if (subPos >= 0 && subPos < subcategories.size) {
                        subcategories[subPos].id
                    } else null
                }
                else -> null
            }

            val showInAll = checkedId == R.id.rbGlobal

            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        db.variableDao().insert(Variable(
                            name = name,
                            displayName = displayName,
                            type = type,
                            showInAll = showInAll,
                            subcategoryId = subcategoryId,
                            isRequired = required
                        ))
                    }
                    loadVariables()
                    Toast.makeText(this@VariablesActivity, "Переменная создана", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(this@VariablesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }
}
