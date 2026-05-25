package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.Task
import com.example.ui.TaskViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlannerScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasksForSelectedDate.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var runningTaskId by remember { mutableStateOf<Int?>(null) }
    var timerSeconds by remember { mutableStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Active Focus Timer Side Effect
    LaunchedEffect(isTimerRunning, runningTaskId) {
        if (isTimerRunning && runningTaskId != null) {
            while (isTimerRunning) {
                delay(1000)
                timerSeconds++
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("planner_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- 1. Week Scroll Strip ---
            WeekCalendarStrip(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) }
            )
            
            // --- 2. Focus Stopwatch Board (If active) ---
            AnimatedVisibility(
                visible = runningTaskId != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val runningTask = tasks.find { it.id == runningTaskId }
                if (runningTask != null) {
                    FocusTimerBanner(
                        taskTitle = runningTask.title,
                        elapsedSeconds = timerSeconds,
                        isRunning = isTimerRunning,
                        onToggle = { isTimerRunning = !isTimerRunning },
                        onStop = {
                            viewModel.updateTask(
                                runningTask.copy(
                                    timeSpentSeconds = runningTask.timeSpentSeconds + timerSeconds
                                )
                            )
                            runningTaskId = null
                            isTimerRunning = false
                            timerSeconds = 0
                        }
                    )
                }
            }
            
            Text(
                text = "Tasks for ${formatDisplayDate(selectedDate)}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // --- 3. Task Items List ---
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EventNote,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No tasks planned.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hit the '+' button below to schedule tasks for " + formatDisplayDate(selectedDate) + "!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            isFocused = runningTaskId == task.id,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            onStartFocus = {
                                runningTaskId = task.id
                                timerSeconds = 0L
                                isTimerRunning = true
                            }
                        )
                    }
                }
            }
        }
    }

    // --- 4. Add Task Modal Sheet Dialogue ---
    if (showAddTaskDialog) {
        AddTaskDialog(
            selectedDate = selectedDate,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, prio, cat, date, recur, recurType, reminder ->
                viewModel.addTask(title, desc, prio, cat, date, recur, recurType, reminder)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun WeekCalendarStrip(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val daySdf = SimpleDateFormat("E", Locale.getDefault())
    val dateNumSdf = SimpleDateFormat("dd", Locale.getDefault())

    val calendarList = remember {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3) // Starts 3 days ago to establish sliding context
        repeat(7) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(calendarList) { date ->
            val dateStr = sdf.format(date)
            val isSelected = dateStr == selectedDate
            
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(72.dp) // meets critical 48dp ergonomics guidelines
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                )
                            )
                        }
                    )
                    .clickable { onDateSelected(dateStr) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = daySdf.format(date).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = dateNumSdf.format(date),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    isFocused: Boolean,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit,
    onStartFocus: () -> Unit
) {
    val cardColor = if (task.isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val shadowElevation = if (task.isCompleted) 0.dp else 4.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(shadowElevation, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Task Checkbox Circle
                IconButton(
                    onClick = onToggleCompletion,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = "Toggle Complete",
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Priority Badge
                        val priorityColor = when (task.priority) {
                            "High" -> Color(0xFFDA1E28)
                            "Medium" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.secondary
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(priorityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.priority.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = priorityColor
                            )
                        }

                        // Category label
                        Text(
                            text = task.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        // XP Reward Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⭐ +${task.xpReward} XP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Recurring Indicators
                        if (task.isRecurring) {
                            Icon(
                                imageVector = Icons.Filled.Autorenew,
                                contentDescription = "Recurring",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.description.isNotEmpty()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete task",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            // Time Tracking & Info Footer Action Row
            if (!task.isCompleted) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Time recorded
                        if (task.timeSpentSeconds > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = "Time spent",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = formatDuration(task.timeSpentSeconds),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Alarm Reminder time
                        if (task.reminderTime != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Reminder",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = task.reminderTime,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // Focus Button
                    Button(
                        onClick = onStartFocus,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Focus", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FocusTimerBanner(
    taskTitle: String,
    elapsedSeconds: Long,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FOCUS TIMER ACTIVE 🎧",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = taskTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Timer: ${formatDuration(elapsedSeconds)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onToggle,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Toggle Timer"
                    )
                }

                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    selectedDate: String,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        desc: String,
        priority: String,
        category: String,
        date: String,
        isRecurring: Boolean,
        recurrenceType: String,
        reminderTime: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var category by remember { mutableStateOf("Work") }
    var taskDate by remember { mutableStateOf(selectedDate) }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceType by remember { mutableStateOf("Daily") }
    var enableReminder by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableStateOf(9) }
    var reminderMinute by remember { mutableStateOf(0) }

    var isTitleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Insert New Task",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotEmpty()) isTitleError = false
                    },
                    label = { Text("Task Title *") },
                    isError = isTitleError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                )
                if (isTitleError) {
                    Text(
                        text = "Task title is strictly required.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }

                // Description Input
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Short Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category and Priority Selector Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category select Spinner
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val cats = listOf("Work", "Personal", "Study", "Health", "Finance", "Other")
                        var expandedByCat by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedByCat = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(category, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(
                                expanded = expandedByCat,
                                onDismissRequest = { expandedByCat = false }
                            ) {
                                cats.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            expandedByCat = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Priority Select Spinner
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Priority", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        var expandedByPrio by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedByPrio = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(priority)
                            }
                            DropdownMenu(
                                expanded = expandedByPrio,
                                onDismissRequest = { expandedByPrio = false }
                            ) {
                                listOf("Low", "Medium", "High").forEach { prio ->
                                    DropdownMenuItem(
                                        text = { Text(prio) },
                                        onClick = {
                                            priority = prio
                                            expandedByPrio = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Date Input Input
                OutlinedTextField(
                    value = taskDate,
                    onValueChange = { taskDate = it },
                    label = { Text("Due Date (yyyy-MM-dd)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Recurrence Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it }
                        )
                        Text("Recurring Task", fontSize = 13.sp)
                    }

                    if (isRecurring) {
                        var expandedByRt by remember { mutableStateOf(false) }
                        Box {
                            Button(
                                onClick = { expandedByRt = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(recurrenceType, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            DropdownMenu(
                                expanded = expandedByRt,
                                onDismissRequest = { expandedByRt = false }
                            ) {
                                listOf("Daily", "Weekly").forEach { rt ->
                                    DropdownMenuItem(
                                        text = { Text(rt) },
                                        onClick = {
                                            recurrenceType = rt
                                            expandedByRt = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Reminder Hours Spinner
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = enableReminder,
                            onCheckedChange = { enableReminder = it }
                        )
                        Text("Set Reminder Time", fontSize = 13.sp)
                    }
                    if (enableReminder) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Trigger at: ", fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = { reminderHour = (reminderHour + 1) % 24 }) {
                                    Text(String.format("%02d", reminderHour))
                                }
                                Text(" : ")
                                OutlinedButton(onClick = { reminderMinute = (reminderMinute + 5) % 60 }) {
                                    Text(String.format("%02d", reminderMinute))
                                }
                            }
                        }
                    }
                }

                // Buttons footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (title.isEmpty()) {
                                isTitleError = true
                            } else {
                                val finalReminder = if (enableReminder) String.format("%02d:%02d", reminderHour, reminderMinute) else null
                                onConfirm(
                                    title,
                                    desc,
                                    priority,
                                    category,
                                    taskDate,
                                    isRecurring,
                                    recurrenceType,
                                    finalReminder
                                )
                            }
                        },
                        modifier = Modifier.testTag("task_confirm_btn")
                    ) {
                        Text("Schedule")
                    }
                }
            }
        }
    }
}

// Format Duration Helpers
fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

// Display Month Format
fun formatDisplayDate(dateStr: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = format.parse(dateStr) ?: return dateStr
        val displayFormat = SimpleDateFormat("E, MMM d", Locale.getDefault())
        displayFormat.format(date)
    } catch (e: Exception) {
        dateStr
    }
}
