package com.example.reports.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.R
import com.example.reports.data.AppDatabase
import com.example.reports.data.Report
import com.example.reports.utils.ErrorHandler
import com.example.reports.utils.Logger
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class ReportsListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private val db by lazy { AppDatabase.getDatabase(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var reports = listOf<Report>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports_list)
        recyclerView = findViewById(R.id.recyclerReports)
        recyclerView.layoutManager = LinearLayoutManager(this)
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        loadReports()
    }

    private fun loadReports() {
        scope.launch {
            try {
                reports = withContext(Dispatchers.IO) { db.reportDao().getAll() }
                if (reports.isEmpty()) {
                    Toast.makeText(this@ReportsListActivity, "Нет сохраненных отчетов", Toast.LENGTH_LONG).show()
                }
                updateAdapter()
            } catch (e: Exception) {
                ErrorHandler.showError(this@ReportsListActivity, "Загрузка отчетов", e)
            }
        }
    }

    private fun updateAdapter() {
        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_2, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val report = reports[position]
                val tvTitle = holder.itemView.findViewById<TextView>(android.R.id.text1)
                val tvSub = holder.itemView.findViewById<TextView>(android.R.id.text2)

                val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    .format(Date(report.createdAt))
                val status = if (report.status == "sent") "✅ Отправлен" else "📝 Черновик"
                tvTitle.text = report.title
                tvSub.text = "$date • $status"

                holder.itemView.setOnClickListener {
                    showReportDialog(report)
                }

                holder.itemView.setOnLongClickListener {
                    showDeleteDialog(report)
                    true
                }
            }

            override fun getItemCount() = reports.size
        }
        recyclerView.adapter = adapter
    }

    private fun showReportDialog(report: Report) {
        val values = report.values.split(";").joinToString("\n") { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) "${parts[0]}: ${parts[1]}" else pair
        }

        AlertDialog.Builder(this)
            .setTitle(report.title)
            .setMessage("Дата: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(report.createdAt))}\n\n$values")
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun showDeleteDialog(report: Report) {
        AlertDialog.Builder(this)
            .setTitle("Удалить отчет?")
            .setMessage(report.title)
            .setPositiveButton("Удалить") { _, _ ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { db.reportDao().delete(report) }
                        loadReports()
                        ErrorHandler.showSuccess(this@ReportsListActivity, "Отчет удален")
                    } catch (e: Exception) {
                        ErrorHandler.showError(this@ReportsListActivity, "Удаление отчета", e)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadReports()
    }
}
