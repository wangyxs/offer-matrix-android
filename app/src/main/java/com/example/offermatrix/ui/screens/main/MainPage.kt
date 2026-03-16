package com.example.offermatrix.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.offermatrix.ui.screens.main.home.HomePage
import com.example.offermatrix.ui.screens.main.profile.ProfilePage
import com.example.offermatrix.ui.screens.main.roles.RolesPage
import com.example.offermatrix.ui.screens.main.training.TrainingPage

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Home : Screen("home", "主页", { Icon(Icons.Filled.Home, contentDescription = null) })
    object Training : Screen("training", "特训", { Icon(Icons.Filled.Star, contentDescription = null) })
    object Roles : Screen("roles", "角色", { Icon(Icons.Filled.Person, contentDescription = null) })
    object Profile : Screen("profile", "我的", { Icon(Icons.Filled.AccountCircle, contentDescription = null) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home,
        Screen.Training,
        Screen.Roles,
        Screen.Profile
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { screen.icon() },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
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
        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
            composable(Screen.Home.route) { HomePage() }
            composable(Screen.Training.route) { TrainingPage() }
            composable(Screen.Roles.route) { RolesPage() }
            composable(Screen.Profile.route) { ProfilePage() }
        }
    }
}
