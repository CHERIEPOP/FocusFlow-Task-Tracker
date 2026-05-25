package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TaskRepository
import com.example.data.api.GeminiServiceClient
import com.example.data.local.AppDatabase
import com.example.data.local.Task
import com.example.data.local.User
import com.example.ui.util.LocalNotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(AppDatabase.getDatabase(application).taskDao())
    
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateString: String = sdf.format(Date())
    
    private val _selectedDate = MutableStateFlow(todayDateString)
    val selectedDate = _selectedDate.asStateFlow()
    
    private val _currentUserId = MutableStateFlow<Int?>(null)
    val currentUserId = _currentUserId.asStateFlow()
    
    val currentUser: StateFlow<User?> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(null)
        else repository.getUserFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn = _isUserLoggedIn.asStateFlow()
    
    val tasksForSelectedDate: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        repository.getTasksByDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    private val _aiCoachAdvice = MutableStateFlow<String?>(null)
    val aiCoachAdvice = _aiCoachAdvice.asStateFlow()
    
    private val _isAiCoachLoading = MutableStateFlow(false)
    val isAiCoachLoading = _isAiCoachLoading.asStateFlow()

    private val _showLevelUpDialog = MutableStateFlow<Int?>(null)
    val showLevelUpDialog = _showLevelUpDialog.asStateFlow()

    fun dismissLevelUpDialog() {
        _showLevelUpDialog.value = null
    }

    init {
        viewModelScope.launch {
            try {
                val guestEmail = "guest@dailytasktracker.com"
                val existing = repository.getUserByEmail(guestEmail)
                if (existing == null) {
                    val newGuest = User(
                        name = "Jane Doe",
                        email = guestEmail,
                        passwordHash = "password",
                        dailyGoal = 3,
                        streak = 2,
                        lastCompletionDate = null,
                        xp = 40, // Start with some starter XP for Jane Doe
                        level = 1
                    )
                    val newId = repository.insertUser(newGuest)
                    _currentUserId.value = newId
                } else {
                    _currentUserId.value = existing.id
                }
                _isUserLoggedIn.value = true
                
                val currentTasks = repository.getTasksByDate(todayDateString).first()
                if (currentTasks.isEmpty()) {
                    seedExampleTasks()
                }
            } catch (e: Throwable) {
                Log.e("TaskViewModel", "Error in init block for database seeding", e)
            }
        }
    }

    private suspend fun seedExampleTasks() {
        try {
            val exampleTasks = listOf(
                Task(
                    title = "Study System Architecture",
                    description = "Read about microservices scaling, database indexes, and caching strategies.",
                    priority = "High",
                    status = "In-Progress",
                    category = "Study",
                    date = todayDateString,
                    isRecurring = false,
                    reminderTime = "09:00",
                    xpReward = 100
                ),
                Task(
                    title = "Design Mockup Assets",
                    description = "Iterate on premium dark visual themes, spacious layouts, and custom card elevations.",
                    priority = "Medium",
                    status = "Completed",
                    category = "Work",
                    date = todayDateString,
                    isRecurring = true,
                    recurrenceType = "Daily",
                    isCompleted = true,
                    reminderTime = "14:00",
                    xpReward = 50
                ),
                Task(
                    title = "30-Min Cardio Booster",
                    description = "Outdoor run or high-intensity interval session.",
                    priority = "Low",
                    status = "Pending",
                    category = "Health",
                    date = todayDateString,
                    isRecurring = true,
                    recurrenceType = "Weekly",
                    reminderTime = "18:30",
                    xpReward = 25
                )
            )
            for (task in exampleTasks) {
                repository.insertTask(task)
            }
        } catch (e: Throwable) {
            Log.e("TaskViewModel", "Error seeding tasks", e)
        }
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun addTask(
        title: String,
        description: String,
        priority: String,
        category: String,
        date: String,
        isRecurring: Boolean,
        recurrenceType: String,
        reminderTime: String? = null
    ) {
        viewModelScope.launch {
            try {
                val xpRewardVal = when (priority) {
                    "High" -> 100
                    "Medium" -> 50
                    else -> 25
                }
                val newTask = Task(
                    title = title,
                    description = description,
                    priority = priority,
                    category = category,
                    date = date,
                    isRecurring = isRecurring,
                    recurrenceType = recurrenceType,
                    reminderTime = reminderTime,
                    xpReward = xpRewardVal
                )
                repository.insertTask(newTask)
                
                LocalNotificationHelper.showNotification(
                    getApplication(),
                    "Task Scheduled",
                    "'$title' has been added to your schedule (+${xpRewardVal} XP reward available)."
                )
            } catch (e: Throwable) {
                Log.e("TaskViewModel", "Error adding task", e)
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.updateTask(task)
                if (task.isCompleted) {
                    checkAndUpdateStreak()
                }
            } catch (e: Throwable) {
                Log.e("TaskViewModel", "Error updating task", e)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                repository.deleteTask(task)
            } catch (e: Throwable) {
                Log.e("TaskViewModel", "Error deleting task", e)
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            try {
                val isCompleted = !task.isCompleted
                val updated = task.copy(
                    isCompleted = isCompleted,
                    status = if (isCompleted) "Completed" else "In-Progress"
                )
                repository.updateTask(updated)
                
                // Perform gamification XP update
                awardXpForTask(updated, isCompleted)

                if (updated.isCompleted) {
                    LocalNotificationHelper.showNotification(
                        getApplication(),
                        "Task Completed! 🎉",
                        "Fantastic job on finishing '${task.title}'! +${updated.xpReward} XP earned!"
                    )
                    checkAndUpdateStreak()
                }
            } catch (e: Throwable) {
                Log.e("TaskViewModel", "Error toggling task completion", e)
            }
        }
    }

    private suspend fun awardXpForTask(task: Task, isCompleted: Boolean) {
        try {
            val userItem = currentUser.value ?: return
            var currentLvl = userItem.level
            var currentXp = userItem.xp
            val xpRewardVal = if (task.xpReward > 0) task.xpReward else {
                when (task.priority) {
                    "High" -> 100
                    "Medium" -> 50
                    else -> 25
                }
            }
            val xpDiff = if (isCompleted) xpRewardVal else -xpRewardVal
            var newXp = currentXp + xpDiff
            
            var leveledUp = false
            if (xpDiff > 0) {
                while (newXp >= currentLvl * 100) {
                    newXp -= currentLvl * 100
                    currentLvl++
                    leveledUp = true
                }
                if (leveledUp) {
                    _showLevelUpDialog.value = currentLvl
                }
            } else {
                while (newXp < 0 && currentLvl > 1) {
                    currentLvl--
                    newXp += currentLvl * 100
                }
                if (newXp < 0) newXp = 0
            }
            
            repository.updateUser(
                userItem.copy(
                    xp = newXp,
                    level = currentLvl
                )
            )
        } catch (e: Throwable) {
            Log.e("TaskViewModel", "Error awarding XP for task", e)
        }
    }

    private suspend fun checkAndUpdateStreak() {
        try {
            val userItem = currentUser.value ?: return
            val currentStreak = userItem.streak
            val lastDate = userItem.lastCompletionDate
            
            if (lastDate != todayDateString) {
                val updatedStreak = if (lastDate == null) {
                    1
                } else {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayStr = sdf.format(calendar.time)
                    
                    if (lastDate == yesterdayStr) {
                        currentStreak + 1
                    } else if (lastDate == todayDateString) {
                        currentStreak
                    } else {
                        1
                    }
                }
                
                repository.updateUser(
                    userItem.copy(
                        streak = updatedStreak,
                        lastCompletionDate = todayDateString
                    )
                )
            }
        } catch (e: Throwable) {
            Log.e("TaskViewModel", "Error checking/updating streak", e)
        }
    }

    fun loginUser(email: String, name: String) {
        viewModelScope.launch {
            try {
                val existing = repository.getUserByEmail(email)
                if (existing != null) {
                    _currentUserId.value = existing.id
                } else {
                    val newId = repository.insertUser(
                        User(
                            name = name,
                            email = email,
                            passwordHash = "password_hash"
                        )
                    )
                    _currentUserId.value = newId
                }
                _isUserLoggedIn.value = true
            } catch (e: Throwable) {
                Log.e("TaskViewModel", "Error logging in user", e)
            }
        }
    }

    fun logoutUser() {
        _currentUserId.value = null
        _isUserLoggedIn.value = false
        _aiCoachAdvice.value = null
    }

    fun updateDailyGoal(goalCount: Int) {
        viewModelScope.launch {
            try {
                val userItem = currentUser.value ?: return@launch
                repository.updateUser(userItem.copy(dailyGoal = goalCount))
            } catch (e: Throwable) {
                Log.e("TaskViewModel", "Error updating daily goal", e)
            }
        }
    }

    fun fetchAiCoachSuggestions() {
        viewModelScope.launch {
            _isAiCoachLoading.value = true
            _aiCoachAdvice.value = null
            
            val userItem = currentUser.value
            val userName = userItem?.name ?: "Guest"
            val streakVal = userItem?.streak ?: 0
            
            val currentTasks = tasksForSelectedDate.value
            val compCount = currentTasks.count { it.isCompleted }
            val totCount = currentTasks.size
            
            val summary = if (currentTasks.isEmpty()) {
                "No tasks scheduled on this day."
            } else {
                currentTasks.joinToString("\n") { t ->
                    "- ${t.title} [Priority: ${t.priority}, Category: ${t.category}, Status: ${t.status}, Recurring: ${t.recurrenceType}]"
                }
            }
            
            val response = GeminiServiceClient.getDailyAiSuggestions(
                userName = userName,
                tasksSummary = summary,
                completedCount = compCount,
                totalCount = totCount,
                streakCount = streakVal
            )
            
            _aiCoachAdvice.value = response
            _isAiCoachLoading.value = false
        }
    }

    fun exportBackupJson(): String {
        val tasks = allTasks.value
        val user = currentUser.value
        
        return """
            {
              "backupDate": "$todayDateString",
              "user": {
                 "name": "${user?.name ?: "Guest"}",
                 "email": "${user?.email ?: ""}",
                 "streak": ${user?.streak ?: 0},
                 "goal": ${user?.dailyGoal ?: 5}
              },
              "tasksCount": ${tasks.size},
              "tasks": [
                ${tasks.joinToString(",") { t ->
                    """{
                      "title": "${t.title.replace("\"", "\\\"")}",
                      "desc": "${t.description.replace("\"", "\\\"")}",
                      "priority": "${t.priority}",
                      "status": "${t.status}",
                      "completed": ${t.isCompleted},
                      "category": "${t.category}",
                      "date": "${t.date}"
                    }"""
                }}
              ]
            }
        """.trimIndent()
    }
}
