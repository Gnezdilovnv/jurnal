package com.example.reports.utils

import android.content.Context
import android.os.Environment
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
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

            // Добавляем заголовок
            val titleParagraph = document.createParagraph()
            titleParagraph.alignment = XWPFParagraph.Alignment.CENTER
            val titleRun = titleParagraph.createRun()
            titleRun.setText(fileName)
            titleRun.isBold = true
            titleRun.fontSize = 18

            // Добавляем дату
            val dateParagraph = document.createParagraph()
            dateParagraph.alignment = XWPFParagraph.Alignment.RIGHT
            val dateRun = dateParagraph.createRun()
            dateRun.setText("Создано: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}")
            dateRun.fontSize = 10

            // Пустая строка
            document.createParagraph()

            // Добавляем содержимое
            val contentParagraph = document.createParagraph()
            val contentRun = contentParagraph.createRun()
            contentRun.setText(content)
            contentRun.fontSize = 12

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
