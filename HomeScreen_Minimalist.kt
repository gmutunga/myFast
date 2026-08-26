package com.example.myfast.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun HomeScreen() {
    var isTimerActive by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var selectedPlan by remember { mutableStateOf<String?>(null) }
    var goalSeconds by remember { mutableStateOf(0) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var currentWeight by remember { mutableStateOf<Double?>(null) }
    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(false) }

    // Gentle pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulsing = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(isTimerActive) {
        while (isTimerActive) {
            delay(1000)
            elapsedSeconds++
        }
    }

    val remainingSeconds = if (goalSeconds > 0) maxOf(0, goalSeconds - elapsedSeconds) else 0
    val progressPercent = if (goalSeconds > 0) {
        min(elapsedSeconds.toFloat() / goalSeconds.toFloat(), 1f)
    } else {
        0f
    }

    val fastingStage = when {
        elapsedSeconds < 3 * 3600 -> "Fed State"
        elapsedSeconds < 8 * 3600 -> "Post-Absorptive"
        elapsedSeconds < 12 * 3600 -> "Fat Burning"
        else -> "Deep Ketosis"
    }

    val stageColors = listOf(
        Color(0xFFFF9800),                      // Fed State - orange
        Color(0xFFFFC107),                      // Post-Absorptive - amber
        MaterialTheme.colorScheme.primary,      // Fat Burning - purple
        Color(0xFF9C27B0)                       // Deep Ketosis - deep purple
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "myFast",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 20.dp)
        )

        // MINIMALIST TIMER - Easy Fast Style
        if (isTimerActive) {
            Spacer(modifier = Modifier.height(20.dp))

            // Clean circular progress timer
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

                // Minimalist circular progress - single ring
                Canvas(
                    modifier = Modifier
                        .size(260.dp)
                        .scale(pulsing.value)
                ) {
                    val radius = size.width / 2
                    val center = Offset(radius, radius)
                    val strokeWidth = 7f

                    // Very subtle background circle
                    drawCircle(
                        color = surfaceVariantColor.copy(alpha = 0.08f),
                        radius = radius - strokeWidth / 2,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    // Progress arc - clean and simple
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = progressPercent * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(
                            (radius - strokeWidth / 2) * 2,
                            (radius - strokeWidth / 2) * 2
                        ),
                        topLeft = Offset(center.x - (radius - strokeWidth / 2), center.y - (radius - strokeWidth / 2))
                    )
                }

                // Time display - Large and clear
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    // Main time - LARGE and bold
                    Text(
                        text = formatElapsedTime(
                            if (showRemainingTime) remainingSeconds else elapsedSeconds
                        ),
                        fontSize = 68.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    // Simple label
                    Text(
                        text = if (showRemainingTime) "remaining" else "fasted",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    // Secondary time info
                    Text(
                        text = if (showRemainingTime) {
                            "Fasted: ${formatElapsedTime(elapsedSeconds)}"
                        } else {
                            "Remaining: ${formatElapsedTime(remainingSeconds)}"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    // Stage indicator - simple and clear
                    Text(
                        text = fastingStage,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle button - minimal
            Button(
                onClick = { showRemainingTime = !showRemainingTime },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    if (showRemainingTime) "Show Time Fasted" else "Show Time Remaining",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // End Fast button
            Button(
                onClick = {
                    isTimerActive = false
                    elapsedSeconds = 0
                    selectedPlan = null
                    goalSeconds = 0
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("End Fast", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Log weight button
            Button(
                onClick = { showWeightDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Log Weight", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            // Not fasting - show quick start options
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Choose Your Fasting Plan",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 20.dp)
            )

            // Quick start buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    "16:8" to (16 * 3600),
                    "18:6" to (18 * 3600),
                    "20:4" to (20 * 3600)
                ).forEach { (label, seconds) ->
                    Button(
                        onClick = {
                            selectedPlan = label
                            goalSeconds = seconds
                            isTimerActive = true
                            elapsedSeconds = 0
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom duration
            OutlinedButton(
                onClick = { showCustomDurationDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Custom Duration", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Fasting Stages Info - Simple card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Fasting Stages",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Stage list
                listOf(
                    "Fed State (0-3h)" to "Body digests food.",
                    "Post-Absorptive (3-8h)" to "Digestion complete.",
                    "Fat Burning (8-12h)" to "Body burns fat.",
                    "Deep Ketosis (12h+)" to "Maximum fat burn."
                ).forEach { (stage, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "•",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = stage,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = desc,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Weight dialog
    if (showWeightDialog) {
        var weightInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("Log Weight") },
            text = {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (weightInput.isNotEmpty()) {
                        currentWeight = weightInput.toDoubleOrNull()
                        showWeightDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showWeightDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Custom duration dialog
    if (showCustomDurationDialog) {
        var hoursInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomDurationDialog = false },
            title = { Text("Custom Fast Duration") },
            text = {
                OutlinedTextField(
                    value = hoursInput,
                    onValueChange = { hoursInput = it },
                    label = { Text("Hours") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (hoursInput.isNotEmpty()) {
                        val hours = hoursInput.toIntOrNull() ?: 0
                        selectedPlan = "$hours:0"
                        goalSeconds = hours * 3600
                        isTimerActive = true
                        elapsedSeconds = 0
                        showCustomDurationDialog = false
                    }
                }) {
                    Text("Start")
                }
            },
            dismissButton = {
                Button(onClick = { showCustomDurationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatElapsedTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}
