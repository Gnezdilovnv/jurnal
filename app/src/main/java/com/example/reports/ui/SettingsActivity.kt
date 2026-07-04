package com.example.reports.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Settings
import com.example.reports.utils.Logger
import kotlinx.coroutines.*

class SettingsActivity : AppCompatActivity() {
    private lateinit var etEmailTo: EditText
    private lateinit var etEmailSubject: EditText
    private lateinit var etEmailBody: EditText
    private lateinit var etFileName: EditText
    private lateinit var spinnerSaveFolder: Spinner
    private lateinit var spinnerFormat: Spinner
    private lateinit var switchDarkMode: androidx.appcompat.widget.SwitchCompat
    private lateinit var btnUserMode: Button
    private lateinit var btnDevMode: Button
    private lateinit var tvModeHint: TextView
    private lateinit var userModeLayout: LinearLayout
    private lateinit var devModeLayout: LinearLayout

    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var settings = Settings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        setupSpinners()
        setupListeners()
        loadSettings()
    }

    private fun initViews() {
        etEmailTo = findViewById(R.id.etEmailTo)
        etEmailSubject = findViewById(R.id.etEmailSubject)
        etEmailBody = findViewById(R.id.etEmailBody)
        etFileName = findViewById(R.id.etFileName)
        spinnerSaveFolder = findViewById(R.id.spinnerSaveFolder)
        spinnerFormat = findViewById(R.id.spinnerFormat)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        btnUserMode = findViewById(R.id.btnUserMode)
        btnDevMode = findViewById(R.id.btnDevMode)
        tvModeHint = findViewById(R.id.tvModeHint)
        userModeLayout = findViewById(R.id.userModeLayout)
        devModeLayout = findViewById(R.id.devModeLayout)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnSave).setOnClickListener { saveSettings() }

        // Переходы
        findViewById<View>(R.id.btnCategories).setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java))
        }
        findViewById<View>(R.id.btnVariables).setOnClickListener {
            startActivity(Intent(this, VariablesActivity::class.java))
        }
        findViewById<View>(R.id.btnTemplates).setOnClickListener {
            startActivity(Intent(this, TemplatesActivity::class.java))
        }
        findViewById<View>(R.id.btnExport).setOnClickListener {
            Toast.makeText(this, "Экспорт данных", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnImport).setOnClickListener {
            Toast.makeText(this, "Импорт данных", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnClearData).setOnClickListener {
            Toast.makeText(this, "Очистка данных", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinners() {
        val folders = arrayOf("Загрузки (Downloads)", "Документы (Documents)", "Внешняя SD карта", "Своя папка")
        val folderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, folders)
        folderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSaveFolder.adapter = folderAdapter

        val formats = arrayOf("DOCX (Word)", "PDF", "TXT")
        val formatAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, formats)
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFormat.adapter = formatAdapter
    }

    private fun setupListeners() {
        btnUserMode.setOnClickListener { setUserMode() }
        btnDevMode.setOnClickListener { setDevMode() }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun loadSettings() {
        scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    db.settingsDao().get()
                }
                if (loaded != null) {
                    settings = loaded
                    applySettings()
                }
            } catch (e: Exception) {
                Logger.writeError("Load settings error", e)
            }
        }
    }

    private fun applySettings() {
        etEmailTo.setText(settings.emailTo)
        etEmailSubject.setText(settings.emailSubject)
        etEmailBody.setText(settings.emailBody)
        etFileName.setText(settings.fileNameTemplate)
        switchDarkMode.isChecked = settings.darkMode

        if (settings.settingsMode == "dev") {
            setDevMode()
        } else {
            setUserMode()
        }

        val folderIndex = when (settings.saveFolder) {
            "Документы (Documents)" -> 1
            "Внешняя SD карта" -> 2
            "Своя папка" -> 3
            else -> 0
        }
        spinnerSaveFolder.setSelection(folderIndex)

        val formatIndex = when {
            settings.formatPdf -> 1
            settings.formatTxt -> 2
            else -> 0
        }
        spinnerFormat.setSelection(formatIndex)
    }

    private fun setUserMode() {
        settings = settings.copy(settingsMode = "user")
        userModeLayout.visibility = View.VISIBLE
        devModeLayout.visibility = View.GONE
        btnUserMode.background = getDrawable(R.drawable.mode_button_active)
        btnUserMode.setTextColor(getColor(android.R.color.black))
        btnDevMode.background = getDrawable(R.drawable.mode_button)
        btnDevMode.setTextColor(getColor(R.color.text_secondary))
        tvModeHint.text = "🔴 Режим пользователя — только основные настройки."
    }

    private fun setDevMode() {
        settings = settings.copy(settingsMode = "dev")
        userModeLayout.visibility = View.VISIBLE
        devModeLayout.visibility = View.VISIBLE
        btnDevMode.background = getDrawable(R.drawable.mode_button_active)
        btnDevMode.setTextColor(getColor(android.R.color.black))
        btnUserMode.background = getDrawable(R.drawable.mode_button)
        btnUserMode.setTextColor(getColor(R.color.text_secondary))
        tvModeHint.text = "🛠️ Режим разработчика — все настройки приложения."
    }

    private fun saveSettings() {
        val emailTo = etEmailTo.text.toString().trim()
        val emailSubject = etEmailSubject.text.toString().trim()
        val emailBody = etEmailBody.text.toString().trim()
        val fileName = etFileName.text.toString().trim()

        settings = settings.copy(
            emailTo = emailTo,
            emailSubject = emailSubject.ifEmpty { "Отчет от {date}" },
            emailBody = emailBody,
            fileNameTemplate = fileName.ifEmpty { "Отчет_{date}_{group}" },
            saveFolder = when (spinnerSaveFolder.selectedItemPosition) {
                1 -> "Документы (Documents)"
                2 -> "Внешняя SD карта"
                3 -> "Своя папка"
                else -> "Загрузки (Downloads)"
            },
            formatDocx = spinnerFormat.selectedItemPosition == 0,
            formatPdf = spinnerFormat.selectedItemPosition == 1,
            formatTxt = spinnerFormat.selectedItemPosition == 2,
            darkMode = switchDarkMode.isChecked,
            updatedAt = System.currentTimeMillis()
        )

        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.settingsDao().insert(settings)
                }
                Toast.makeText(this@SettingsActivity, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
