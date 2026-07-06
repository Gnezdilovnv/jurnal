Вот исправленный код с устранением всех ошибок:

package com.example.reports.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Report
import com.example.reports.data.Template
import com.example.reports.data.Variable
import com.example.reports.utils.ErrorHandler
import com.example.reports.utils.Logger
import com.example.reports.utils.WordGenerator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CreateReportActivity : AppCompatActivity() {
    private lateinit var spinnerTemplate: Spinner
    private lateinit var containerFields: LinearLayout
    private lateinit var tvPreview: TextView
    private lateinit var btnGenerate: Button
    private lateinit var btnSave: Button
    private lateinit var btnSend: Button
    private lateinit var btnWord: Button

    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var templates = listOf<Template>()
    private var allVariables = listOf<Variable>()
    private var selectedTemplate: Template? = null
    private val fieldValues = mutableMapOf<String, String>()
    private var lastLocation: Location? = null
    private var generatedText: String = ""
    private var currentReportId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_report)

        initViews()
        loadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun initViews() {
        spinnerTemplate = findViewById(R.id.spinnerTemplate)
        containerFields = findViewById(R.id.containerFields)
        tvPreview = findViewById(R.id.tvPreview)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnSave = findViewById(R.id.btnSave)
        btnSend = findViewById(R.id.btnSend)
        btnWord = findViewById(R.id.btnWord)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        btnGenerate.setOnClickListener { generateReport() }
        btnSave.setOnClickListener { saveReport() }
        btnSend.setOnClickListener { shareReport() }
        btnWord.setOnClickListener { generateWordFile() }
    }

    private fun loadData() {
        scope.launch {
            try {
                templates = withContext(Dispatchers.IO) { db.templateDao().getAll() }
                allVariables = withContext(Dispatchers.IO) { db.variableDao().getAll() }

                if (templates.isEmpty()) {
                    Toast.makeText(this@CreateReportActivity, "Создайте шаблон", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val adapter = ArrayAdapter(
                    this@CreateReportActivity,
                    android.R.layout.simple_spinner_item,
                    templates.map { it.name }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerTemplate.adapter = adapter

                spinnerTemplate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        selectedTemplate = templates[position]
                        buildFields()
                        generateReport()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            } catch (e: Exception) {
                ErrorHandler.showError(this@CreateReportActivity, "Загрузка данных", e)
            }
        }
    }

    private fun buildFields() {
        containerFields.removeAllViews()
        fieldValues.clear()

        val template = selectedTemplate ?: return
        val variableNames = extractVariables(template.text)

        variableNames.forEach { varName ->
            val variable = allVariables.find { it.name == varName }
            if (variable != null) {
                addFieldView(variable)
            } else {
                addUnknownField(varName)
            }
        }
    }

    private fun extractVariables(text: String): List<String> {
        val regex = "\\{([^}]+)\\}".toRegex()
        return regex.findAll(text).map { it.groupValues[1] }.distinct().toList()
    }

    private fun addFieldView(variable: Variable) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }

        val label = TextView(this).apply {
            text = if (variable.isRequired) "${variable.displayName} *" else variable.displayName
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        container.addView(label)

        when (variable.type) {
            "TEXT" -> addTextField(container, variable)
            "NUMBER" -> addNumberField(container, variable)
            "DATE" -> addDateField(container, variable)
            "TIME" -> addTimeField(container, variable)
            "BOOLEAN" -> addBooleanField(container, variable)
            "SELECT" -> addSelectField(container, variable)
            "LOCATION" -> addLocationField(container, variable)
            else -> addTextField(container, variable)
        }

        containerFields.addView(container)
    }

    private fun addUnknownField(varName: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        val label = TextView(this).apply {
            text = "⚠️ $varName (неизвестная переменная)"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@CreateReportActivity, R.color.red))
        }
        container.addView(label)
        containerFields.addView(container)
    }

    private fun addTextField(container: LinearLayout, variable: Variable) {
        val input = EditText(this).apply {
            hint = "Введите текст"
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    fieldValues[variable.name] = s.toString()
                    generateReport()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        container.addView(input)
        fieldValues[variable.name] = ""
    }

    private fun addNumberField(container: LinearLayout, variable: Variable) {
        val input = EditText(this).apply {
            hint = "Введите число"
            inputType = InputType.TYPE_CLASS_NUMBER
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    fieldValues[variable.name] = s.toString()
                    generateReport()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        container.addView(input)
        fieldValues[variable.name] = ""
    }

    private fun addDateField(container: LinearLayout, variable: Variable) {
        val button = Button(this).apply {
            text = "Выбрать дату"
            setOnClickListener {
                val calendar = Calendar.getInstance()
                DatePickerDialog(
                    this@CreateReportActivity,
                    { _, year, month, day ->
                        val date = "$day.${month + 1}.$year"
                        fieldValues[variable.name] = date
                        text = date
                        generateReport()
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
        container.addView(button)
        fieldValues[variable.name] = ""
    }

    private fun addTimeField(container: LinearLayout, variable: Variable) {
        val button = Button(this).apply {
            text = "Выбрать время"
            setOnClickListener {
                val calendar = Calendar.getInstance()
                TimePickerDialog(
                    this@CreateReportActivity,
                    { _, hour, minute ->
                        val time = String.format("%02d:%02d", hour, minute)
                        fieldValues[variable.name] = time
                        text = time
                        generateReport()
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            }
        }
        container.addView(button)
        fieldValues[variable.name] = ""
    }

    private fun addBooleanField(container: LinearLayout, variable: Variable) {
        val switch = SwitchMaterial(this).apply {
            text = "Нет"
            setOnCheckedChangeListener { _, isChecked ->
                val value = if (isChecked) "Да" else "Нет"
                fieldValues[variable.name] = value
                text = value
                generateReport()
            }
        }
        container.addView(switch)
        fieldValues[variable.name] = "Нет"
    }

    private fun addSelectField(container: LinearLayout, variable: Variable) {
        val options = variable.options.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val finalOptions = if (options.isEmpty()) listOf("Нет вариантов") else options
        
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@CreateReportActivity,
                android.R.layout.simple_spinner_item,
                finalOptions
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    fieldValues[variable.name] = finalOptions[position]
                    generateReport()
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
        container.addView(spinner)
        fieldValues[variable.name] = finalOptions.firstOrNull() ?: ""
    }

    private fun addLocationField(container: LinearLayout, variable: Variable) {
        val button = Button(this).apply {
            text = "Получить координаты"
            setOnClickListener {
                if (checkLocationPermission()) {
                    getLocation { location ->
                        val coords = "${location.latitude}, ${location.longitude}"
                        fieldValues[variable.name] = coords
                        text = coords
                        generateReport()
                    }
                }
            }
        }
        container.addView(button)
        fieldValues[variable.name] = ""
    }

    private fun generateReport() {
        val template = selectedTemplate ?: return
        var text = template.text
        fieldValues.forEach { (key, value) ->
            text = text.replace("{$key}", value.ifEmpty { "..." })
        }
        generatedText = text
        tvPreview.text = generatedText
    }

    private fun saveReport() {
        val template = selectedTemplate ?: return
        val title = "Отчет от ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}"

        scope.launch {
            try {
                val report = Report(
                    templateId = template.id,
                    title = title,
                    values = fieldValues.entries.joinToString(";") { "${it.key}:${it.value}" },
                    status = "draft"
                )
                withContext(Dispatchers.IO) {
                    currentReportId = db.reportDao().insert(report).toString()
                }
                ErrorHandler.showSuccess(this@CreateReportActivity, "Отчет сохранен")
            } catch (e: Exception) {
                ErrorHandler.showError(this@CreateReportActivity, "Сохранение отчета", e)
            }
        }
    }

    private fun generateWordFile() {
        if (generatedText.isEmpty()) {
            Toast.makeText(this, "Сначала заполните отчет", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "Отчет_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
        val file = WordGenerator.generateDocx(
            context = this,
            fileName = fileName,
            content = generatedText,
            folder = "Reports"
        )

        if (file != null) {
            ErrorHandler.showSuccess(this, "Word файл создан: ${file.name}")
        } else {
            ErrorHandler.showError(this, "Не удалось создать Word файл")
        }
    }

    private fun shareReport() {
        if (generatedText.isEmpty()) {
            Toast.makeText(this, "Сначала заполните отчет", Toast.LENGTH_SHORT).show()
            return
        }

        // Сначала создаем Word файл
        val fileName = "Отчет_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
        val file = WordGenerator.generateDocx(
            context = this,
            fileName = fileName,
            content = generatedText,
            folder = "Reports"
        )

        if (file == null) {
            ErrorHandler.showError(this, "Не удалось создать файл для отправки")
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, "Отчет сгенерирован в приложении")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Отправить отчет через..."))
            Logger.writeLog("Отчет отправлен: $fileName")
        } catch (e: Exception) {
            ErrorHandler.showError(this, "Ошибка отправки", e)
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return false
        }
        return true
    }

    private fun getLocation(callback: (Location) -> Unit) {
        if (!checkLocationPermission()) return
        val client = LocationServices.getFusedLocationProviderClient(this)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    lastLocation = location
                    callback(location)
                } else {
                    Toast.makeText(this, "Не удалось получить координаты", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка GPS: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
**Основные исправления:**

1. **Добавлен импорт `Editable`** - для использования в TextWatcher
2. **Добавлен импорт `InputType`** - для использования в addNumberField
3. **Добавлен импорт `TextWatcher`** - для использования в текстовых полях
4. **Добавлен `SupervisorJob()`** в CoroutineScope для правильной обработки ошибок
5. **Добавлен `onDestroy()`** с отменой корутин для предотвращения утечек памяти
6. **Исправлен `getColor()`** на `ContextCompat.getColor()` для совместимости
7. **Исправлены типы параметров** в TextWatcher (использование `Editable?` вместо `android.text.Editable?`)

Все остальные функциональные части кода остались без изменений, так как они были корректны.