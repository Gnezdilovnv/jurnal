Вот исправленный код файла `VariablesActivity.kt`:

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
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
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

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun loadData() {
        scope.launch {
            try {
                variables = withContext(Dispatchers.IO) { db.variableDao().getAll() }
                categories = withContext(Dispatchers.IO) { db.categoryDao().getAll() }
                subcategories = withContext(Dispatchers.IO) { db.subcategoryDao().getAll() }
                updateAdapter()
            } catch (e: Exception) {
                Logger.e("VariablesActivity", "Ошибка загрузки данных", e)
                Toast.makeText(this@VariablesActivity, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
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
                
                holder.itemView.setOnClickListener {
                    showEditDialog(variables[position])
                }
                
                holder.itemView.setOnLongClickListener {
                    showDeleteDialog(variables[position])
                    true
                }
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
                    Logger.e("VariablesActivity", "Ошибка создания переменной", e)
                    Toast.makeText(this@VariablesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

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

        etName.setText(variable.name)
        etDisplayName.setText(variable.displayName)
        chkRequired.isChecked = variable.isRequired

        setupTypeSpinner(spinnerType)
        val typeIndex = listOf("TEXT", "NUMBER", "DATE", "TIME", "BOOLEAN", "SELECT", "LOCATION").indexOf(variable.type)
        if (typeIndex >= 0) spinnerType.setSelection(typeIndex)

        when {
            variable.showInAll -> rgScope.check(R.id.rbGlobal)
            variable.subcategoryId != null -> rgScope.check(R.id.rbSub)
            else -> rgScope.check(R.id.rbRoot)
        }

        setupCategorySpinners(spinnerCategory, spinnerSubcategory)
        setupScopeListeners(rgScope, spinnerCategory, spinnerSubcategory)

        if (variable.subcategoryId != null) {
            val sub = subcategories.find { it.id == variable.subcategoryId }
            if (sub != null) {
                val catIndex = categories.indexOfFirst { it.id == sub.categoryId }
                if (catIndex >= 0) spinnerCategory.setSelection(catIndex)
                val subList = subcategories.filter { it.categoryId == sub.categoryId }
                val subIndex = subList.indexOfFirst { it.id == sub.id }
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
                    Logger.e("VariablesActivity", "Ошибка обновления переменной", e)
                    Toast.makeText(this@VariablesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun showDeleteDialog(variable: Variable) {
        AlertDialog.Builder(this)
            .setTitle("Удалить переменную?")
            .setMessage("${variable.name} (${variable.displayName})")
            .setPositiveButton("Удалить") { _, _ ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { db.variableDao().delete(variable) }
                        loadData()
                        Toast.makeText(this@VariablesActivity, "Переменная удалена", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Logger.e("VariablesActivity", "Ошибка удаления переменной", e)
                        Toast.makeText(this@VariablesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

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
<<<<<<< HEAD
}
**Основные исправления:**

1. **Добавлен `SupervisorJob()`** в `CoroutineScope` для правильной обработки ошибок в корутинах
2. **Добавлен `onDestroy()`** с вызовом `scope.cancel()` для предотвращения утечек памяти
3. **Добавлено логирование ошибок** через `Logger.e()` для лучшей отладки
4. **Улучшены сообщения об ошибках** - теперь показывается текст ошибки
5. **Исправлена обработка ошибок** - все исключения логируются перед показом Toast
=======
}
>>>>>>> b903a30 (🤖 AI: исправление ошибок в коде)
