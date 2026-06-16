package com.oncewind.sms2Email.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oncewind.sms2Email.ui.screen.HelpScreen
import com.oncewind.sms2Email.ui.screen.LogScreen
import com.oncewind.sms2Email.ui.screen.SettingsScreen
import com.oncewind.sms2Email.ui.screen.StatusScreen
import com.oncewind.sms2Email.viewmodel.LogViewModel
import com.oncewind.sms2Email.viewmodel.SettingsViewModel
import com.oncewind.sms2Email.viewmodel.StatusViewModel

/**
 * 底部导航项定 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("status", "状态", Icons.Default.Home),
    BottomNavItem("settings", "配置", Icons.Default.Settings),
    BottomNavItem("logs", "日志", Icons.Default.List),
    BottomNavItem("help", "帮助", Icons.Default.Help)
)

/**
 * APP 主导航结 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel()
    val statusViewModel: StatusViewModel = viewModel()
    val logViewModel: LogViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "status",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("status") {
                StatusScreen(statusViewModel)
            }
            composable("settings") {
                SettingsScreen(settingsViewModel)
            }
            composable("logs") {
                LogScreen(statusViewModel, logViewModel)
            }
            composable("help") {
                HelpScreen()
            }
        }
    }
}