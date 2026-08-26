package com.example.myfast.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myfast.data.ThemeRepository
import com.example.myfast.presentation.screens.HomeScreen
import com.example.myfast.presentation.screens.HistoryScreen
import com.example.myfast.presentation.screens.StatsScreen
import com.example.myfast.presentation.screens.SettingsScreen
import com.example.myfast.ui.theme.MyFastTheme

sealed class NavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : NavItem("home", "Home", Icons.Default.Home)
    object History : NavItem("history", "History", Icons.Default.Favorite)
    object Dashboard : NavItem("dashboard", "Dashboard", Icons.Default.Star)
    object Settings : NavItem("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf("home") }
    
    val context = LocalContext.current
    val themeRepository = remember { ThemeRepository(context) }
    var isDarkMode by remember { mutableStateOf(themeRepository.isDarkMode()) }

    MyFastTheme(darkTheme = isDarkMode) {
        Scaffold(
            topBar = {
                if (currentRoute != "home") {
                    TopAppBar(
                        title = { Text("") },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    navController.navigate("home") {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    val navItems = listOf(
                        NavItem.Home,
                        NavItem.History,
                        NavItem.Dashboard,
                        NavItem.Settings
                    )

                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                currentRoute = item.route
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
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
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("home") {
                    currentRoute = "home"
                    HomeScreen()
                }
                composable("history") {
                    currentRoute = "history"
                    HistoryScreen()
                }
                composable("dashboard") {
                    currentRoute = "dashboard"
                    StatsScreen()
                }
                composable("settings") {
                    currentRoute = "settings"
                    SettingsScreen(
                        isDarkMode = isDarkMode,
                        onDarkModeChange = { newMode ->
                            isDarkMode = newMode
                            themeRepository.setDarkMode(newMode)
                        }
                    )
                }
            }
        }
    }
}
