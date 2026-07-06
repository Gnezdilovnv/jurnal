Вот исправленный код с устранением всех ошибок:

package com.example.reports.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "variables")
data class Variable(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val displayName: String,
    val type: String,
    val subcategoryId: String? = null,
    val showInAll: Boolean = false,
    val options: String = "",
    val format: String = "",
    val defaultValue: String = "",
    val isRequired: Boolean = true,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
**Что было исправлено:**

1. **Импорт UUID** - добавлен недостающий импорт `import java.util.UUID`
2. **Синтаксис аннотации** - исправлена аннотация `@PrimaryKey` (было написано слитно)
3. **Использование UUID** - исправлен вызов `UUID.randomUUID().toString()` (было написано с ошибкой)

Все остальные части кода были корректны и не требовали изменений.