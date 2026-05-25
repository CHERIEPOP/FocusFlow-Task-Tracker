package com.example.data

import com.example.data.local.Task
import com.example.data.local.TaskDao
import com.example.data.local.User
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    // --- Tasks ---
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksByDate(date: String): Flow<List<Task>> = taskDao.getTasksByDate(date)

    fun getTasksInRange(startDate: String, endDate: String): Flow<List<Task>> = taskDao.getTasksInRange(startDate, endDate)

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteAllTasks() = taskDao.deleteAllTasks()

    // --- Users ---
    fun getUserFlow(userId: Int): Flow<User?> = taskDao.getUserByIdFlow(userId)

    suspend fun getUserById(userId: Int): User? = taskDao.getUserById(userId)

    suspend fun getUserByEmail(email: String): User? = taskDao.getUserByEmail(email)

    suspend fun insertUser(user: User): Int = taskDao.insertUser(user).toInt()

    suspend fun updateUser(user: User) = taskDao.updateUser(user)
}
