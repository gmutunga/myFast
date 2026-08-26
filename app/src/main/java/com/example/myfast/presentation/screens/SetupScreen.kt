package com.example.myfast.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfast.data.UserProfileRepository

@Composable
fun SetupScreen(
    onSetupComplete: (weight: Float, height: Float, goalWeight: Float, targetMonths: Int) -> Unit,
    onSkip: () -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var goalWeight by remember { mutableStateOf("") }
    var targetMonths by remember { mutableStateOf(0) }
    var expandedMonthDropdown by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    
    var currentWeight = weight.toFloatOrNull() ?: 0f
    var currentHeight = height.toFloatOrNull() ?: 0f
    var currentGoalWeight = goalWeight.toFloatOrNull() ?: 0f
    
    val repository = UserProfileRepository(androidx.compose.ui.platform.LocalContext.current)
    val bmi = if (currentHeight > 0 && currentWeight > 0) {
        repository.calculateBMI(currentWeight, currentHeight)
    } else {
        0f
    }
    
    val (minHealthyWeight, maxHealthyWeight) = repository.getHealthyWeightRange(currentHeight)
    val weeksToGoal = repository.estimateWeeksToGoal(currentWeight, currentGoalWeight)
    val maxWeightLossInTargetMonths = (targetMonths * 4.33f) * 0.5f // 4.33 weeks per month, 0.5kg per week
    val isGoalRealistic = if (currentGoalWeight > 0 && currentGoalWeight < currentWeight) {
        val weightToLose = currentWeight - currentGoalWeight
        weightToLose <= maxWeightLossInTargetMonths
    } else {
        false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            "Welcome to FastSpire",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            "Let's set up your profile (optional)",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Current Weight Input
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Current Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Height Input
        OutlinedTextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Goal Weight Input
        OutlinedTextField(
            value = goalWeight,
            onValueChange = { goalWeight = it },
            label = { Text("Goal Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Target Months Dropdown
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { expandedMonthDropdown = !expandedMonthDropdown },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (targetMonths == 0) "Select target time" else "Target: $targetMonths month${if (targetMonths > 1) "s" else ""}",
                    modifier = Modifier.weight(1f)
                )
                Text("▼", modifier = Modifier.padding(start = 8.dp))
            }
            
            DropdownMenu(
                expanded = expandedMonthDropdown,
                onDismissRequest = { expandedMonthDropdown = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                (1..12).forEach { month ->
                    DropdownMenuItem(
                        text = { Text("$month month${if (month > 1) "s" else ""}") },
                        onClick = {
                            targetMonths = month
                            expandedMonthDropdown = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Buttons - moved above health summary
        if (weight.isNotEmpty() && height.isNotEmpty() && goalWeight.isNotEmpty() && targetMonths > 0) {
            Button(
                onClick = {
                    onSetupComplete(currentWeight, currentHeight, currentGoalWeight, targetMonths)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Skip", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Show results if all fields are filled and target months selected
        if (weight.isNotEmpty() && height.isNotEmpty() && goalWeight.isNotEmpty() && targetMonths > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Health Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Current BMI
                    Text(
                        "Current BMI: ${String.format("%.1f", bmi)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Healthy Weight Range
                    Text(
                        "Healthy Weight Range",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${String.format("%.1f", minHealthyWeight)} - ${String.format("%.1f", maxHealthyWeight)} kg",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Time to Goal with educational message
                    if (currentGoalWeight < currentWeight) {
                        val weightToLose = currentWeight - currentGoalWeight
                        Text(
                            "You can safely lose ${String.format("%.1f", weightToLose)} kg by losing 0.5 kg per week in ~$weeksToGoal weeks",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    } else if (currentGoalWeight > currentWeight) {
                        Text(
                            "Goal weight is greater than current weight",
                            fontSize = 12.sp,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}
