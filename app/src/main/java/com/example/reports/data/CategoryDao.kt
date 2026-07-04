package com.example.reports.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Category>>

    @Insert
    suspend fun insert(category: Category)
}
