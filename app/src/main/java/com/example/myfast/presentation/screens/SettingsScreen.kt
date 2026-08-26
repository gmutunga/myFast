package com.example.myfast.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfast.data.UserProfile
import com.example.myfast.data.UserProfileRepository
import com.example.myfast.data.WeightRepository
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    isDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val userProfileRepository = remember { UserProfileRepository(context) }
    val weightRepository = remember { WeightRepository(context) }
    val initialProfile = remember { userProfileRepository.getUserProfile() }
    
    var weight by remember { mutableStateOf(initialProfile.weight.toString()) }
    var height by remember { mutableStateOf(initialProfile.height.toString()) }
    var goalWeight by remember { mutableStateOf(initialProfile.goalWeight.toString()) }
    var targetMonths by remember { mutableStateOf(initialProfile.targetMonths.toString()) }
    var isSaved by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(isDarkMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Settings",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            // Theme Section
            item {
                SettingsSectionHeader("Appearance")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dark Mode",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { newMode ->
                                darkMode = newMode
                                onDarkModeChange(newMode)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Profile Section
            item {
                SettingsSectionHeader("Your Profile")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current Weight
                    EditableProfileField(
                        label = "Current Weight (kg)",
                        value = weight,
                        onValueChange = {
                            weight = it
                            isSaved = false
                        },
                        keyboardType = KeyboardType.Decimal
                    )

                    // Height
                    EditableProfileField(
                        label = "Height (cm)",
                        value = height,
                        onValueChange = {
                            height = it
                            isSaved = false
                        },
                        keyboardType = KeyboardType.Decimal
                    )

                    // Goal Weight
                    EditableProfileField(
                        label = "Goal Weight (kg)",
                        value = goalWeight,
                        onValueChange = {
                            goalWeight = it
                            isSaved = false
                        },
                        keyboardType = KeyboardType.Decimal
                    )

                    // Target Time (dropdown)
                    EditableTargetMonthsField(
                        value = targetMonths,
                        onValueChange = {
                            targetMonths = it
                            isSaved = false
                        }
                    )

                    // Save Button
                    Button(
                        onClick = {
                            val w = weight.toFloatOrNull() ?: 0f
                            val h = height.toFloatOrNull() ?: 0f
                            val g = goalWeight.toFloatOrNull() ?: 0f
                            val m = targetMonths.toIntOrNull() ?: 0
                            
                            val profile = UserProfile(
                                weight = w,
                                height = h,
                                goalWeight = g,
                                targetMonths = m,
                                hasCompletedSetup = true
                            )
                            userProfileRepository.saveUserProfile(profile)
                            
                            // Log weight if it changed
                            if (w > 0) {
                                weightRepository.addWeightLog(w)
                            }
                            
                            isSaved = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isSaved) "✓ Saved" else "Save Changes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // BMI and Health Summary Section
            item {
                val w = weight.toFloatOrNull() ?: 0f
                val h = height.toFloatOrNull() ?: 0f
                val g = goalWeight.toFloatOrNull() ?: 0f
                
                if (w > 0 && h > 0) {
                    val bmi = w / ((h / 100f) * (h / 100f))
                    val lowBMI = 18.5f
                    val highBMI = 24.9f
                    val lowWeight = lowBMI * (h / 100f) * (h / 100f)
                    val highWeight = highBMI * (h / 100f) * (h / 100f)
                    
                    SettingsSectionHeader("Health Summary")
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Current BMI
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current BMI",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.1f", bmi),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Divider()

                            // Healthy BMI Range
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Healthy Weight Range",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.1f", lowWeight)} - ${String.format("%.1f", highWeight)} kg",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }

                            // Weight Loss Timeline
                            if (g > 0 && w > g) {
                                Divider()
                                
                                val weightToLose = w - g
                                val weeksNeeded = (weightToLose / 0.5f).toInt()
                                
                                Text(
                                    text = "By losing 0.5 kg per week, you can achieve your target weight in approximately $weeksNeeded weeks",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // About
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FastSpire",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "v1.0.0",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "Track your fasting journey",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditableProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
            )
        }
    }
}

@Composable
fun EditableTargetMonthsField(
    value: String,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Target Time (months)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy()
                ) {
                    Text(
                        text = if (value.isEmpty() || value.toIntOrNull() == 0) "Select target time" 
                               else "$value month${if (value.toIntOrNull() ?: 1 > 1) "s" else ""}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    (1..12).forEach { month ->
                        DropdownMenuItem(
                            text = { Text("$month month${if (month > 1) "s" else ""}") },
                            onClick = {
                                onValueChange(month.toString())
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}
