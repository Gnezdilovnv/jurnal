Вот исправленный код файла `Settings.kt`:

package com.example.reports.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey
    val id: String = "settings",
    val emailTo: String = "",
    val emailSubject: String = "Отчет от {date} - {root_category}",
    val emailBody: String = "",
    val saveFolder: String = "Downloads",
    val fileNameTemplate: String = "Отчет_{date}_{group}",
    val formatDocx: Boolean = true,
    val formatPdf: Boolean = false,
    val formatTxt: Boolean = false,
    val darkMode: Boolean = false,
    val settingsMode: String = "user",
    val updatedAt: Long = System.currentTimeMillis()
)
**Исправления:**
1. Удалена пустая строка между `@PrimaryKey` и `val id`
2. Удалена пустая строка между `@Entity(tableName = "settings")` и `data class Settings`

Код теперь корректен и не содержит синтаксических ошибок.