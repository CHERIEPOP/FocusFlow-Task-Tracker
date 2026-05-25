package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val dailyGoal: Int = 3,
    val streak: Int = 0,
    val lastCompletionDate: String? = null, // "yyyy-MM-dd"
    val xp: Int = 0,
    val level: Int = 1
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val priority: String = "Medium", // "Low", "Medium", "High"
    val status: String = "Pending", // "Pending", "In-Progress", "Completed"
    val category: String = "General", // "Work", "Personal", "Study", "Health", "Finance"
    val date: String, // "yyyy-MM-dd"
    val isRecurring: Boolean = false,
    val recurrenceType: String = "None", // "None", "Daily", "Weekly"
    val isCompleted: Boolean = false,
    val reminderTime: String? = null, // "HH:mm"
    val timeSpentSeconds: Long = 0L,
    val xpReward: Int = 50
)
