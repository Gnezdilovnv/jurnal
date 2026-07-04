package com.example.reports.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.reports.R
import com.example.reports.data.Category

class CategoriesAdapter(
    private val categories: List<Category>,
    private val subCounts: Map<String, Int>,
    private val onItemClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category)
    }

    override fun getItemCount() = categories.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName = itemView.findViewById<TextView>(R.id.tvName)
        private val tvSubCount = itemView.findViewById<TextView>(R.id.tvSubCount)

        fun bind(category: Category) {
            tvName.text = category.name
            val count = subCounts[category.id] ?: 0
            tvSubCount.text = "$count подкатегорий"
            itemView.setOnClickListener { onItemClick(category) }
        }
    }
}
