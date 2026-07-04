package com.example.reports.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val templateId: String,
    val title: String,
    val values: String = "",
    val filePath: String = "",
    val status: String = "draft",
    val createdAt: Long = System.currentTimeMillis()
)
