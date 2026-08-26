package com.example.myfast.data

import android.content.Context
import android.content.SharedPreferences
import com.example.myfast.presentation.screens.FastRecord
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FastingRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fasting_prefs", Context.MODE_PRIVATE)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    fun saveFastRecord(record: FastRecord) {
        val records = getFastRecords().toMutableList()
        val newRecord = record.copy(id = (records.maxOfOrNull { it.id } ?: 0) + 1)
        records.add(newRecord)
        saveFasts(records)
    }
    
    fun getFastRecords(): List<FastRecord> {
        val recordsStr = prefs.getString("fast_records", "") ?: ""
        if (recordsStr.isEmpty()) return emptyList()
        
        return recordsStr.split("|").mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size >= 5) {
                try {
                    FastRecord(
                        id = parts[0].toInt(),
                        plan = parts[1],
                        date = LocalDateTime.parse(parts[2], dateFormatter),
                        durationSeconds = parts[3].toInt(),
                        isOngoing = parts.getOrNull(4)?.toBoolean() ?: false
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
    
    fun deleteFastRecord(id: Int) {
        val records = getFastRecords().filter { it.id != id }
        saveFasts(records)
    }
    
    private fun saveFasts(records: List<FastRecord>) {
        val recordsStr = records.joinToString("|") { record ->
            "${record.id},${record.plan},${record.date.format(dateFormatter)},${record.durationSeconds},${record.isOngoing}"
        }
        prefs.edit().putString("fast_records", recordsStr).apply()
    }
}
