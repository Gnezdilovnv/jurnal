package com.example.reports.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: String = "settings",
    val emailTo: String = "",
    val emailSubject: String = "Отчет от {date}",
    val emailBody: String = "",
    val saveFolder: String = "Downloads",
    val fileNameTemplate: String = "Отчет_{date}",
    val formatDocx: Boolean = true,
    val formatPdf: Boolean = false,
    val formatTxt: Boolean = false,
    val darkMode: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
