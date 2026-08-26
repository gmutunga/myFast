package com.example.myfast.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfast.data.FastingRepository
import com.example.myfast.data.UserProfileRepository
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

data class FastingStage(
    val name: String,
    val startHours: Float,
    val endHours: Float,
    val color: Color,
    val description: String
)

private fun formatFastingDateTime(dateTime: LocalDateTime, today: LocalDate): String {
    val day = when (dateTime.toLocalDate()) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }

    return "$day, ${dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

private fun formatTimePickerDateTime(dateTime: LocalDateTime): Pair<String, String> {
    val today = LocalDate.now()
    val dateStr = when (dateTime.toLocalDate()) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> dateTime.format(DateTimeFormatter.ofPattern("EEE MMM d"))
    }
    val timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    return Pair(dateStr, timeStr)
}

val FASTING_STAGES = listOf(
    FastingStage("Fed State", 0f, 2f, Color(0xFFFF9800), "Body digests food"),
    FastingStage("Post-Absorptive", 2f, 5f, Color(0xFFFFC107), "Digestion complete"),
    FastingStage("Fat Burning", 5f, 8f, Color(0xFF9C27B0), "Body burns fat"),
    FastingStage("Ketosis", 8f, 12f, Color(0xFF673AB7), "Maximum fat burn"),
    FastingStage("Deep Ketosis", 12f, 18f, Color(0xFF512DA8), "Cellular healing")
)

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val repository = remember { FastingRepository(context) }
    val userProfileRepository = remember { UserProfileRepository(context) }
    
    // Check if first time setup is needed
    val userProfile = remember { userProfileRepository.getUserProfile() }
    var showSetup by remember { mutableStateOf(!userProfile.hasCompletedSetup) }
    
    var isTimerActive by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var showRemainingTime by remember { mutableStateOf(false) }
    var goalSeconds by remember { mutableStateOf(57600) } // 16 hours default
    var lastFastEndTime by remember { mutableStateOf(0L) }
    var fastStartTime by remember { mutableStateOf(0L) }
    var selectedPlanName by remember { mutableStateOf("16:8") }
    var goalReached by remember { mutableStateOf(false) }
    var showGoalReachedDialog by remember { mutableStateOf(false) }
    
    // End Fast dialog states
    var showEndFastConfirm by remember { mutableStateOf(false) }  // First step: End Fast or Cancel
    var showSaveDeleteDialog by remember { mutableStateOf(false) }  // Second step: Save or Delete
    
    // Time picker states
    var showTimePicker by remember { mutableStateOf(false) }
    var editingStartTime by remember { mutableStateOf(true) }  // true for start, false for end
    var pickerDate by remember { mutableStateOf(LocalDate.now()) }
    var pickerHour by remember { mutableStateOf(0) }
    var pickerMinute by remember { mutableStateOf(0) }
    
    // Initialize lastFastEndTime from last recorded fast
    LaunchedEffect(Unit) {
        val fastRecords = repository.getFastRecords()
        if (fastRecords.isNotEmpty()) {
            val lastFast = fastRecords.last()
            val lastFastEndTimeFromRecord = lastFast.date.plusSeconds(lastFast.durationSeconds.toLong())
            lastFastEndTime = lastFastEndTimeFromRecord.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
    
    // Timer update
    LaunchedEffect(isTimerActive, fastStartTime, goalSeconds) {
        while (isTimerActive) {
            val now = System.currentTimeMillis()
            elapsedSeconds = maxOf(0, ((now - fastStartTime) / 1000).toInt())
            val hasStarted = now >= fastStartTime
            val hasReachedGoal = hasStarted && now >= fastStartTime + goalSeconds * 1000L
            if (hasReachedGoal && !goalReached) {
                goalReached = true
                showGoalReachedDialog = true
            }
            delay(1000)
        }
    }
    
    val timeSinceLastFast = if (lastFastEndTime > 0) {
        (System.currentTimeMillis() - lastFastEndTime) / 1000
    } else {
        0L
    }
    
    // Show setup screen on first launch
    if (showSetup) {
        SetupScreen(
            onSetupComplete = { weight, height, goalWeight, targetMonths ->
                userProfileRepository.saveUserProfile(
                    com.example.myfast.data.UserProfile(
                        weight = weight,
                        height = height,
                        goalWeight = goalWeight,
                        targetMonths = targetMonths,
                        hasCompletedSetup = true
                    )
                )
                showSetup = false
            },
            onSkip = {
                userProfileRepository.skipSetup()
                showSetup = false
            }
        )
        return
    }
    
    if (isTimerActive) {
        FastingAppTimerScreen(
            elapsedSeconds = elapsedSeconds,
            goalSeconds = goalSeconds,
            showRemainingTime = showRemainingTime,
            onToggleTimeView = { showRemainingTime = !showRemainingTime },
            onEndFast = {
                showEndFastConfirm = true  // Show first dialog
            },
            onSaveFast = { actualElapsedSeconds ->
                val fastRecord = FastRecord(
                    id = 0,
                    plan = selectedPlanName,
                    date = LocalDateTime.now().minusSeconds(actualElapsedSeconds.toLong()),
                    durationSeconds = actualElapsedSeconds,
                    isOngoing = false
                )
                repository.saveFastRecord(fastRecord)
            },
            fastStartTime = fastStartTime,
            goalReached = goalReached,
            showGoalReachedDialog = showGoalReachedDialog,
            onGoalReachedDialogDismiss = { showGoalReachedDialog = false },
            onEditStartTime = {
                val startDateTime = Instant.ofEpochMilli(fastStartTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
                pickerDate = LocalDate.now(ZoneId.systemDefault())
                pickerHour = startDateTime.hour
                pickerMinute = startDateTime.minute
                editingStartTime = true
                showTimePicker = true
            },
            onEditEndTime = {
                val startDateTime = Instant.ofEpochMilli(fastStartTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
                val endDateTime = startDateTime.plusSeconds(goalSeconds.toLong())
                pickerDate = LocalDate.now(ZoneId.systemDefault())
                pickerHour = endDateTime.hour
                pickerMinute = endDateTime.minute
                editingStartTime = false
                showTimePicker = true
            }
        )
    } else {
        FastingAppHomeScreen(
            isFasting = isTimerActive,
            timeSinceLastFast = timeSinceLastFast,
            onStartFast = {
                isTimerActive = true
                elapsedSeconds = 0
                fastStartTime = System.currentTimeMillis()
            },
            onSelectPlan = { hours, planName ->
                selectedPlanName = planName
                goalSeconds = (hours * 3600).toInt()
                isTimerActive = true
                elapsedSeconds = 0
                fastStartTime = System.currentTimeMillis()
            }
        )
    }
    
    // First step: End Fast or Cancel dialog
    if (showEndFastConfirm) {
        AlertDialog(
            onDismissRequest = { showEndFastConfirm = false },
            title = {
                Text("End Fast?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Do you want to end your fast?")
            },
            dismissButton = {
                Button(
                    onClick = {
                        showEndFastConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndFastConfirm = false
                        showSaveDeleteDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Text("End Fast")
                }
            }
        )
    }
    
    // Second step: Save or Delete dialog
    if (showSaveDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDeleteDialog = false },
            title = {
                Text("You have fasted for:", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        formatSeconds(elapsedSeconds.toLong()),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Save this fast to your history or delete it.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showSaveDeleteDialog = false
                        isTimerActive = false
                        lastFastEndTime = System.currentTimeMillis()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDeleteDialog = false
                        val fastRecord = FastRecord(
                            id = 0,
                            plan = selectedPlanName,
                            date = LocalDateTime.now().minusSeconds(elapsedSeconds.toLong()),
                            durationSeconds = elapsedSeconds,
                            isOngoing = false
                        )
                        repository.saveFastRecord(fastRecord)
                        isTimerActive = false
                        lastFastEndTime = System.currentTimeMillis()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Save")
                }
            }
        )
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        val today = LocalDate.now(ZoneId.systemDefault())
        val startDateTime = Instant.ofEpochMilli(fastStartTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val startDate = startDateTime.toLocalDate()
        val startHour = startDateTime.hour
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(if (editingStartTime) "Edit Start Time" else "Edit End Time", fontWeight = FontWeight.Bold)
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (i in -10..10) {
                            val date = pickerDate.plusDays(i.toLong())
                            val isDisabled = !editingStartTime && date.isBefore(startDate)
                            Button(
                                onClick = { if (!isDisabled) pickerDate = date },
                                enabled = !isDisabled,
                                modifier = Modifier
                                    .width(55.dp)
                                    .height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (date == pickerDate) MaterialTheme.colorScheme.primary 
                                                   else MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(3.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text(
                                    date.format(DateTimeFormatter.ofPattern("MMM d")),
                                    fontSize = 8.sp,
                                    color = if (date == pickerDate) Color.White else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = if (isDisabled) 0.5f else 1f
                                    )
                                )
                            }
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (hour in 0..23) {
                            val isDisabled = !editingStartTime && pickerDate == startDate && hour < startHour
                            Button(
                                onClick = { if (!isDisabled) pickerHour = hour },
                                enabled = !isDisabled,
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hour == pickerHour) MaterialTheme.colorScheme.primary 
                                                   else MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(3.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text(
                                    "%02d".format(hour),
                                    fontSize = 8.sp,
                                    color = if (hour == pickerHour) Color.White else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = if (isDisabled) 0.5f else 1f
                                    )
                                )
                            }
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (minute in 0..59) {
                            Button(
                                onClick = { pickerMinute = minute },
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (minute == pickerMinute) MaterialTheme.colorScheme.primary 
                                                   else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(3.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text(
                                    "%02d".format(minute),
                                    fontSize = 8.sp,
                                    color = if (minute == pickerMinute) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { showTimePicker = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selectedDateTime = LocalDateTime.of(pickerDate, java.time.LocalTime.of(pickerHour, pickerMinute))
                        val zoneId = ZoneId.systemDefault()
                        val selectedInstant = selectedDateTime.atZone(zoneId).toInstant()
                        val now = Instant.now()
                        val startInstant = Instant.ofEpochMilli(fastStartTime)
                        
                        if (editingStartTime) {
                            fastStartTime = selectedInstant.toEpochMilli()
                            val elapsedMillis = now.toEpochMilli() - selectedInstant.toEpochMilli()
                            elapsedSeconds = maxOf(0, (elapsedMillis / 1000).toInt())
                        } else {
                            if (selectedInstant.isBefore(startInstant)) {
                                return@Button
                            }
                            val newGoal = java.time.temporal.ChronoUnit.SECONDS.between(
                                startInstant,
                                selectedInstant
                            ).toInt()
                            goalSeconds = maxOf(0, newGoal)
                        }
                        
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Confirm")
                }
            }
        )
    }
}


@Composable
fun FastingAppHomeScreen(
    isFasting: Boolean,
    timeSinceLastFast: Long,
    onStartFast: () -> Unit,
    onSelectPlan: (Int, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            "FastSpire",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        if (isFasting) {
            Text(
                "Fasting: YES",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4CAF50)
            )
        } else {
            if (timeSinceLastFast > 0) {
                Text(
                    "Not Fasting For",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                val hours = timeSinceLastFast / 3600
                val minutes = (timeSinceLastFast % 3600) / 60
                Text(
                    "${hours}h ${minutes}m",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No fasting has been recorded so far",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "Pick one of the options below to start a fast",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            "Choose Your Fasting Plan",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FastingPlanButton("12:12 (12 hours fasting)", 12) { onSelectPlan(12, "12:12") }
            FastingPlanButton("14:10 (14 hours fasting)", 14) { onSelectPlan(14, "14:10") }
            FastingPlanButton("16:8 (16 hours fasting)", 16) { onSelectPlan(16, "16:8") }
            FastingPlanButton("18:6 (18 hours fasting)", 18) { onSelectPlan(18, "18:6") }
            FastingPlanButton("20:4 (20 hours fasting)", 20) { onSelectPlan(20, "20:4") }
            FastingPlanButton("OMAD (24 hours fasting)", 24) { onSelectPlan(24, "OMAD") }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Show "not fasting for" message instead of stages
        if (timeSinceLastFast > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Not Fasting For",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val hours = timeSinceLastFast / 3600
                    val minutes = (timeSinceLastFast % 3600) / 60
                    Text(
                        "${hours}h ${minutes}m",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun FastingPlanButton(label: String, hours: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FastingAppTimerScreen(
    elapsedSeconds: Int,
    goalSeconds: Int,
    showRemainingTime: Boolean,
    onToggleTimeView: () -> Unit,
    onEndFast: () -> Unit,
    onSaveFast: (Int) -> Unit,
    fastStartTime: Long,
    goalReached: Boolean,
    showGoalReachedDialog: Boolean,
    onGoalReachedDialogDismiss: () -> Unit,
    onEditStartTime: () -> Unit = {},
    onEditEndTime: () -> Unit = {}
) {
    val nowMillis = System.currentTimeMillis()
    val isFastingStarted = nowMillis >= fastStartTime
    val endTimeMillis = fastStartTime + goalSeconds * 1000L
    val currentElapsedSeconds = maxOf(0, ((nowMillis - fastStartTime) / 1000).toInt())
    val progressPercent = if (goalSeconds > 0) {
        minOf(1f, currentElapsedSeconds.toFloat() / goalSeconds)
    } else {
        0f
    }
    val remainingSeconds = maxOf(0, ((endTimeMillis - nowMillis) / 1000).toInt())
    
    // Calculate start and end times from the recorded start timestamp.
    val zoneId = ZoneId.systemDefault()
    val startDateTime = Instant.ofEpochMilli(fastStartTime).atZone(zoneId).toLocalDateTime()
    val endDateTime = startDateTime.plusSeconds(goalSeconds.toLong())
    val today = LocalDate.now(zoneId)
    val startTimeLabel = formatFastingDateTime(startDateTime, today)
    val endTimeLabel = formatFastingDateTime(endDateTime, today)
    
    // State for showing stage details
    var selectedStageIndex by remember { mutableStateOf(-1) }
    
    // Pulsing animation
    val pulsing = rememberInfiniteTransition(label = "pulse")
    val pulseScale = pulsing.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    // Get theme colors before Canvas
    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val circleTextColor = Color.White
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // Large circle timer with clickable water drops inside
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale.value)
                    ) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val strokeWidth = 20.dp.toPx()  // Much wider outline
                        val radius = size.width / 2 - strokeWidth / 2
                        
                        // Background circle with GREEN gradient outline
                        val greenLight = Color(0xFF4CAF50)  // Light green
                    val greenMedium = Color(0xFF388E3C) // Medium green
                    val greenDark = Color(0xFF1B5E20)   // Dark green
                    
                    drawCircle(
                        color = greenLight,
                        radius = radius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Progress arc with GREEN gradient (fills from light to dark)
                    val progressGreen = when {
                        progressPercent < 0.5f -> {
                            val ratio = (progressPercent * 2)
                            Color(
                                red = (76 + (56 - 76) * ratio).toInt(),
                                green = (175 + (142 - 175) * ratio).toInt(),
                                blue = (80 + (60 - 80) * ratio).toInt()
                            )
                        }
                        else -> {
                            val ratio = ((progressPercent - 0.5f) * 2)
                            Color(
                                red = (56 + (27 - 56) * ratio).toInt(),
                                green = (142 + (94 - 142) * ratio).toInt(),
                                blue = (60 + (32 - 60) * ratio).toInt()
                            )
                        }
                    }
                    
                    drawArc(
                        color = progressGreen,
                        startAngle = -90f,
                        sweepAngle = progressPercent * 360f,
                        useCenter = false,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Water drop markers at 0h, 2h, 5h, 8h, 12h - positioned inside the stroke
                    val totalHours = goalSeconds / 3600f
                    val dropHours = listOf(0f, 2f, 5f, 8f, 12f)
                    
                    dropHours.forEach { hours ->
                        if (hours <= totalHours) {
                            val progressAtHour = (hours / totalHours) * 360f
                            val angle = -90f + progressAtHour
                            val rad = Math.toRadians(angle.toDouble())
                            
                            // Position inside the circle outline (on the stroke ring)
                            val dropRadius = radius - strokeWidth / 3  // Inside the thick stroke
                            val dropX = centerX + dropRadius * cos(rad).toFloat()
                            val dropY = centerY + dropRadius * sin(rad).toFloat()
                            
                            // Draw water drop
                            drawCircle(
                                color = Color(0xFF2196F3),
                                radius = 12.dp.toPx(),
                                center = Offset(dropX, dropY)
                            )
                        }
                    }
                }
                
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (!isFastingStarted) {
                            // Fasting not started yet
                            Text(
                                "Fasting not started",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = circleTextColor
                            )
                        } else if (!showRemainingTime) {
                            // Large "Fasting for" at top
                            Text(
                                "Fasting for",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = circleTextColor
                            )
                            Text(
                                "${currentElapsedSeconds / 3600}h",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = circleTextColor
                            )
                        } else {
                            // Large "Time remaining" at top
                            Text(
                                "Time remaining",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = circleTextColor
                            )
                            Text(
                                String.format("%02d:%02d", remainingSeconds / 3600, (remainingSeconds % 3600) / 60),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = circleTextColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Toggle arrow INSIDE the circle
                        if (isFastingStarted) {
                            Text(
                                "⇅",
                                fontSize = 24.sp,
                                color = Color(0xFF4CAF50),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clickable { onToggleTimeView() }
                                    .padding(2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Small time display at bottom (swaps with top)
                        if (isFastingStarted) {
                            if (!showRemainingTime) {
                                // Show small "Time remaining" at bottom
                                Text(
                                    "until fast ends",
                                    fontSize = 9.sp,
                                    color = onSurfaceVariantColor
                                )
                                Text(
                                    String.format("%02d:%02d", remainingSeconds / 3600, (remainingSeconds % 3600) / 60),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = circleTextColor
                                )
                            } else {
                                // Show small "Fasting for" at bottom
                                Text(
                                    "fasting for",
                                    fontSize = 9.sp,
                                    color = onSurfaceVariantColor
                                )
                                Text(
                                    "${currentElapsedSeconds / 3600}h",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = circleTextColor
                                )
                            }
                        }
                    }
                
                // Clickable dots overlay - only show when fasting has started
                if (isFastingStarted && goalSeconds > 0) {
                    val totalHours = goalSeconds / 3600f
                    val dropHours = listOf(0f, 2f, 5f, 8f, 12f)
                    val strokeWidth = 20f
                    val radius = 140f - strokeWidth / 2
                    val centerX = 140f
                    val centerY = 140f
                    
                    dropHours.forEachIndexed { index, hours ->
                        if (hours <= totalHours) {
                            val progressAtHour = (hours / totalHours) * 360f
                            val angle = -90f + progressAtHour
                            val rad = Math.toRadians(angle.toDouble())
                            
                            val dropRadius = radius - strokeWidth / 3
                            val dropX = centerX + dropRadius * cos(rad).toFloat()
                            val dropY = centerY + dropRadius * sin(rad).toFloat()
                            
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .offset(
                                        x = (dropX - 14).dp,
                                        y = (dropY - 14).dp
                                    )
                                    .clickable { selectedStageIndex = index }
                                    .padding(2.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Start and end times
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        surfaceVariantColor,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.clickable { onEditStartTime() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Start",
                        fontSize = 10.sp,
                        color = onSurfaceVariantColor
                    )
                    val (startDate, startTime) = formatTimePickerDateTime(startDateTime)
                    Text(
                        startDate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onBackgroundColor
                    )
                    Text(
                        startTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBackgroundColor
                    )
                }
                
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(50.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                Column(
                    modifier = Modifier.clickable { onEditEndTime() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "End",
                        fontSize = 10.sp,
                        color = onSurfaceVariantColor
                    )
                    val (endDate, endTime) = formatTimePickerDateTime(endDateTime)
                    Text(
                        endDate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onBackgroundColor
                    )
                    Text(
                        endTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = onBackgroundColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // End Fast button
            Button(
                onClick = { onEndFast() },
                enabled = isFastingStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252),
                    disabledContainerColor = Color(0xFFCCCCCC)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "End Fast",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFastingStarted) Color.White else Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
    
    // Show stage details in dialog if a dot was tapped
    if (selectedStageIndex in FASTING_STAGES.indices) {
        val stage = FASTING_STAGES[selectedStageIndex]
        AlertDialog(
            onDismissRequest = { selectedStageIndex = -1 },
            title = {
                Text(stage.name, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Hours: ${stage.startHours.toInt()}-${stage.endHours.toInt()}h",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stage.description,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedStageIndex = -1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = stage.color
                    )
                ) {
                    Text("OK")
                }
            }
        )
    }
}

fun formatSeconds(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

fun minOf(a: Float, b: Float) = if (a < b) a else b
fun maxOf(a: Int, b: Int) = if (a > b) a else b
