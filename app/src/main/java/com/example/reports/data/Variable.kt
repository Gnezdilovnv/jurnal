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