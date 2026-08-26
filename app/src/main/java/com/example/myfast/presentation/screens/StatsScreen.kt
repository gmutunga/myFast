package com.example.myfast.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfast.data.WeightLog
import com.example.myfast.data.WeightRepository
import com.example.myfast.data.UserProfileRepository
import com.example.myfast.data.FastingRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private enum class FastingChartRange(val label: String, val periodDescription: String) {
    WEEK("Week", "Last 7 days"),
    MONTH("Month", "This month"),
    YEAR("Year", "This year")
}

private data class FastingChartBucket(
    val label: String,
    val startDate: LocalDate,
    val endDateExclusive: LocalDate
)

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val weightRepository = remember { WeightRepository(context) }
    val userProfileRepository = remember { UserProfileRepository(context) }
    val fastingRepository = remember { FastingRepository(context) }
    
    val userProfile = remember { userProfileRepository.getUserProfile() }
    val weightLogs = remember { weightRepository.getWeightLogs() }
    val fastRecords = remember { fastingRepository.getFastRecords() }
    
    // Calculate weight progress
    val currentWeight = userProfile.weight
    val logsWithCurrent = remember {
        if (currentWeight > 0 && weightLogs.lastOrNull()?.weight != currentWeight.toFloat()) {
            weightLogs + listOf(
                WeightLog(
                    id = (weightLogs.maxOfOrNull { it.id } ?: 0) + 1,
                    date = LocalDateTime.now(),
                    weight = currentWeight
                )
            )
        } else {
            weightLogs
        }
    }
    
    val latestLog = logsWithCurrent.lastOrNull()
    val previousLog = if (logsWithCurrent.size >= 2) logsWithCurrent[logsWithCurrent.size - 2] else null
    
    val weightChange = if (previousLog != null && latestLog != null) {
        latestLog.weight - previousLog.weight
    } else {
        0f
    }
    
    // Calculate fasting stats
    val totalFasts = fastRecords.size
    
    val longestFastSeconds = if (fastRecords.isNotEmpty()) {
        fastRecords.maxOf { it.durationSeconds }
    } else {
        0
    }
    
    val totalFastingSeconds = fastRecords.sumOf { it.durationSeconds }
    val totalFastingHours = totalFastingSeconds / 3600
    
    val totalWeightLoss = if (logsWithCurrent.isNotEmpty()) {
        logsWithCurrent.first().weight - logsWithCurrent.last().weight
    } else {
        0f
    }
    
    // Calculate average fasts this week
    val sevenDaysAgo = LocalDateTime.now().minusDays(7)
    val fastsThisWeek = fastRecords.filter { it.date.isAfter(sevenDaysAgo) }
    val averageThisWeekSeconds = if (fastsThisWeek.isNotEmpty()) {
        fastsThisWeek.sumOf { it.durationSeconds } / fastsThisWeek.size
    } else {
        0
    }
    
    // Calculate current streak (consecutive days with at least one fast)
    val currentStreak = remember {
        var streak = 0
        var checkDate = LocalDateTime.now().toLocalDate()
        
        while (true) {
            val fastsOnDate = fastRecords.filter { 
                it.date.toLocalDate() == checkDate
            }
            if (fastsOnDate.isEmpty()) {
                break
            }
            streak++
            checkDate = checkDate.minusDays(1)
        }
        streak
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Dashboard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            // Summary Cards
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total Fasts",
                            value = totalFasts.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Current Streak",
                            value = "$currentStreak days",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Longest Fast",
                            value = formatSeconds(longestFastSeconds.toLong()),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Avg This Week",
                            value = formatSeconds(averageThisWeekSeconds.toLong()),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total Hours",
                            value = totalFastingHours.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Total Weight Loss",
                            value = String.format("%.1f kg", totalWeightLoss),
                            modifier = Modifier.weight(1f),
                            valueColor = if (totalWeightLoss > 0) Color(0xFF4CAF50) else Color(0xFFFF6B6B)
                        )
                    }
                }
            }

            item {
                FastingActivityChart(fastRecords)
            }

            // Latest Weight Section
            item {
                Text(
                    text = "Weight Progress",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
                )
            }

            if (latestLog != null) {
                item {
                    LatestWeightCard(latestLog, weightChange)
                }
                
                // Weight Chart
                item {
                    WeightChartCard(logsWithCurrent)
                }
            } else {
                item {
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No weight logs yet",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Add your weight in Settings to start tracking progress",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FastingActivityChart(fastRecords: List<FastRecord>) {
    var selectedRange by remember { mutableStateOf(FastingChartRange.WEEK) }
    val today = LocalDate.now()
    
    val oldestFastDate = remember(fastRecords) {
        fastRecords.minByOrNull { it.date }?.date?.toLocalDate()
    }
    
    val buckets = remember(selectedRange, today, oldestFastDate) {
        fastingChartBuckets(selectedRange, today, oldestFastDate)
    }
    val hoursByBucket = remember(fastRecords, buckets) {
        buckets.map { bucket ->
            fastRecords.sumOf { record ->
                record.secondsWithin(bucket.startDate, bucket.endDateExclusive)
            } / 3600f
        }
    }
    val totalHours = hoursByBucket.sum()

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
                .padding(16.dp)
        ) {
            Text(
                text = "Fasting Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FastingChartRange.entries.forEach { range ->
                    val selected = range == selectedRange
                    Button(
                        onClick = { selectedRange = range },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.background
                            },
                            contentColor = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            }
                        ),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(range.label, fontSize = 13.sp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedRange.periodDescription,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total: %.1f h".format(totalHours),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            FastingBarChart(
                values = hoursByBucket,
                labels = buckets.map { it.label },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun FastingBarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val maximumHours = maxOf(values.maxOrNull() ?: 0f, 1f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            val chartHeight = size.height
            val chartWidth = size.width
            val barSlotWidth = chartWidth / values.size
            val barWidth = barSlotWidth * 0.58f

            repeat(4) { index ->
                val y = chartHeight * index / 3f
                drawLine(
                    color = Color.Gray.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            values.forEachIndexed { index, hours ->
                val barHeight = (hours / maximumHours) * chartHeight
                val left = index * barSlotWidth + (barSlotWidth - barWidth) / 2f
                drawRect(
                    color = Color(0xFF4CAF50),
                    topLeft = Offset(left, chartHeight - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

private fun fastingChartBuckets(
    range: FastingChartRange,
    today: LocalDate,
    oldestFastDate: LocalDate?
): List<FastingChartBucket> = when (range) {
    FastingChartRange.WEEK -> (6 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        FastingChartBucket(
            label = date.format(DateTimeFormatter.ofPattern("MMM d")),
            startDate = date,
            endDateExclusive = date.plusDays(1)
        )
    }

    FastingChartRange.MONTH -> {
        val twentyEightDaysAgo = today.minusDays(28)
        val periodStart = if (oldestFastDate != null && oldestFastDate.isAfter(twentyEightDaysAgo)) {
            oldestFastDate
        } else {
            twentyEightDaysAgo
        }
        
        buildList {
            var weekStart = periodStart
            while (weekStart.isBefore(today)) {
                val weekEndExclusive = minOf(weekStart.plusDays(7), today.plusDays(1))
                add(
                    FastingChartBucket(
                        label = weekStart.format(DateTimeFormatter.ofPattern("MMM d")),
                        startDate = weekStart,
                        endDateExclusive = weekEndExclusive
                    )
                )
                weekStart = weekEndExclusive
            }
        }
    }

    FastingChartRange.YEAR -> {
        val startDate = if (oldestFastDate != null) {
            val oldestYear = oldestFastDate.year
            val todayYear = today.year
            if (oldestYear == todayYear) {
                oldestFastDate.withDayOfMonth(1)
            } else {
                today.minusMonths(11)
            }
        } else {
            today.minusMonths(11)
        }

        buildList {
            var monthStart = startDate
            val monthEnd = today.withDayOfMonth(1)
            while (monthStart.isBefore(monthEnd) || monthStart == monthEnd) {
                val monthEndExclusive = monthStart.plusMonths(1)
                add(
                    FastingChartBucket(
                        label = monthStart.format(DateTimeFormatter.ofPattern("MMM")),
                        startDate = monthStart,
                        endDateExclusive = monthEndExclusive
                    )
                )
                monthStart = monthEndExclusive
            }
        }
    }
}

private fun FastRecord.secondsWithin(startDate: LocalDate, endDateExclusive: LocalDate): Long {
    val periodStart = startDate.atStartOfDay()
    val periodEnd = endDateExclusive.atStartOfDay()
    val fastEnd = date.plusSeconds(durationSeconds.toLong())
    val overlapStart = maxOf(date, periodStart)
    val overlapEnd = minOf(fastEnd, periodEnd)

    return Duration.between(overlapStart, overlapEnd).seconds.coerceAtLeast(0)
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LatestWeightCard(log: WeightLog, weightChange: Float) {
    val arrowColor = when {
        weightChange < 0 -> Color(0xFF4CAF50) // Green for weight loss
        weightChange > 0 -> Color(0xFFE53935) // Red for weight gain
        else -> MaterialTheme.colorScheme.onSurfaceVariant // Neutral for no change
    }
    
    val arrowSymbol = when {
        weightChange < 0 -> "↓" // Down arrow for loss
        weightChange > 0 -> "↑" // Up arrow for gain
        else -> "→" // Right arrow for no change
    }
    
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Latest Weight",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").format(log.date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.1f kg".format(log.weight),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (weightChange != 0f) {
                        Text(
                            text = String.format("%.1f kg", kotlin.math.abs(weightChange)),
                            fontSize = 12.sp,
                            color = arrowColor,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                
                if (arrowSymbol.isNotEmpty()) {
                    Text(
                        text = arrowSymbol,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = arrowColor
                    )
                }
            }
        }
    }
}

@Composable
fun WeightChartCard(logs: List<WeightLog>) {
    if (logs.isEmpty()) return
    
    val minWeight = logs.minOf { it.weight }
    val maxWeight = logs.maxOf { it.weight }
    val weightRange = maxWeight - minWeight
    val padding = if (weightRange > 0) weightRange * 0.1f else 5f
    
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
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
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                // Grid background
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    repeat(4) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = Color.Gray.copy(alpha = 0.1f),
                            thickness = 1.dp
                        )
                    }
                }
                
                // Canvas for lines and interactive points
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    val minY = minWeight - padding
                    val maxY = maxWeight + padding
                    val yRange = maxY - minY
                    
                    val chartWidth = size.width
                    val chartHeight = size.height
                    
                    // Calculate point positions
                    val pointPositions = logs.mapIndexed { index, log ->
                        val x = (index.toFloat() / (logs.size - 1)) * chartWidth
                        val y = chartHeight - ((log.weight - minY) / yRange) * chartHeight
                        Offset(x, y)
                    }
                    
                    // Draw connecting lines
                    for (i in 0 until pointPositions.size - 1) {
                        drawLine(
                            color = Color(0xFF4CAF50),
                            start = pointPositions[i],
                            end = pointPositions[i + 1],
                            strokeWidth = 3f
                        )
                    }
                    
                    // Draw circles at each point
                    pointPositions.forEachIndexed { index, offset ->
                        val isSelected = selectedIndex == index
                        val radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx()
                        val color = if (isSelected) Color(0xFF2E7D32) else Color(0xFF4CAF50)
                        
                        drawCircle(
                            color = color,
                            radius = radius,
                            center = offset
                        )
                    }
                }
                
                // Interactive overlay for click detection
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val minY = minWeight - padding
                    val maxY = maxWeight + padding
                    val yRange = maxY - minY
                    
                    logs.forEachIndexed { index, log ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    selectedIndex = if (selectedIndex == index) null else index
                                }
                        )
                    }
                }
                
                // Tooltip
                if (selectedIndex != null && selectedIndex!! < logs.size) {
                    val selectedLog = logs[selectedIndex!!]
                    val tooltipX = (selectedIndex!!.toFloat() / (logs.size - 1))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = if (tooltipX < 0.5f) Alignment.TopStart else Alignment.TopEnd
                    ) {
                        Card(
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format("%.1f kg", selectedLog.weight),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = DateTimeFormatter.ofPattern("MMM dd").format(selectedLog.date),
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = DateTimeFormatter.ofPattern("HH:mm").format(selectedLog.date),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
            
            // X-axis with start and end weights/dates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Start weight and date
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = String.format("%.1f kg", logs.first().weight),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = DateTimeFormatter.ofPattern("MM/dd").format(logs.first().date),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Entry count in center
                Text(
                    text = "${logs.size} entries",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // End weight and date
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format("%.1f kg", logs.last().weight),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = DateTimeFormatter.ofPattern("MM/dd").format(logs.last().date),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
