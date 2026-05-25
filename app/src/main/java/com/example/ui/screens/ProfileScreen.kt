package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.TaskViewModel

data class ProfileBadge(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isUnlocked: Boolean,
    val color: Color
)

@Composable
fun ProfileScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val completedCount = allTasks.count { it.isCompleted }
    val userLevel = currentUser?.level ?: 1
    val userStreak = currentUser?.streak ?: 0

    val badges = remember(userLevel, userStreak, completedCount) {
        listOf(
            ProfileBadge(
                title = "Launchpad Starter",
                description = "Earned for beginning your journey & reaching Level 2",
                icon = Icons.Filled.DirectionsRun,
                isUnlocked = userLevel >= 2,
                color = Color(0xFF4CAF50)
            ),
            ProfileBadge(
                title = "Streak Fire",
                description = "Kept a daily schedule streak of 3+ consecutive days",
                icon = Icons.Filled.LocalFireDepartment,
                isUnlocked = userStreak >= 3,
                color = Color(0xFFFF5722)
            ),
            ProfileBadge(
                title = "Task Crusher",
                description = "Marked 8+ tasks completed successfully",
                icon = Icons.Filled.EmojiEvents,
                isUnlocked = completedCount >= 8,
                color = Color(0xFFFFC107)
            ),
            ProfileBadge(
                title = "Elite Planner",
                description = "Achieved Level 5 productivity status",
                icon = Icons.Filled.WorkspacePremium,
                isUnlocked = userLevel >= 5,
                color = Color(0xFF00BCD4)
            )
        )
    }
    
    var showLoginDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupString by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("profile_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. Header Profile Box ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (currentUser?.name ?: "U").take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentUser?.name ?: "Guest Champion",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentUser?.email ?: "local.offline@dailytasktracker.com",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Button(
                    onClick = { showLoginDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("profile_switch_btn")
                ) {
                    Icon(imageVector = Icons.Filled.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch Profile / Login")
                }
            }
        }

        // --- 1.5 Badges & Gamified Achievements ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp))
                .testTag("gamified_badges_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unlocked Achievements 🏆",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    val unlockedCount = badges.count { it.isUnlocked }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$unlockedCount / ${badges.size} Badges",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Text(
                    text = "Complete high-priority tasks and maintain your streak to unlock unique badges and build up your Level stats!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
                
                // Badges list
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    badges.forEach { badge ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (badge.isUnlocked) badge.color.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Badge Icon Circle
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (badge.isUnlocked) badge.color.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = badge.icon,
                                    contentDescription = badge.title,
                                    tint = if (badge.isUnlocked) badge.color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = badge.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    if (badge.isUnlocked) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(badge.color.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "UNLOCKED",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = badge.color
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = badge.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Target Goal Settings ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Goal Configurations🎯",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Target Completed Tasks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Target count of daily items to accomplish.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val currentTarget = currentUser?.dailyGoal ?: 3
                        
                        IconButton(
                            onClick = { if (currentTarget > 1) viewModel.updateDailyGoal(currentTarget - 1) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrement", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "$currentTarget",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(20.dp),
                            textAlign = TextAlign.Center
                        )

                        IconButton(
                            onClick = { if (currentTarget < 20) viewModel.updateDailyGoal(currentTarget + 1) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Increment", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // --- 3. Synchronisation & Offline Backups ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Backup & Cloud Sync ☁️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "We use secure sandboxed SQLite structures to preserve all statistics locally. You can export clean JSON dumps of your entire historic schedule to sync manually across screens or platforms.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = {
                        backupString = viewModel.exportBackupJson()
                        showBackupDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("export_backup_btn")
                ) {
                    Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Database State Draft")
                }
            }
        }
    }

    // Modal dialogue Login Switcher
    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { showLoginDialog = false },
            onConfirm = { email, name ->
                viewModel.loginUser(email, name)
                showLoginDialog = false
            }
        )
    }

    // Backup Serialized State Exporter display
    if (showBackupDialog) {
        Dialog(onDismissRequest = { showBackupDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "JSON Backup File Config",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    OutlinedTextField(
                        value = backupString,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                    )

                    Button(
                        onClick = { showBackupDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onConfirm: (email: String, name: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sign In / Switch Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        isError = false
                    },
                    label = { Text("Email Address *") },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth().testTag("profile_email_input")
                )
                if (isError) {
                    Text(text = "Valid Email is strictly required.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

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
                            if (email.contains("@") && name.isNotEmpty()) {
                                onConfirm(email, name)
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier.testTag("dialog_login_confirm")
                    ) {
                        Text("Sign In")
                    }
                }
            }
        }
    }
}
