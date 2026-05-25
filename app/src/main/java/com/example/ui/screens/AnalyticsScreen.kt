package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.Task
import com.example.ui.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsState()
    
    val totalScheduled = allTasks.size
    val totalCompleted = allTasks.count { it.isCompleted }
    val overallRate = if (totalScheduled > 0) (totalCompleted * 100) / totalScheduled else 0
    val totalFocusSeconds = allTasks.sumOf { it.timeSpentSeconds }
    val focusHours = totalFocusSeconds / 3600f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. Header ---
        item {
            Column {
                Text(
                    text = "Analytics Dashboard",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "Track your focus, completion rates, and historical consistency.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // --- 2. Key Insights Grid ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Key rate
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Completion Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "$overallRate%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Total hours
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Focus Hours", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = String.format("%.1f hrs", focusHours),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // --- 3. Custom Canvas Completion Rate Chart ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Weekly Completion Trends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Completed tasks count of the past 7 days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val weekData = computeWeeklyChartData(allTasks)
                    
                    // Render Custom Canvas Bar Chart
                    CompletionBarChart(
                        weekData = weekData,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        linesColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // --- 4. Category Productivity Weight Distribution ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Productivity Focus Weights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Category distribution across scheduled items.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val catDistribution = allTasks.groupBy { it.category }
                    if (catDistribution.isEmpty()) {
                        Text(
                            text = "No metrics to display yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            catDistribution.forEach { (cat, list) ->
                                val count = list.size
                                val fraction = count.toFloat() / totalScheduled
                                val completedCount = list.count { it.isCompleted }
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$completedCount/$count Completed (${(fraction * 100).toInt()}%)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { fraction },
                                        color = if (cat == "Work") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionBarChart(
    weekData: List<Pair<String, Int>>, // Days ("Mon") mapping completed task count
    primaryColor: Color,
    accentColor: Color,
    textColor: Color,
    linesColor: Color
) {
    val maxCount = (weekData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(3)
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(180, textColor.red.times(255).toInt(), textColor.green.times(255).toInt(), textColor.blue.times(255).toInt())
        textSize = 28f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 10.dp)
    ) {
        val width = size.width
        val height = size.height
        val barPadding = 12.dp.toPx()
        val bottomLabelSpace = 24.dp.toPx()
        
        val drawingHeight = height - bottomLabelSpace
        val countOfBars = weekData.size
        val blockWidth = width / countOfBars

        // Draw horizontal gridlines
        for (i in 0..maxCount) {
            val ratio = i.toFloat() / maxCount
            val y = drawingHeight - (ratio * drawingHeight)
            drawLine(
                color = linesColor.copy(alpha = 0.15f),
                start = Offset(x = 0f, y = y),
                end = Offset(x = width, y = y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw vertical bars
        weekData.forEachIndexed { idx, pair ->
            val (day, count) = pair
            val ratio = count.toFloat() / maxCount
            val barHeight = ratio * drawingHeight
            
            val blockLeft = idx * blockWidth
            val barWidth = blockWidth - (barPadding * 2)
            val barLeft = blockLeft + barPadding
            val barTop = drawingHeight - barHeight

            if (count > 0) {
                // Diagonal Gradient brush for bars
                val barBrush = Brush.verticalGradient(
                    colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f))
                )
                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(x = barLeft, y = barTop),
                    size = Size(width = barWidth, height = barHeight),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            } else {
                // Draw a small empty state placeholder dot
                drawCircle(
                    color = linesColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x = barLeft + (barWidth / 2), y = drawingHeight - 4.dp.toPx())
                )
            }

            // Draw Day Text Labels on the bottom
            drawContext.canvas.nativeCanvas.drawText(
                day,
                barLeft + (barWidth / 2),
                height - 4.dp.toPx(),
                textPaint
            )
        }
    }
}

// Compute the chart completions past 7 calendar days
fun computeWeeklyChartData(tasks: List<Task>): List<Pair<String, Int>> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("E", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    val list = mutableListOf<Pair<String, Int>>()
    repeat(7) {
        val dateString = sdf.format(calendar.time)
        val shortName = dayFormat.format(calendar.time)
        val countCompletedOnThisDate = tasks.count { it.date == dateString && it.isCompleted }
        
        list.add(Pair(shortName, countCompletedOnThisDate))
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    return list.reversed()
}
