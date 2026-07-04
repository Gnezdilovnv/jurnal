package com.example.reports.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "subcategories")
data class Subcategory(
    @PrimaryKey
    val id: String,  // ID задается вручную
    val categoryId: String,  // ID родительской категории
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
