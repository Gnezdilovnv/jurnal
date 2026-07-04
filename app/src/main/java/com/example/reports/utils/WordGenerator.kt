package com.example.reports.utils

import android.content.Context
import android.os.Environment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object WordGenerator {

    fun generateDocx(
        context: Context,
        fileName: String,
        content: String,
        folder: String = "Reports"
    ): File? {
        return try {
            // Создаем папку если нет
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val reportsDir = File(documentsDir, folder)
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            // Создаем файл
            val file = File(reportsDir, "$fileName.docx")

            // Создаем документ
            val document = XWPFDocument()

            // Добавляем заголовок (выравнивание по центру - 1)
            val titleParagraph = document.createParagraph()
            titleParagraph.setAlignment(1) // CENTER
            val titleRun = titleParagraph.createRun()
            titleRun.setText(fileName)
            titleRun.setBold(true)
            titleRun.setFontSize(18)

            // Добавляем дату (выравнивание по правому краю - 2)
            val dateParagraph = document.createParagraph()
            dateParagraph.setAlignment(2) // RIGHT
            val dateRun = dateParagraph.createRun()
            dateRun.setText("Создано: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
            dateRun.setFontSize(10)

            // Пустая строка
            document.createParagraph()

            // Добавляем содержимое
            val contentParagraph = document.createParagraph()
            val contentRun = contentParagraph.createRun()
            contentRun.setText(content)
            contentRun.setFontSize(12)

            // Сохраняем
            FileOutputStream(file).use { out ->
                document.write(out)
            }
            document.close()

            Logger.writeLog("Word файл создан: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Logger.writeError("Ошибка создания Word файла", e)
            null
        }
    }
}
