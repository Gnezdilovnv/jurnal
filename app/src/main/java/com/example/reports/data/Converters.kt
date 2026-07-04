package com.example.reports.data
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter fun fromString(value: String): Map<String, String> {
        return if (value.isEmpty()) emptyMap() else Gson().fromJson(value, object : TypeToken<Map<String, String>>() {}.type)
    }
    @TypeConverter fun fromMap(map: Map<String, String>): String = Gson().toJson(map)
}
