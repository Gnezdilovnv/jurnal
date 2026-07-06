package com.example.reports.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "variables")
data class Variable(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,                    // Внутреннее имя (например "temperature")
    val displayName: String,             // Отображаемое имя (например "Температура")
    val type: String = "TEXT",           // TEXT, NUMBER, DATE, TIME, BOOLEAN, SELECT, LOCATION
    val showInAll: Boolean = false,      // Глобальная переменная (показывать во всех шаблонах)
    val subcategoryId: String? = null,   // Привязка к подкатегории
    val isRequired: Boolean = false,     // Обязательное поле
    val options: String = "",            // Для SELECT: "Вариант1,Вариант2,Вариант3"
    val order: Int = 0,                  // Порядок отображения
    val createdAt: Long = System.currentTimeMillis()
)
