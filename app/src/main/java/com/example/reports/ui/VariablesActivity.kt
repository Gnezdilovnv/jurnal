package com.example.reports.ui

import android.app.AlertDialog
import android.os.Bundle
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
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_variables)

        Logger.writeLog("VariablesActivity started")

        recyclerView = findViewById(R.id.recyclerVariables)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAddVariable).setOnClickListener { showAddDialog() }

        loadVariables()
    }

    private fun loadVariables() {
        scope.launch {
            try {
                variables = withContext(Dispatchers.IO) {
                    db.variableDao().getAll()
                }
                val items = variables.map { "${it.name} (${it.type}) - ${it.displayName}" }
                adapter = ArrayAdapter(this@VariablesActivity, android.R.layout.simple_list_item_1, items)
                recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                        object : RecyclerView.ViewHolder(
                            LayoutInflater.from(parent.context)
                                .inflate(android.R.layout.simple_list_item_1, parent, false)
                        ) {}

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                        val tv = holder.itemView as TextView
                        tv.text = items[position]
                    }

                    override fun getItemCount() = items.size
                }
                Logger.writeLog("Loaded ${variables.size} variables")
            } catch (e: Exception) {
                Logger.writeError("Load variables error", e)
            }
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_variable, null)
        val etName = dialogView.findViewById<EditText>(R.id.etVarName)
        val etDisplayName = dialogView.findViewById<EditText>(R.id.etVarDisplayName)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerVarType)
        val chkShowInAll = dialogView.findViewById<CheckBox>(R.id.chkShowInAll)
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
        scope.launch {
            val cats = withContext(Dispatchers.IO) {
                db.categoryDao().getAll()
            }
            val catNames = cats.map { it.name }.toTypedArray()
            val catAdapter = ArrayAdapter(this@VariablesActivity, android.R.layout.simple_spinner_item, catNames)
            catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = catAdapter
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val displayName = etDisplayName.text.toString().trim()
            val type = spinnerType.selectedItem.toString()
            val showInAll = chkShowInAll.isChecked
            val required = chkRequired.isChecked

            if (name.isEmpty() || displayName.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        db.variableDao().insert(Variable(
                            name = name,
                            displayName = displayName,
                            type = type,
                            showInAll = showInAll,
                            isRequired = required
                        ))
                    }
                    loadVariables()
                    Toast.makeText(this@VariablesActivity, "Переменная создана", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Logger.writeError("Create variable error", e)
                    Toast.makeText(this@VariablesActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }
}
