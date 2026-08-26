package com.example.myfast.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class WeightLog(
    val id: Int,
    val date: LocalDateTime,
    val weight: Float
)

class WeightRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("weight_logs", Context.MODE_PRIVATE)
    
    private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME
    
    fun getWeightLogs(): List<WeightLog> {
        val logs = mutableListOf<WeightLog>()
        val logsJson = prefs.getString("weight_logs_list", "")
        
        if (logsJson.isNullOrEmpty()) return logs
        
        logsJson.split("|").forEach { entry ->
            if (entry.isNotEmpty()) {
                val parts = entry.split(",")
                if (parts.size == 3) {
                    try {
                        logs.add(
                            WeightLog(
                                id = parts[0].toInt(),
                                date = LocalDateTime.parse(parts[1], dateFormatter),
                                weight = parts[2].toFloat()
                            )
                        )
                    } catch (e: Exception) {
                        // Skip malformed entries
                    }
                }
            }
        }
        
        return logs.sortedBy { it.date }
    }
    
    fun addWeightLog(weight: Float) {
        val currentLogs = getWeightLogs().toMutableList()
        val newId = (currentLogs.maxOfOrNull { it.id } ?: 0) + 1
        
        currentLogs.add(
            WeightLog(
                id = newId,
                date = LocalDateTime.now(),
                weight = weight
            )
        )
        
        saveWeightLogs(currentLogs)
    }
    
    fun saveWeightLogs(logs: List<WeightLog>) {
        val logsString = logs.map { "${it.id},${it.date.format(dateFormatter)},${it.weight}" }
            .joinToString("|")
        
        prefs.edit().putString("weight_logs_list", logsString).apply()
    }
    
    fun getLatestWeightLog(): WeightLog? {
        return getWeightLogs().lastOrNull()
    }
}
