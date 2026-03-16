package com.example.offermatrix.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
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
import com.example.offermatrix.ui.screens.analysis.AnalysisPage
import com.example.offermatrix.ui.screens.home.HomePage
import com.example.offermatrix.ui.screens.interview.InterviewPage
import com.example.offermatrix.ui.screens.login.LoginPage
import com.example.offermatrix.ui.screens.my.MyPage
import com.example.offermatrix.ui.screens.records.AllRecordsPage
import com.example.offermatrix.ui.screens.register.RegisterPage
import com.example.offermatrix.ui.screens.roles.MaterialsManagementPage
import com.example.offermatrix.ui.screens.roles.ResumeManagementPage
import com.example.offermatrix.ui.screens.roles.RolesPage
import com.example.offermatrix.ui.screens.select_role.SelectRolePage
import com.example.offermatrix.ui.screens.training.GeneratedQuestionsListPage
import com.example.offermatrix.ui.screens.training.LearningPage
import com.example.offermatrix.ui.screens.training.MyFavoritesPage
import com.example.offermatrix.ui.screens.training.TrainingPage
import com.example.offermatrix.ui.screens.training.QuestionDetailPage
import java.net.URLDecoder
import java.net.URLEncoder
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import com.example.offermatrix.ui.theme.PrimaryBlue
import com.example.offermatrix.ui.theme.TextGray
import java.nio.charset.StandardCharsets

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import android.widget.Toast
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.network.LoginRequest
import com.example.offermatrix.network.RegisterRequest
import com.example.offermatrix.network.UserSession
import com.example.offermatrix.network.LoginResponse

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginPage(
                onLoginClick = { username, password ->
                    scope.launch {
                        try {
                            val response = RetrofitClient.apiService.login(LoginRequest(username, password))
                            Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                            
                            // Store session data
                            UserSession.token = response.access_token
                            UserSession.userId = response.user_id
                            UserSession.username = response.username
                            UserSession.avatar = response.avatar
                            UserSession.roles = response.roles
                            if (response.roles.isNotEmpty()) {
                                UserSession.currentRole = response.roles[0]
                            }

                            if (response.roles.isEmpty()) {
                                navController.navigate("select_role") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "登录失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onRegisterClick = { navController.navigate("register") },
                onGuestClick = { 
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            RegisterPage(
                onRegisterClick = { username, password ->
                    scope.launch {
                        try {
                            val response = RetrofitClient.apiService.register(RegisterRequest(username, password))
                            if (response.isSuccessful && response.body()?.success == true) {
                                Toast.makeText(context, "注册成功，正在自动登录...", Toast.LENGTH_SHORT).show()
                                // Auto login
                                try {
                                    val loginResponse = RetrofitClient.apiService.login(LoginRequest(username, password))
                                    UserSession.token = loginResponse.access_token
                                    UserSession.userId = loginResponse.user_id
                                    UserSession.username = loginResponse.username
                                    UserSession.avatar = loginResponse.avatar
                                    UserSession.roles = loginResponse.roles
                                    
                                    navController.navigate("select_role") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "自动登录失败，请手动登录", Toast.LENGTH_LONG).show()
                                    navController.navigate("login")
                                }
                            } else {
                                Toast.makeText(context, "注册失败: ${response.message()}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable("select_role") {
            SelectRolePage(
                onNextClick = {
                    navController.navigate("main") {
                        popUpTo("select_role") { inclusive = true }
                    }
                },
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable("main") { 
            MainAppScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            ) 
        }
    }
}

@Composable
fun MainAppScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        BottomNavItem("主页", Icons.Default.Home, "home"),
        BottomNavItem("特训", Icons.Default.School, "training"),
        BottomNavItem("角色", Icons.Default.Work, "roles"),
        BottomNavItem("我的", Icons.Default.Person, "my")
    )
    val screensWithBottomNav = bottomNavItems.map { it.route }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in screensWithBottomNav

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    shadowElevation = 16.dp, // Add shadow for "floating" feel or separation
                    color = Color.White
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        contentColor = PrimaryBlue,
                        tonalElevation = 0.dp // Remove default tonal elevation to keep it clean white
                    ) {
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = { 
                                    Icon(
                                        screen.icon, 
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    ) 
                                },
                                label = { 
                                    Text(
                                        screen.label, 
                                        fontSize = 10.sp, 
                                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    ) 
                                },
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryBlue,
                                    selectedTextColor = PrimaryBlue,
                                    indicatorColor = Color(0xFFE3F2FD), // Light blue indicator
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "home", Modifier.padding(innerPadding)) {
            composable("home") { 
                HomePage(
                    onStartInterview = { navController.navigate("interview") }, 
                    onSeeAllRecords = { navController.navigate("all_records") },
                    onRecordClick = { id -> navController.navigate("analysis/$id") }
                ) 
            }
            composable("training") { 
                TrainingPage(
                    onSeeAllGeneratedQuestions = { navController.navigate("generated_questions_list") },
                    onSeeAllFavorites = { navController.navigate("my_favorites") },
                    onQuestionSetClick = { id -> navController.navigate("learning/$id") },
                    onFavoriteClick = { id -> navController.navigate("question_detail/$id") },
                    onNavigateToLearning = { id -> navController.navigate("learning/$id") }
                )
            }
            composable("roles") {
                RolesPage(
                    onAddRoleClick = { navController.navigate("select_role_nested") }, // Use a different route for nested navigation
                    onResumeClick = { roleName -> 
                        val encodedRoleName = URLEncoder.encode(roleName, StandardCharsets.UTF_8.toString())
                        navController.navigate("resume_management/$encodedRoleName") 
                    },
                    onMaterialsClick = { roleName -> 
                        val encodedRoleName = URLEncoder.encode(roleName, StandardCharsets.UTF_8.toString())
                        navController.navigate("materials_management/$encodedRoleName") 
                    }
                )
            }
            composable("my") { MyPage(onLogout = onLogout) }
            composable("interview") { InterviewPage(navController = navController) }
            composable("all_records") { AllRecordsPage(onRecordClick = { id -> navController.navigate("analysis/$id") }, onBackPressed = { navController.popBackStack() }) }
            composable("analysis/{recordId}") { backStackEntry ->
                val recordId = backStackEntry.arguments?.getString("recordId")?.toIntOrNull() ?: 0
                AnalysisPage(recordId = recordId, onBackPressed = { navController.popBackStack() })
            }
            composable("generated_questions_list") { 
                GeneratedQuestionsListPage(
                    onQuestionSetClick = { id -> navController.navigate("learning/$id") },
                    onBackPressed = { navController.popBackStack() }
                )
            }
            composable("my_favorites") { 
                MyFavoritesPage(
                    onBackPressed = { navController.popBackStack() },
                    onFavoriteClick = { id -> navController.navigate("question_detail/$id") }
                ) 
            }
            composable("learning/{questionSetId}") { backStackEntry ->
                val questionSetId = backStackEntry.arguments?.getString("questionSetId") ?: ""
                LearningPage(questionSetId = questionSetId, onBackPressed = { navController.popBackStack() })
            }
             composable("resume_management/{roleName}") { backStackEntry ->
                val roleName = backStackEntry.arguments?.getString("roleName")?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) } ?: ""
                ResumeManagementPage(roleName = roleName, onBackPressed = { navController.popBackStack() })
            }
            composable("materials_management/{roleName}") { backStackEntry ->
                val roleName = backStackEntry.arguments?.getString("roleName")?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) } ?: ""
                MaterialsManagementPage(roleName = roleName, onBackPressed = { navController.popBackStack() })
            }
             composable("question_detail/{questionId}") { backStackEntry ->
                val questionId = backStackEntry.arguments?.getString("questionId") ?: ""
                QuestionDetailPage(questionId = questionId, onBackPressed = { navController.popBackStack() })
            }
             composable("select_role_nested") {
                SelectRolePage(onNextClick = { navController.popBackStack() }, onBackPressed = { navController.popBackStack() })
            }
        }
    }
}