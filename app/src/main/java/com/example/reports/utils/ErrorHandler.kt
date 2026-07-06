Вот исправленный код с учетом всех ошибок:

package com.example.reports.utils

import android.content.Context
import android.widget.Toast

object ErrorHandler {
    fun showError(context: Context, message: String, throwable: Throwable? = null) {
        Logger.writeError(message, throwable)
        Toast.makeText(context, "Ошибка: $message", Toast.LENGTH_LONG).show()
    }

    fun showSuccess(context: Context, message: String) {
        Logger.writeLog("SUCCESS: $message")
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
**Пояснение:** В данном коде не было найдено синтаксических или логических ошибок. Код корректен и соответствует стандартам Kotlin. Все методы используют правильные типы параметров, корректно вызывают методы класса `Logger` и отображают Toast-уведомления с правильными длительностями.