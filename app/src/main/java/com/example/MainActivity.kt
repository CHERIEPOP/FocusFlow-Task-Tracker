package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import com.example.ui.TaskViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

enum class ScreenTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    DASHBOARD("Status", Icons.Filled.Home, Icons.Outlined.Home, "tab_dashboard"),
    PLANNER("Planner", Icons.Filled.DateRange, Icons.Outlined.DateRange, "tab_planner"),
    ANALYTICS("Trends", Icons.Filled.InsertChart, Icons.Outlined.InsertChartOutlined, "tab_analytics"),
    AI_COACH("AI Coach", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "tab_ai_coach"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
}

class MainActivity : ComponentActivity() {
    private val taskViewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
                val showLevelUpDialog by taskViewModel.showLevelUpDialog.collectAsState()

                if (showLevelUpDialog != null) {
                    LevelUpCongratulationsDialog(
                        level = showLevelUpDialog!!,
                        onDismiss = { taskViewModel.dismissLevelUpDialog() }
                    )
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            ScreenTab.values().forEach { tab ->
                                val isSelected = currentTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                            },
                            label = "MainScreenCrossfade"
                        ) { targetTab ->
                            when (targetTab) {
                                ScreenTab.DASHBOARD -> DashboardScreen(
                                    viewModel = taskViewModel,
                                    onNavigateToPlanner = { currentTab = ScreenTab.PLANNER }
                                )
                                ScreenTab.PLANNER -> PlannerScreen(viewModel = taskViewModel)
                                ScreenTab.ANALYTICS -> AnalyticsScreen(viewModel = taskViewModel)
                                ScreenTab.AI_COACH -> AiCoachScreen(viewModel = taskViewModel)
                                ScreenTab.PROFILE -> ProfileScreen(viewModel = taskViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LevelUpCongratulationsDialog(
    level: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("level_up_dismiss_btn")
            ) {
                Text("Awesome! Onward! 🚀")
            }
        },
        title = {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD700), // Gold
                                    Color(0xFFFFA500)  // Orange
                                )
                            )
                        ),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Trophy/Level Up",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "LEVEL UP! 🎉",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Congratulations, Champion!",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = "You've ascended to Level $level!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = "Your daily consistency is paying off. Keep checking off tasks to claim new badges and dominate your leveling progress!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 12.dp
    )
}
