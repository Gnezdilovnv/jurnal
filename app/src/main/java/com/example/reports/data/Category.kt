package com.example.reports.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String,  // ID задается вручную при создании
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
