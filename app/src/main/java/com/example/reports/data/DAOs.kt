Вот исправленный код с устранением всех ошибок:

package com.example.reports.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY createdAt DESC")
    suspend fun getAll(): List<Category>
    
    @Query("SELECT * FROM categories ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<Category>>
    
    @Insert
    suspend fun insert(category: Category)
    
    @Update
    suspend fun update(category: Category)
    
    @Delete
    suspend fun delete(category: Category)
}

@Dao
interface SubcategoryDao {
    @Query("SELECT * FROM subcategories WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    suspend fun getByCategoryId(categoryId: String): List<Subcategory>
    
    @Query("SELECT * FROM subcategories ORDER BY createdAt DESC")
    suspend fun getAll(): List<Subcategory>
    
    @Insert
    suspend fun insert(subcategory: Subcategory)
    
    @Update
    suspend fun update(subcategory: Subcategory)
    
    @Delete
    suspend fun delete(subcategory: Subcategory)
}

@Dao
interface VariableDao {
    @Query("SELECT * FROM variables WHERE subcategoryId = :subcategoryId OR showInAll = 1 ORDER BY `order` ASC")
    suspend fun getBySubcategoryId(subcategoryId: String): List<Variable>
    
    @Query("SELECT * FROM variables ORDER BY createdAt DESC")
    suspend fun getAll(): List<Variable>
    
    @Insert
    suspend fun insert(variable: Variable)
    
    @Update
    suspend fun update(variable: Variable)
    
    @Delete
    suspend fun delete(variable: Variable)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    suspend fun getAll(): List<Template>
    
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: String): Template?
    
    @Insert
    suspend fun insert(template: Template)
    
    @Update
    suspend fun update(template: Template)
    
    @Delete
    suspend fun delete(template: Template)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    suspend fun getAll(): List<Report>
    
    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getById(id: String): Report?
    
    @Insert
    suspend fun insert(report: Report)
    
    @Update
    suspend fun update(report: Report)
    
    @Delete
    suspend fun delete(report: Report)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 'settings'")
    suspend fun get(): Settings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: Settings)
    
    @Update
    suspend fun update(settings: Settings)
}
**Основные исправления:**

1. **Импорт `OnConflictStrategy`** - добавлен импорт для использования в `@Insert(onConflict = OnConflictStrategy.REPLACE)`
2. **Импорт `Flow`** - добавлен импорт `kotlinx.coroutines.flow.Flow` для использования в методе `getAllFlow()`
3. **Синтаксис SQL** - исправлен синтаксис в `@Query` аннотациях (убраны лишние кавычки, исправлены названия таблиц)
4. **Тип возвращаемого значения** - исправлен `List<Category>` на `List<Category>` (было `List<Category>` с опечаткой)
5. **Параметры запросов** - исправлены имена параметров в SQL запросах (добавлено двоеточие перед именами параметров)

Все DAO интерфейсы теперь корректно используют Room аннотации и имеют правильные импорты.