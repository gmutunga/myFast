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

// Poles of the diagonal gradient: magenta/pink at lower-left, blue at upper-right.
// Canvas angle convention: 0deg = right (3 o'clock), increasing clockwise (y grows downward).
internal const val GRADIENT_BLUE_POLE_DEG = 315f // upper-right
internal val GradientMagenta = Color(0xFFFF2E93)
internal val GradientBlue = Color(0xFF2E93FF)

internal fun lerpChannel(a: Float, b: Float, t: Float) = a + (b - a) * t

internal fun blendGradientColor(t: Float, alpha: Float = 1f): Color = Color(
    red = lerpChannel(GradientMagenta.red, GradientBlue.red, t),
    green = lerpChannel(GradientMagenta.green, GradientBlue.green, t),
    blue = lerpChannel(GradientMagenta.blue, GradientBlue.blue, t),
    alpha = alpha
)

/**
 * Builds a seamless set of sweep-gradient color stops that smoothly cycles
 * magenta -> purple -> blue -> purple -> magenta around the full circle, with
 * blue peaking at the upper-right and magenta peaking at the lower-left
 * (the two points sit directly opposite each other, so the purple blend
 * appears naturally at the perpendicular points, with no hard seam).
 */
internal fun buildDiagonalGradientStops(alpha: Float = 1f, steps: Int = 36): List<Pair<Float, Color>> {
    return (0..steps).map { i ->
        val angleDeg = i * 360f / steps
        val fraction = angleDeg / 360f
        val t = (cos(Math.toRadians((angleDeg - GRADIENT_BLUE_POLE_DEG).toDouble())).toFloat() + 1f) / 2f
        fraction to blendGradientColor(t, alpha)
    }
}

@Composable
fun PremiumFastingProgressCircle(
    elapsedSeconds: Int,
    goalSeconds: Int,
    remainingSeconds: Int,
    fastStartTime: Long = 0L
) {
    val progressPercent = minOf(1f, elapsedSeconds.toFloat() / goalSeconds)

    // Gradient color stops are static, so compute them once.
    val mainGradientStops = remember { buildDiagonalGradientStops().toTypedArray() }
    val glowGradientStops = remember { buildDiagonalGradientStops(alpha = 0.3f).toTypedArray() }
    
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
                        *glowGradientStops,
                        center = Offset(centerX, centerY),
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
                        *mainGradientStops,
                        center = Offset(centerX, centerY),
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
