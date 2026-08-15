package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.CommandLogScreen
import com.example.ui.screens.ConnectionScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AdbManagerApp()
            }
        }
    }
}

@Composable
fun AdbManagerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val screens = listOf(
        Screen.Connection,
        Screen.Terminal,
        Screen.Backup
    )

    BoxWithConstraints {
        val isTablet = maxWidth > 600.dp

        if (isTablet) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    Spacer(Modifier.weight(1f))
                    screens.forEach { screen ->
                        NavigationRailItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
                Scaffold(
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    NavHostContainer(navController, Modifier.padding(innerPadding))
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        screens.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHostContainer(navController, Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun NavHostContainer(navController: androidx.navigation.NavHostController, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Connection.route,
        modifier = modifier
    ) {
        composable(Screen.Connection.route) { ConnectionScreen() }
        composable(Screen.Terminal.route) { CommandLogScreen() }
        composable(Screen.Backup.route) { BackupScreen() }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Connection : Screen("connection", "Device", Icons.Default.Usb)
    object Terminal : Screen("terminal", "Terminal", Icons.Default.Terminal)
    object Backup : Screen("backup", "Backup", Icons.Default.Save)
}
