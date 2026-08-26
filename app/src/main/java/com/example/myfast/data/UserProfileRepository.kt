package com.example.myfast.data

import android.content.Context
import android.content.SharedPreferences

data class UserProfile(
    val weight: Float = 0f,  // in kg
    val height: Float = 0f,  // in cm
    val goalWeight: Float = 0f,  // in kg
    val targetMonths: Int = 0,  // target months to achieve goal
    val hasCompletedSetup: Boolean = false
)

class UserProfileRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
    
    fun getUserProfile(): UserProfile {
        return UserProfile(
            weight = prefs.getFloat("weight", 0f),
            height = prefs.getFloat("height", 0f),
            goalWeight = prefs.getFloat("goal_weight", 0f),
            targetMonths = prefs.getInt("target_months", 0),
            hasCompletedSetup = prefs.getBoolean("has_completed_setup", false)
        )
    }
    
    fun saveUserProfile(profile: UserProfile) {
        prefs.edit().apply {
            putFloat("weight", profile.weight)
            putFloat("height", profile.height)
            putFloat("goal_weight", profile.goalWeight)
            putInt("target_months", profile.targetMonths)
            putBoolean("has_completed_setup", true)
        }.apply()
    }
    
    fun skipSetup() {
        prefs.edit().putBoolean("has_completed_setup", true).apply()
    }
    
    fun calculateBMI(weight: Float, height: Float): Float {
        if (height == 0f) return 0f
        return weight / ((height / 100f) * (height / 100f))
    }
    
    fun getHealthyWeightRange(height: Float): Pair<Float, Float> {
        val heightInMeters = height / 100f
        val minWeight = 18.5f * heightInMeters * heightInMeters
        val maxWeight = 24.9f * heightInMeters * heightInMeters
        return Pair(minWeight, maxWeight)
    }
    
    fun estimateWeeksToGoal(currentWeight: Float, goalWeight: Float): Int {
        // Average weight loss with fasting: 0.5-1 kg per week
        // Using conservative estimate of 0.5 kg per week
        val weightToLose = currentWeight - goalWeight
        return if (weightToLose > 0) {
            (weightToLose / 0.5f).toInt()
        } else {
            0
        }
    }
}
