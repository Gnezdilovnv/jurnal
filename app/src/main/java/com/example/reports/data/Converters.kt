package com.example.reports.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String): Map<String, String> {
        if (value.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, String>()
        value.split(";").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2) {
                map[parts[0]] = parts[1]
            }
        }
        return map
    }

    @TypeConverter
    fun fromMap(map: Map<String, String>): String {
        return map.map { "${it.key}:${it.value}" }.joinToString(";")
    }
}
