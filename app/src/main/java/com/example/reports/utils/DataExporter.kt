package com.example.reports.utils

import android.content.Context
import android.os.Environment
import com.example.reports.data.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object DataExporter {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    data class ExportData(
        val version: String = "1.0",
        val exportDate: String,
        val categories: List<Category>,
        val subcategories: List<Subcategory>,
        val variables: List<Variable>,
        val templates: List<Template>,
        val settings: Settings?
    )

    fun exportAllData(context: Context): File? {
        return try {
            val db = AppDatabase.getDatabase(context)
            val categories = db.categoryDao().getAll()
            val subcategories = db.subcategoryDao().getAll()
            val variables = db.variableDao().getAll()
            val templates = db.templateDao().getAll()
            val settings = db.settingsDao().get()

            val exportData = ExportData(
                exportDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                categories = categories,
                subcategories = subcategories,
                variables = variables,
                templates = templates,
                settings = settings
            )

            val json = gson.toJson(exportData)

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val reportsDir = File(downloadsDir, "Reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            val file = File(reportsDir, "backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json")
            FileWriter(file).use { writer ->
                writer.write(json)
            }

            Logger.writeLog("Данные экспортированы: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Logger.writeError("Ошибка экспорта данных", e)
            null
        }
    }

    fun importAllData(context: Context, file: File): Boolean {
        return try {
            val json = FileReader(file).readText()
            val type = object : TypeToken<ExportData>() {}.type
            val exportData: ExportData = gson.fromJson(json, type)

            val db = AppDatabase.getDatabase(context)

            // Очищаем БД перед импортом
            db.categoryDao().getAll().forEach { db.categoryDao().delete(it) }
            db.subcategoryDao().getAll().forEach { db.subcategoryDao().delete(it) }
            db.variableDao().getAll().forEach { db.variableDao().delete(it) }
            db.templateDao().getAll().forEach { db.templateDao().delete(it) }

            // Импортируем данные
            exportData.categories.forEach { db.categoryDao().insert(it) }
            exportData.subcategories.forEach { db.subcategoryDao().insert(it) }
            exportData.variables.forEach { db.variableDao().insert(it) }
            exportData.templates.forEach { db.templateDao().insert(it) }
            exportData.settings?.let { db.settingsDao().insert(it) }

            Logger.writeLog("Данные импортированы из: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Logger.writeError("Ошибка импорта данных", e)
            false
        }
    }

    fun getBackupFiles(): List<File> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val reportsDir = File(downloadsDir, "Reports")
        return if (reportsDir.exists()) {
            reportsDir.listFiles { _, name -> name.startsWith("backup_") && name.endsWith(".json") }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
}
