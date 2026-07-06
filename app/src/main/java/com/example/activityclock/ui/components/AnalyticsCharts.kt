package com.example.activityclock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.activityclock.data.ActivityStats

// Extension function to format seconds into readable duration e.g., "2h 15m 30s"
fun Long.formatSecondsToDuration(): String {
    val hrs = this / 3600
    val mins = (this % 3600) / 60
    val secs = this % 60
    return when {
        hrs > 0 -> String.format("%dh %dm %ds", hrs, mins, secs)
        mins > 0 -> String.format("%dm %ds", mins, secs)
        else -> String.format("%ds", secs)
    }
}

// Parse hex color safely, falling back to gray if malformed
fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.Gray
    }
}

@Composable
fun DonutChart(
    statsList: List<ActivityStats>,
    modifier: Modifier = Modifier
) {
    val totalSeconds = statsList.sumOf { it.totalDurationSeconds }
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (totalSeconds == 0L) {
            // Empty State
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = surfaceVariantColor,
                    radius = size.minDimension / 2 - 20.dp.toPx(),
                    style = Stroke(width = 30.dp.toPx())
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No Data",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "0s tracked",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outerRadius = size.minDimension / 2 - 20.dp.toPx()
                val strokeWidth = 26.dp.toPx()
                var startAngle = -90f

                statsList.forEach { stat ->
                    val sweepAngle = stat.percentage * 360f
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = stat.activityColorHex.toColor(),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - 2f, // Subtle gap between slices
                            useCenter = false,
                            topLeft = Offset(
                                (size.width - outerRadius * 2) / 2,
                                (size.height - outerRadius * 2) / 2
                            ),
                            size = Size(outerRadius * 2, outerRadius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Tracked",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = totalSeconds.formatSecondsToDuration(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BarChart(
    statsList: List<ActivityStats>,
    modifier: Modifier = Modifier
) {
    if (statsList.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No stats data for this period",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return;
    }

    val maxDuration = statsList.maxOf { it.totalDurationSeconds }.toFloat()
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        statsList.take(6).forEach { stat ->
            val fraction = if (maxDuration > 0) stat.totalDurationSeconds / maxDuration else 0f
            val color = stat.activityColorHex.toColor()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stat.activityName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = stat.totalDurationSeconds.formatSecondsToDuration(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom Draw Bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                ) {
                    // Background bar track
                    drawRoundRect(
                        color = surfaceVariantColor,
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                    // Filled bar
                    drawRoundRect(
                        color = color,
                        size = Size(width = size.width * fraction, height = size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                }
            }
        }
    }
}
