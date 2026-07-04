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
        loadData()
    }

    private fun loadData() {
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

    private fun getScopeText(variable: Variable): String {
        return when {
            variable.showInAll -> "🌍 Глобальная"
            variable.subcategoryId != null -> {
                val sub = subcategories.find { it.id == variable.subcategoryId }
                if (sub != null) {
                    val cat = categories.find { it.id == sub.categoryId }
                    "📂 ${cat?.name ?: "?"} → ${sub.name}"
                } else {
                    "📂 Без категории"
                }
            }
            else -> "📁 Без категории"
        }
    }

    private fun updateAdapter() {
        val items = variables.map { varItem ->
            val scopeText = getScopeText(varItem)
            "${varItem.name} (${varItem.type}) - ${varItem.displayName} [$scopeText]"
        }
        
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val tv = holder.itemView as TextView
                tv.text = items[position]
                
                // Клик — редактирование
                holder.itemView.setOnClickListener {
                    showEditDialog(variables[position])
                }
                
                // Долгий клик — удаление
                holder.itemView.setOnLongClickListener {
                    showDeleteDialog(variables[position])
                    true
                }
            }

            override fun getItemCount() = items.size
        }
        recyclerView.adapter = adapter
    }

    // ========== ДИАЛОГ СОЗДАНИЯ ПЕРЕМЕННОЙ ==========
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

        setupTypeSpinner(spinnerType)
        setupCategorySpinners(spinnerCategory, spinnerSubcategory)
        setupScopeListeners(rgScope, spinnerCategory, spinnerSubcategory)

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

            val (showInAll, subcategoryId) = getScopeData(rgScope, spinnerSubcategory)

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
                    loadData()
                    Toast.makeText(this@VariablesActivity, "Переменная создана", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(this@VariablesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    // ========== РЕДАКТИРОВАНИЕ ПЕРЕМЕННОЙ ==========
    private fun showEditDialog(variable: Variable) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_variable, null)
        val etName = dialogView.findViewById<EditText>(R.id.etVarName)
        val etDisplayName = dialogView.findViewById<EditText>(R.id.etVarDisplayName)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerVarType)
        val rgScope = dialogView.findViewById<RadioGroup>(R.id.rgScope)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerSubcategory = dialogView.findViewById<Spinner>(R.id.spinnerSubcategory)
        val chkRequired = dialogView.findViewById<CheckBox>(R.id.chkRequired)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveVar)

        // Заполняем текущими значениями
        etName.setText(variable.name)
        etDisplayName.setText(variable.displayName)
        chkRequired.isChecked = variable.isRequired

        setupTypeSpinner(spinnerType)
        spinnerType.setSelection(listOf("TEXT", "NUMBER", "DATE", "TIME", "BOOLEAN", "SELECT", "LOCATION").indexOf(variable.type))

        // Устанавливаем принадлежность
        val scopeIndex = when {
            variable.showInAll -> 0
            variable.subcategoryId != null -> 2
            else -> 1
        }
        rgScope.check(R.id.rbGlobal)
        when (scopeIndex) {
            0 -> rgScope.check(R.id.rbGlobal)
            1 -> rgScope.check(R.id.rbRoot)
            2 -> rgScope.check(R.id.rbSub)
        }

        setupCategorySpinners(spinnerCategory, spinnerSubcategory)
        setupScopeListeners(rgScope, spinnerCategory, spinnerSubcategory)

        // Устанавливаем выбранные категории
        if (variable.subcategoryId != null) {
            val sub = subcategories.find { it.id == variable.subcategoryId }
            if (sub != null) {
                val catIndex = categories.indexOfFirst { it.id == sub.categoryId }
                if (catIndex >= 0) spinnerCategory.setSelection(catIndex)
                val subIndex = subcategories.filter { it.categoryId == sub.categoryId }.indexOfFirst { it.id == sub.id }
                if (subIndex >= 0) spinnerSubcategory.setSelection(subIndex)
            }
        }

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

            val (showInAll, subcategoryId) = getScopeData(rgScope, spinnerSubcategory)

            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        db.variableDao().update(variable.copy(
                            name = name,
                            displayName = displayName,
                            type = type,
                            showInAll = showInAll,
                            subcategoryId = subcategoryId,
                            isRequired = required
                        ))
                    }
                    loadData()
                    Toast.makeText(this@VariablesActivity, "Переменная обновлена", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(this@VariablesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    // ========== УДАЛЕНИЕ ПЕРЕМЕННОЙ ==========
    private fun showDeleteDialog(variable: Variable) {
        AlertDialog.Builder(this)
            .setTitle("Удалить переменную?")
            .setMessage("${variable.name} (${variable.displayName})\nОна может использоваться в шаблонах")
            .setPositiveButton("Удалить") { _, _ ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { db.variableDao().delete(variable) }
                        loadData()
                        Toast.makeText(this@VariablesActivity, "Переменная удалена", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@VariablesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHOW).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========
    private fun setupTypeSpinner(spinner: Spinner) {
        val types = arrayOf("TEXT", "NUMBER", "DATE", "TIME", "BOOLEAN", "SELECT", "LOCATION")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun setupCategorySpinners(catSpinner: Spinner, subSpinner: Spinner) {
        val catNames = categories.map { it.name }.toTypedArray()
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, catNames)
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        catSpinner.adapter = catAdapter

        fun updateSubcategories(categoryId: String) {
            val subs = subcategories.filter { it.categoryId == categoryId }
            val subNames = subs.map { it.name }.toTypedArray()
            val subAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, subNames)
            subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            subSpinner.adapter = subAdapter
        }

        catSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position < categories.size) {
                    updateSubcategories(categories[position].id)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        if (categories.isNotEmpty()) {
            updateSubcategories(categories[0].id)
        }
    }

    private fun setupScopeListeners(rgScope: RadioGroup, catSpinner: Spinner, subSpinner: Spinner) {
        rgScope.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbGlobal -> {
                    catSpinner.isEnabled = false
                    subSpinner.isEnabled = false
                }
                R.id.rbRoot -> {
                    catSpinner.isEnabled = true
                    subSpinner.isEnabled = false
                }
                R.id.rbSub -> {
                    catSpinner.isEnabled = true
                    subSpinner.isEnabled = true
                }
            }
        }
        // Начальное состояние
        catSpinner.isEnabled = false
        subSpinner.isEnabled = false
    }

    private fun getScopeData(rgScope: RadioGroup, subSpinner: Spinner): Pair<Boolean, String?> {
        val checkedId = rgScope.checkedRadioButtonId
        return when (checkedId) {
            R.id.rbGlobal -> Pair(true, null)
            R.id.rbRoot -> Pair(false, null)
            R.id.rbSub -> {
                val subPos = subSpinner.selectedItemPosition
                val sub = if (subPos >= 0 && subPos < subcategories.size) subcategories[subPos] else null
                Pair(false, sub?.id)
            }
            else -> Pair(true, null)
        }
    }
}
