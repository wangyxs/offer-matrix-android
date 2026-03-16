package com.example.offermatrix.ui.screens.training

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offermatrix.network.*
import com.example.offermatrix.ui.theme.*
import com.example.offermatrix.ui.viewmodel.TrainingViewModel
import kotlinx.coroutines.launch

@Composable
fun TrainingPage(
    onSeeAllGeneratedQuestions: () -> Unit,
    onSeeAllFavorites: () -> Unit,
    onQuestionSetClick: (String) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    onNavigateToLearning: (Int) -> Unit = {}
) {
    val viewModel: TrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Silent refresh to keep data consistent without wiping UI
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Observe generation status for toasts
    LaunchedEffect(uiState.generationStatus) {
        uiState.generationStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearGenerationStatus()
        }
    }

    Scaffold(containerColor = BackgroundWhite) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            AIGenerationPlatform(
                viewModel = viewModel, 
                onNavigateToLearning = onNavigateToLearning
            )
            Spacer(modifier = Modifier.height(28.dp))
            GeneratedQuestions(
                questionSets = uiState.questionSets,
                onSeeAll = onSeeAllGeneratedQuestions,
                onQuestionSetClick = { id -> onQuestionSetClick(id.toString()) }
            )
            Spacer(modifier = Modifier.height(28.dp))
            MyFavorites(
                favorites = uiState.favorites, 
                onSeeAll = onSeeAllFavorites,
                onFavoriteClick = onFavoriteClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIGenerationPlatform(
    viewModel: TrainingViewModel,
    onNavigateToLearning: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var questionCount by remember { mutableStateOf("5题") }
    var questionStyle by remember { mutableStateOf("中等文本") }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showRoleMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.padding(start = 12.dp))
                Text("AI 定向生成台", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.weight(1f))
                // 角色选择器
                if (uiState.roles.isNotEmpty()) {
                    var showRoleMenu by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF5F7FA))
                                .clickable { showRoleMenu = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = uiState.selectedRole?.role?.name ?: "选择角色",
                                fontSize = 14.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Expand",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showRoleMenu,
                            onDismissRequest = { showRoleMenu = false },
                            modifier = Modifier
                                .background(SurfaceWhite)
                                .widthIn(min = 180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            uiState.roles.forEach { userRole ->
                                val isSelected = uiState.selectedRole?.id == userRole.id
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = userRole.role?.name ?: "未知岗位",
                                                color = if (isSelected) PrimaryBlue else TextDark,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = PrimaryBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSelectedRole(userRole)
                                        showRoleMenu = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SimpleDropdown(
                    title = "题目数量",
                    options = listOf("3题", "5题", "10题"),
                    selected = questionCount,
                    onSelected = { questionCount = it },
                    modifier = Modifier.weight(1f)
                )
                SimpleDropdown(
                    title = "题目风格",
                    options = listOf("短文本", "中等文本", "长文本"),
                    selected = questionStyle,
                    onSelected = { questionStyle = it },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("额外需求描述,例如:重点考察并发编程...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundWhite,
                    unfocusedContainerColor = BackgroundWhite,
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (uiState.selectedRole == null) {
                        Toast.makeText(context, "请先选择角色", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // Show immediate feedback and start background generation
                    Toast.makeText(context, "后台生成中，请稍后查看列表", Toast.LENGTH_LONG).show()
                    
                    // Call ViewModel to generate (no navigation on success)
                    viewModel.generateQuestionSet(
                        questionCount = questionCount,
                        questionStyle = questionStyle,
                        description = description
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("✨ 生成模拟题组", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun GeneratedQuestions(
    questionSets: List<QuestionSetResponse>,
    onSeeAll: () -> Unit,
    onQuestionSetClick: (Int) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("我的题组", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDark)
            TextButton(onClick = onSeeAll) {
                Text("查看全部", color = PrimaryBlue, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
        ) {
            if (questionSets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "暂无题组,立即生成一个题组吧!",
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    questionSets.take(2).forEachIndexed { index, set ->
                        if (index > 0) {
                            Divider(modifier = Modifier.padding(horizontal = 20.dp), color = BackgroundWhite)
                        }
                        val formattedDate = try {
                            val inputFormatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
                            val outputFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            val dateTime = java.time.LocalDateTime.parse(set.created_at, inputFormatter)
                            val zonedDateTime = dateTime.atZone(java.time.ZoneId.of("UTC"))
                                .withZoneSameInstant(java.time.ZoneId.systemDefault())
                            outputFormatter.format(zonedDateTime)
                        } catch (e: Exception) {
                            set.created_at?.replace("T", " ")?.substringBefore(".") ?: ""
                        }
                        ListItem(
                            icon = Icons.Default.Description,
                            title = set.title,
                            subtitle = "${set.question_count}道题目, $formattedDate",
                            details = set.role_name ?: "未知角色",
                            isViewed = set.is_viewed,
                            onClick = { onQuestionSetClick(set.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyFavorites(favorites: List<QuestionDetail>, onSeeAll: () -> Unit, onFavoriteClick: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("我的收藏", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDark)
            TextButton(onClick = onSeeAll) {
                Text("查看全部", color = PrimaryBlue, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
        ) {
            if (favorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "暂无收藏的题目",
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    favorites.take(3).forEachIndexed { index, question ->
                        if (index > 0) {
                            Divider(modifier = Modifier.padding(horizontal = 20.dp), color = BackgroundWhite)
                        }
                        FavoriteItem(
                            title = question.question_text,
                            onClick = { onFavoriteClick(question.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDropdown(title: String, options: List<String>, selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(title, fontSize = 14.sp, color = TextGray, modifier = Modifier.padding(bottom = 8.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = BackgroundWhite,
                    unfocusedContainerColor = BackgroundWhite,
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        onSelected(option)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Composable
fun ListItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, details: String, isViewed: Boolean = true, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Unread Indicator
        if (!isViewed) {
             Box(
                 modifier = Modifier
                     .padding(end = 8.dp)
                     .size(8.dp)
                     .background(PrimaryBlue, androidx.compose.foundation.shape.CircleShape)
             )
        }
        
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = PrimaryBlue)
        Spacer(modifier = Modifier.padding(start = if (!isViewed) 8.dp else 16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextGray)
        }
        Text(details, style = MaterialTheme.typography.bodySmall, color = PrimaryBlue, fontWeight = FontWeight.Medium)
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color(0xFFE0E0E0), modifier = Modifier
            .padding(start = 12.dp)
            .size(14.dp))
    }
}

@Composable
fun FavoriteItem(title: String, onClick: () -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier
            .padding(end = 16.dp)
            .size(22.dp))
        Text(title, modifier = Modifier.weight(1f), color = TextDark)
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(14.dp))
    }
}
