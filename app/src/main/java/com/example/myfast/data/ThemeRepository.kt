package com.example.myfast.data

import android.content.Context
import android.content.SharedPreferences

class ThemeRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    fun isDarkMode(): Boolean {
        return prefs.getBoolean("dark_mode", false)
    }
    
    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean("dark_mode", isDark).apply()
    }
}
