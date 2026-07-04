package com.example.reports.ui

import android.os.Bundle
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
    private lateinit var switchDarkMode: Switch
    private lateinit var btnUserMode: Button
    private lateinit var btnDevMode: Button
    private lateinit var userModeLayout: LinearLayout
    private lateinit var devModeLayout: LinearLayout

    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var settings = Settings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        Logger.writeLog("SettingsActivity started")

        initViews()
        setupSpinners()
        loadSettings()
        setupListeners()
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
        userModeLayout = findViewById(R.id.userModeLayout)
        devModeLayout = findViewById(R.id.devModeLayout)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveSettings() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportData() }
        findViewById<Button>(R.id.btnImport).setOnClickListener { importData() }
        findViewById<Button>(R.id.btnCategories).setOnClickListener {
            Toast.makeText(this, "Категории (в разработке)", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnVariables).setOnClickListener {
            Toast.makeText(this, "Переменные (в разработке)", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnTemplates).setOnClickListener {
            Toast.makeText(this, "Шаблоны (в разработке)", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnClearData).setOnClickListener { clearData() }
    }

    private fun setupSpinners() {
        // Место сохранения
        val folders = arrayOf("Downloads", "Documents", "Внешняя SD карта", "Своя папка")
        val folderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, folders)
        folderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSaveFolder.adapter = folderAdapter

        // Формат файла
        val formats = arrayOf("DOCX (Word)", "PDF", "TXT")
        val formatAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, formats)
        formatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFormat.adapter = formatAdapter
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
                Logger.writeLog("Settings loaded")
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

        // Режим
        if (settings.settingsMode == "dev") {
            setDevMode()
        } else {
            setUserMode()
        }

        // Spinner
        val folderIndex = when (settings.saveFolder) {
            "Documents" -> 1
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

        // Темная тема
        if (settings.darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
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

    private fun setUserMode() {
        settings.settingsMode = "user"
        userModeLayout.visibility = android.view.View.VISIBLE
        devModeLayout.visibility = android.view.View.GONE
        btnUserMode.setBackgroundColor(getColor(R.color.primary))
        btnUserMode.setTextColor(getColor(android.R.color.white))
        btnDevMode.setBackgroundColor(getColor(R.color.surface))
        btnDevMode.setTextColor(getColor(R.color.primary))
    }

    private fun setDevMode() {
        settings.settingsMode = "dev"
        userModeLayout.visibility = android.view.View.VISIBLE
        devModeLayout.visibility = android.view.View.VISIBLE
        btnDevMode.setBackgroundColor(getColor(R.color.primary))
        btnDevMode.setTextColor(getColor(android.R.color.white))
        btnUserMode.setBackgroundColor(getColor(R.color.surface))
        btnUserMode.setTextColor(getColor(R.color.primary))
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
                1 -> "Documents"
                2 -> "Внешняя SD карта"
                3 -> "Своя папка"
                else -> "Downloads"
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
                Logger.writeLog("Settings saved")
                Toast.makeText(this@SettingsActivity, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Logger.writeError("Save settings error", e)
                Toast.makeText(this@SettingsActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportData() {
        Toast.makeText(this, "Экспорт данных (в разработке)", Toast.LENGTH_SHORT).show()
    }

    private fun importData() {
        Toast.makeText(this, "Импорт данных (в разработке)", Toast.LENGTH_SHORT).show()
    }

    private fun clearData() {
        Toast.makeText(this, "Очистка данных (в разработке)", Toast.LENGTH_SHORT).show()
    }
}
