package com.example.myfast.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PremiumFastingProgressCircle(
    elapsedSeconds: Int,
    goalSeconds: Int,
    remainingSeconds: Int,
    fastStartTime: Long = 0L
) {
    val progressPercent = minOf(1f, elapsedSeconds.toFloat() / goalSeconds)
    
    // Format times with memoization to prevent blinking
    val startTime = remember(fastStartTime) {
        java.time.Instant.ofEpochMilli(fastStartTime)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
    val endTime = remember(fastStartTime, elapsedSeconds) {
        val endTimeMillis = fastStartTime + (elapsedSeconds * 1000L)
        java.time.Instant.ofEpochMilli(endTimeMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Circle container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
            ) {
                val circleRadius = size.minDimension / 2f
                val centerX = size.width / 2f
            val centerY = size.height / 2f
            val strokeWidth = 50f
            
            // Draw inactive (dark) portion
            drawArc(
                color = Color(0xFF1A1A2E),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(centerX - circleRadius + strokeWidth / 2, centerY - circleRadius + strokeWidth / 2),
                size = Size(
                    (circleRadius - strokeWidth / 2) * 2,
                    (circleRadius - strokeWidth / 2) * 2
                ),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Draw active (gradient) portion with glow
            if (progressPercent > 0) {
                // Draw glow layer
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF00B4FF).copy(alpha = 0.3f),
                            Color(0xFF6D5FFF).copy(alpha = 0.3f),
                            Color(0xFFAD00FF).copy(alpha = 0.3f),
                            Color(0xFFFF006E).copy(alpha = 0.3f),
                            Color(0xFF00B4FF).copy(alpha = 0.3f)
                        ),
                        center = Offset(centerX, centerY)
                    ),
                    startAngle = -90f,
                    sweepAngle = progressPercent * 360f,
                    useCenter = false,
                    topLeft = Offset(centerX - circleRadius + strokeWidth / 2 - 8, centerY - circleRadius + strokeWidth / 2 - 8),
                    size = Size(
                        (circleRadius - strokeWidth / 2 + 8) * 2,
                        (circleRadius - strokeWidth / 2 + 8) * 2
                    ),
                    style = Stroke(width = strokeWidth + 16, cap = StrokeCap.Round)
                )
                
                // Draw main gradient progress ring
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF00B4FF),  // Electric blue
                            Color(0xFF6D5FFF),  // Blue/violet
                            Color(0xFFAD00FF),  // Purple
                            Color(0xFFFF006E),  // Magenta/pink
                            Color(0xFF00B4FF)   // Back to blue
                        ),
                        center = Offset(centerX, centerY)
                    ),
                    startAngle = -90f,
                    sweepAngle = progressPercent * 360f,
                    useCenter = false,
                    topLeft = Offset(centerX - circleRadius + strokeWidth / 2, centerY - circleRadius + strokeWidth / 2),
                    size = Size(
                        (circleRadius - strokeWidth / 2) * 2,
                        (circleRadius - strokeWidth / 2) * 2
                    ),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            
            // Draw milestone markers (5 total)
            val milestones = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)
            val markerRadius = 20f
            
            milestones.forEachIndexed { index, milestonePercent ->
                val angle = -90f + (milestonePercent * 360f)
                val angleRad = Math.toRadians(angle.toDouble()).toFloat()
                
                val markerX = centerX + (circleRadius - strokeWidth / 2 - 10) * cos(angleRad)
                val markerY = centerY + (circleRadius - strokeWidth / 2 - 10) * sin(angleRad)
                
                val isCompleted = milestonePercent <= progressPercent
                val isCurrent = milestonePercent <= progressPercent && 
                    (index == milestones.indexOfFirst { it > progressPercent } || index == milestones.size - 1)
                
                // Draw marker background
                drawCircle(
                    color = if (isCompleted) Color(0xFF1A3A4A) else Color(0xFF1A1A2E),
                    radius = markerRadius,
                    center = Offset(markerX, markerY)
                )
                
                // Draw marker border
                drawCircle(
                    color = when {
                        isCurrent -> Color(0xFF00B4FF)
                        isCompleted -> Color(0xFF6D5FFF)
                        else -> Color(0xFF3A3A4E)
                    },
                    radius = markerRadius,
                    center = Offset(markerX, markerY),
                    style = Stroke(width = 2f)
                )
                
                // Draw glow for current/completed
                if (isCurrent || isCompleted) {
                    drawCircle(
                        color = when {
                            isCurrent -> Color(0xFF00B4FF).copy(alpha = 0.2f)
                            else -> Color(0xFF6D5FFF).copy(alpha = 0.15f)
                        },
                        radius = markerRadius + 6,
                        center = Offset(markerX, markerY)
                    )
                }
            }
        }
        
        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            // Flame icon
            Text(
                text = "🔥",
                fontSize = 36.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // "Fasting for" label
            Text(
                text = "Fasting for",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Main timer
            Text(
                text = String.format("%02d:%02d", elapsedSeconds / 3600, (elapsedSeconds % 3600) / 60),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Seconds
            Text(
                text = String.format("%02ds", elapsedSeconds % 60),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Divider
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(1.dp)
                    .background(Color(0xFF3A3A4E))
                    .padding(vertical = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // "Remaining" label
            Text(
                text = "Remaining",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Remaining time - balanced font size
            Text(
                text = String.format("%02d:%02d", remainingSeconds / 3600, (remainingSeconds % 3600) / 60),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B6B)
            )
        }
        }
        
        // Start and End times below the circle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Start",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = startTime,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "End",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = endTime,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
