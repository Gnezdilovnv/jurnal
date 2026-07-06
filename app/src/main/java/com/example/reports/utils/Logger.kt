Вот исправленный код с устранением всех ошибок:

package com.example.reports.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private var logFile: File? = null

    fun init(context: Context) {
        try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dir != null && (dir.exists() || dir.mkdirs())) {
                logFile = File(dir, "reports_app_log.txt")
                logFile?.delete()
                logFile?.createNewFile()
                writeLog("=== APP STARTED ===")
            }
        } catch (_: Exception) {
            // Игнорируем исключения при инициализации
        }
    }

    fun writeLog(msg: String) {
        try {
            logFile?.appendText("[${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())}] $msg\n")
        } catch (_: Exception) {
            // Игнорируем исключения при записи лога
        }
    }

    fun writeError(msg: String, e: Throwable? = null) {
        val errorMsg = if (e != null) "$msg: ${e.message}" else msg
        writeLog("ERROR: $errorMsg")
    }
}
**Основные исправления:**

1. **Синтаксическая ошибка в `writeLog`**: В оригинале была лишняя закрывающая скобка `)` после `Date()` и неправильный порядок скобок. Исправлено на правильный вызов `SimpleDateFormat(...).format(Date())`.

2. **Синтаксическая ошибка в `writeError`**: Была лишняя закрывающая скобка `)` после `e.message`. Исправлено на правильное выражение `"$msg: ${e.message}"`.

3. **Добавлены комментарии к пустым catch-блокам**: Хотя это не ошибка компиляции, добавление комментариев улучшает читаемость кода и показывает намерение разработчика.

4. **Форматирование**: Код отформатирован для лучшей читаемости, хотя это не влияет на функциональность.

Все остальные части кода остались без изменений, так как они были корректны.