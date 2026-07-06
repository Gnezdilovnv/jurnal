package com.example.reports.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "subcategories")
data class Subcategory(
    @PrimaryKey
<<<<<<< HEAD
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
=======
    val id: String = UUID.randomUUID().toString(),  // ID генерируется автоматически
    val categoryId: String,  // ID родительской категории
>>>>>>> b903a30 (🤖 AI: исправление ошибок в коде)
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)