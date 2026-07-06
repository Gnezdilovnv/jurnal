Вот исправленный код файла `SettingsActivity.kt`:

package com.example.reports.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Settings
import com.example.reports.utils.DataExporter
import com.example.reports.utils.ErrorHandler
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
    private lateinit var btnExport: Button
    private lateinit var btnImport: Button

    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var settings = Settings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        initViews()
        setupSpinners()
        loadSettings()
        setupListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
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
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveSettings() }
        findViewById<Button>(R.id.btnCategories).setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java))
        }
        findViewById<Button>(R.id.btnVariables).setOnClickListener {
            startActivity(Intent(this, VariablesActivity::class.java))
        }
        findViewById<Button>(R.id.btnTemplates).setOnClickListener {
            startActivity(Intent(this, TemplatesActivity::class.java))
        }
        
        btnExport.setOnClickListener { exportData() }
        btnImport.setOnClickListener { importData() }
        findViewById<Button>(R.id.btnClearData).setOnClickListener { clearData() }
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

    private fun loadSettings() {
        scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { db.settingsDao().get() }
                if (loaded != null) {
                    settings = loaded
                    applySettings()
                }
            } catch (e: Exception) {
                ErrorHandler.showError(this@SettingsActivity, "Загрузка настроек", e)
            }
        }
    }

    private fun applySettings() {
        etEmailTo.setText(settings.emailTo)
        etEmailSubject.setText(settings.emailSubject)
        etEmailBody.setText(settings.emailBody)
        etFileName.setText(settings.fileNameTemplate)
        switchDarkMode.isChecked = settings.darkMode
        
        // Устанавливаем позиции спиннеров
        val folderPosition = when (settings.saveFolder) {
            "Документы (Documents)" -> 1
            "Внешняя SD карта" -> 2
            "Своя папка" -> 3
            else -> 0
        }
        spinnerSaveFolder.setSelection(folderPosition)
        
        val formatPosition = when {
            settings.formatPdf -> 1
            settings.formatTxt -> 2
            else -> 0
        }
        spinnerFormat.setSelection(formatPosition)
        
        if (settings.settingsMode == "dev") setDevMode() else setUserMode()
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
        userModeLayout.visibility = android.view.View.VISIBLE
        devModeLayout.visibility = android.view.View.GONE
        btnUserMode.setBackgroundColor(getColor(R.color.primary))
        btnUserMode.setTextColor(getColor(android.R.color.white))
        btnDevMode.setBackgroundColor(getColor(R.color.surface))
        btnDevMode.setTextColor(getColor(R.color.primary))
        settings = settings.copy(settingsMode = "user")
    }

    private fun setDevMode() {
        userModeLayout.visibility = android.view.View.VISIBLE
        devModeLayout.visibility = android.view.View.VISIBLE
        btnDevMode.setBackgroundColor(getColor(R.color.primary))
        btnDevMode.setTextColor(getColor(android.R.color.white))
        btnUserMode.setBackgroundColor(getColor(R.color.surface))
        btnUserMode.setTextColor(getColor(R.color.primary))
        settings = settings.copy(settingsMode = "dev")
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
                withContext(Dispatchers.IO) { db.settingsDao().insert(settings) }
                ErrorHandler.showSuccess(this@SettingsActivity, "Настройки сохранены")
            } catch (e: Exception) {
                ErrorHandler.showError(this@SettingsActivity, "Сохранение настроек", e)
            }
        }
    }

    private fun exportData() {
        scope.launch {
            try {
                val file = DataExporter.exportAllData(this@SettingsActivity)
                if (file != null) {
                    ErrorHandler.showSuccess(this@SettingsActivity, "Данные экспортированы: ${file.name}")
                    
                    try {
                        val uri = FileProvider.getUriForFile(
                            this@SettingsActivity,
                            "${packageName}.fileprovider",
                            file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "Бэкап данных")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Отправить бэкап через..."))
                    } catch (e: Exception) {
                        // Если не получилось отправить, просто показываем сообщение
                        Logger.e("Share failed", e)
                    }
                } else {
                    ErrorHandler.showError(this@SettingsActivity, "Ошибка экспорта данных")
                }
            } catch (e: Exception) {
                ErrorHandler.showError(this@SettingsActivity, "Экспорт данных", e)
            }
        }
    }

    private fun importData() {
        val backupFiles = DataExporter.getBackupFiles()
        if (backupFiles.isEmpty()) {
            Toast.makeText(this, "Нет файлов бэкапа в папке Reports", Toast.LENGTH_LONG).show()
            return
        }

        val fileNames = backupFiles.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Выберите файл для импорта")
            .setItems(fileNames) { _, which ->
                val file = backupFiles[which]
                AlertDialog.Builder(this)
                    .setTitle("Импорт данных")
                    .setMessage("Будут импортированы все данные из файла: ${file.name}\nТекущие данные будут удалены.")
                    .setPositiveButton("Импортировать") { _, _ ->
                        scope.launch {
                            try {
                                val success = DataExporter.importAllData(this@SettingsActivity, file)
                                if (success) {
                                    ErrorHandler.showSuccess(this@SettingsActivity, "Данные импортированы")
                                    loadSettings()
                                } else {
                                    ErrorHandler.showError(this@SettingsActivity, "Ошибка импорта данных")
                                }
                            } catch (e: Exception) {
                                ErrorHandler.showError(this@SettingsActivity, "Импорт данных", e)
                            }
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun clearData() {
        AlertDialog.Builder(this)
            .setTitle("Очистить все данные?")
            .setMessage("Все категории, подкатегории, переменные, шаблоны и отчеты будут удалены.\nЭто действие невозможно отменить.")
            .setPositiveButton("Очистить") { _, _ ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            db.categoryDao().getAll().forEach { db.categoryDao().delete(it) }
                            db.subcategoryDao().getAll().forEach { db.subcategoryDao().delete(it) }
                            db.variableDao().getAll().forEach { db.variableDao().delete(it) }
                            db.templateDao().getAll().forEach { db.templateDao().delete(it) }
                            db.reportDao().getAll().forEach { db.reportDao().delete(it) }
                        }
                        ErrorHandler.showSuccess(this@SettingsActivity, "Все данные очищены")
                    } catch (e: Exception) {
                        ErrorHandler.showError(this@SettingsActivity, "Очистка данных", e)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
**Основные исправления:**

1. **Импорт Switch**: Заменил `android.widget.Switch` на `Switch` (импорт уже есть)
2. **Добавил SupervisorJob**: Для корректной отмены корутин при уничтожении Activity
3. **Добавил onDestroy**: Для отмены scope корутин
4. **Добавил установку позиций спиннеров**: В методе `applySettings()` теперь устанавливаются позиции для `spinnerSaveFolder` и `spinnerFormat`
5. **Добавил сохранение режима**: В методах `setUserMode()` и `setDevMode()` теперь сохраняется режим в объекте `settings`
6. **Добавил логирование ошибки**: В блоке catch при неудачной отправке файла
7. **Убрал неиспользуемый импорт**: `Logger` используется, но был импортирован