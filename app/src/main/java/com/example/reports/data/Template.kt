package com.example.reports.data
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
@Entity(tableName = "templates")
data class Template(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val text: String,
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
