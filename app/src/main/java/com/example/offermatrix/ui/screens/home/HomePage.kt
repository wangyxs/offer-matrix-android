package com.example.offermatrix.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.offermatrix.network.UserSession
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.network.Role
import com.example.offermatrix.network.InterviewRecordResponse
import com.example.offermatrix.ui.viewmodel.InterviewRecordsViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.offermatrix.ui.theme.BackgroundWhite
import com.example.offermatrix.ui.theme.DarkBlue
import com.example.offermatrix.ui.theme.PrimaryBlue
import com.example.offermatrix.ui.theme.SuccessGreen
import com.example.offermatrix.ui.theme.SurfaceWhite
import com.example.offermatrix.ui.theme.TextDark
import com.example.offermatrix.ui.theme.TextGray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    onStartInterview: () -> Unit, 
    onSeeAllRecords: () -> Unit,
    onRecordClick: (Int) -> Unit = {},
    viewModel: InterviewRecordsViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    
    // 监听生命周期，当页面恢复时重新加载数据
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // 重新加载角色数据
                scope.launch {
                    try {
                        val userRoles = RetrofitClient.apiService.getUserRoles(UserSession.userId)
                        UserSession.roles = userRoles.mapNotNull { it.role }
                        if (UserSession.currentRole != null && 
                            UserSession.roles.none { it.id == UserSession.currentRole?.id }) {
                            UserSession.currentRole = UserSession.roles.firstOrNull()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                // 重新加载面试记录
                viewModel.loadRecords()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Scaffold(
        containerColor = BackgroundWhite,
        modifier = Modifier.fillMaxSize()

    ) { innerPadding ->
        
        // Stabilize bottom padding: capture the maximum bottom padding seen (e.g. when Nav Bar is present)
        // and keep it even when the Nav Bar disappears during transitions.
        var maxBottomPadding by remember { mutableStateOf(0.dp) }
        val currentBottomPadding = innerPadding.calculateBottomPadding()
        if (currentBottomPadding > maxBottomPadding) {
            maxBottomPadding = currentBottomPadding
        }
        
        // Use the stable max padding
        val stableBottomPadding = if (currentBottomPadding > maxBottomPadding) currentBottomPadding else maxBottomPadding

        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding(), bottom = 0.dp) // 取消底部 padding 强制
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // 添加滚动支持
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Role Selector
            var showRoleMenu by remember { mutableStateOf(false) }
            var currentRole by remember { mutableStateOf(UserSession.currentRole) }
            
            // 当 UserSession.currentRole 变化时，更新 currentRole
            LaunchedEffect(UserSession.currentRole) {
                currentRole = UserSession.currentRole
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Wrapper Box to anchor the DropdownMenu to the button
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F7FA))
                            .clickable { showRoleMenu = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "当前角色: ",
                            fontSize = 16.sp,
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currentRole?.name ?: "请选择角色",
                            fontSize = 16.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showRoleMenu,
                        onDismissRequest = { showRoleMenu = false },
                        modifier = Modifier
                            .background(SurfaceWhite)
                            .widthIn(min = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        UserSession.roles.forEach { role ->
                            val isSelected = currentRole?.id == role.id
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = role.name,
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
                                    UserSession.currentRole = role
                                    currentRole = role
                                    showRoleMenu = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp)) // Reduced from 40.dp

            // Enhanced Start Interview Button with unified status
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAlpha"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Main Button Box
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(220.dp) // Reduced from 260.dp
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onStartInterview
                        )
                ) {
                    // Single Soft Pulse
                    Box(
                        modifier = Modifier
                            .size(200.dp) // Reduced from 240.dp
                            .scale(pulseScale)
                            .alpha(glowAlpha)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        PrimaryBlue.copy(alpha = 0.3f),
                                        PrimaryBlue.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // Main Button
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier
                            .size(180.dp) // Reduced from 220.dp
                            .shadow(
                                elevation = 16.dp,
                                spotColor = PrimaryBlue.copy(alpha = 0.4f),
                                shape = CircleShape
                            ),
                        color = Color.Transparent
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF29B6F6), // Lighter Blue
                                            PrimaryBlue,       // Main Blue
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    )
                                )
                        ) {
                            // Subtle top highlight
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.15f),
                                                Color.Transparent
                                            ),
                                            center = androidx.compose.ui.geometry.Offset(300f, 50f),
                                            radius = 300f
                                        )
                                    )
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp) // Reduced from 56.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "开始面试",
                                    color = Color.White,
                                    fontSize = 22.sp, // Reduced from 24.sp
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "AI 语音实时对话",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp)) // Reduced from 16.dp

                // Status section integrated
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val dotAlphaStatus by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlphaStatus"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp) // Smaller, cleaner dot
                            .alpha(dotAlphaStatus)
                            .background(SuccessGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "设备检测正常，随时开始",
                        fontSize = 13.sp,
                        color = TextGray.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp)) // Reduced from 30.dp

            // Recent Records - Adaptive height, pushed to bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,  // 禁用点击水波纹
                        onClick = {} // 拦截点击防止穿透
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最近记录",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    TextButton(onClick = onSeeAllRecords) {
                        Text(
                            text = "查看全部",
                            color = PrimaryBlue,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // List Items from ViewModel
                // 仅在初始加载且无数据时显示 Loading，避免闪烁
                if (uiState.isLoading && uiState.records.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = PrimaryBlue
                        )
                    }
                } else if (uiState.records.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无面试记录\n开始一次面试吧！",
                            color = TextGray,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // 只显示最近2条记录
                    Column {
                        uiState.records.take(2).forEach { record ->
                            key(record.id) {
                                RecentRecordItem(record, onClick = { onRecordClick(record.id) })
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 根据分数获取状态文本
 */
private fun getStatusFromScore(score: Float?): String {
    return when {
        score == null -> "待分析"
        score >= 90 -> "优秀"
        score >= 80 -> "良好"
        score >= 70 -> "一般"
        score >= 60 -> "及格"
        else -> "不及格"
    }
}

/**
 * 根据分数获取状态颜色
 */
private fun getStatusColorFromScore(score: Float?): Color {
    return when {
        score == null -> Color(0xFF9E9E9E)  // 灰色 - 待分析
        score >= 90 -> Color(0xFF007AFF)    // 蓝色 - 优秀
        score >= 80 -> Color(0xFF5856D6)    // 紫色 - 良好
        score >= 70 -> Color(0xFF34C759)    // 绿色 - 一般
        score >= 60 -> Color(0xFFFF9500)    // 橙色 - 及格
        else -> Color(0xFFFF3B30)           // 红色 - 不及格
    }
}

/**
 * 格式化日期时间，并转换为北京时间
 */
private fun formatDateTime(dateTimeStr: String): String {
    return try {
        // 后端返回格式通常为 2026-01-31T10:30:00
        // 如果没有时区标志，认为是 UTC
        val isoStr = if (dateTimeStr.endsWith("Z") || dateTimeStr.contains("+")) {
            dateTimeStr
        } else {
            "${dateTimeStr}Z"
        }
        
        val zonedDateTime = java.time.ZonedDateTime.parse(isoStr)
        val beijingTime = zonedDateTime.withZoneSameInstant(java.time.ZoneId.of("Asia/Shanghai"))
        beijingTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } catch (e: Exception) {
        // 降级处理
        dateTimeStr.replace("T", " ").take(16)
    }
}

@Composable
fun RecentRecordItem(record: InterviewRecordResponse, onClick: () -> Unit = {}) {
    val score = record.score
    val status = getStatusFromScore(score)
    val statusColor = getStatusColorFromScore(score)
    val icon = when {
        score == null -> Icons.Default.History
        score >= 90 -> Icons.Default.TrendingUp
        score >= 70 -> Icons.Default.BarChart
        else -> Icons.Default.History
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp), // Reduced from 20.dp
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Background
            Surface(
                modifier = Modifier.size(44.dp), // Reduced from 52.dp
                shape = CircleShape,
                color = BackgroundWhite
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp), // Reduced from 28.dp
                    tint = PrimaryBlue
                )
            }

            Spacer(modifier = Modifier.padding(start = 16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (score != null) "${score.toInt()} 分" else record.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp, // Reduced from 20.sp
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDateTime(record.created_at),
                    fontSize = 12.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = TextGray.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = record.role_name ?: "面试记录",
                        fontSize = 13.sp,
                        color = TextGray.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Status Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statusColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = status,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color(0xFFC4C4C4)
            )
        }
    }
}

