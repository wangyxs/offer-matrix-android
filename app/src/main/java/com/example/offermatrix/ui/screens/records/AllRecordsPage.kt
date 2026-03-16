package com.example.offermatrix.ui.screens.records

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.offermatrix.network.InterviewRecordResponse
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.ui.theme.*
import com.example.offermatrix.ui.viewmodel.InterviewRecordsViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRecordsPage(
    onRecordClick: (Int) -> Unit, 
    onBackPressed: () -> Unit,
    viewModel: InterviewRecordsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val context = LocalContext.current
    
    // 追踪当前展开删除按钮的条目ID
    var expandedItemId by remember { mutableStateOf<Int?>(null) }
    
    // 监听列表滚动状态
    val listState = rememberLazyListState()
    
    // 当列表滚动时收起删除按钮
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            expandedItemId = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("全部面试记录", fontWeight = FontWeight.Bold) },
                navigationIcon = { 
                    IconButton(onClick = onBackPressed) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null) 
                    } 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundWhite)
            )
        },
        containerColor = BackgroundWhite
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (uiState.records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无面试记录",
                        color = TextGray,
                        fontSize = 16.sp
                    )
                }
            } else {
                val filtered = uiState.records
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            expandedItemId = null
                        },
                    contentPadding = PaddingValues(vertical = 16.dp), 
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filtered, key = { it.id }) { record ->
                        SwipeToRevealDeleteRecord(
                            itemId = record.id,
                            isExpanded = expandedItemId == record.id,
                            onExpandChange = { isExpanded ->
                                expandedItemId = if (isExpanded) record.id else null
                            },
                            onDelete = {
                                expandedItemId = null
                                viewModel.deleteRecord(
                                    recordId = record.id,
                                    onSuccess = {
                                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            content = {
                                RecordItemContent(
                                    record = record, 
                                    onClick = { 
                                        expandedItemId = null
                                        onRecordClick(record.id) 
                                    }
                                )
                            }
                        )
                    }
                    
                    item {
                        Text(
                            text = "向左滑动显示删除按钮",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 滑动显示删除按钮的面试记录组件
 */
@Composable
fun SwipeToRevealDeleteRecord(
    itemId: Int,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val deleteButtonWidth = 80.dp
    val deleteButtonWidthPx = with(LocalDensity.current) { deleteButtonWidth.toPx() }
    
    val offsetX = remember { Animatable(0f) }
    
    // 当外部 isExpanded 状态变化时，更新内部偏移量
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            offsetX.animateTo(-deleteButtonWidthPx, animationSpec = tween(200))
        } else {
            offsetX.animateTo(0f, animationSpec = tween(100))
        }
    }
    
    // 确保偏移量与展开状态同步
    LaunchedEffect(Unit) {
        snapshotFlow { isExpanded }.collect { expanded ->
            if (!expanded && offsetX.value != 0f && !offsetX.isRunning) {
                offsetX.snapTo(0f)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(20.dp))
            .background(ErrorRed)
    ) {
        // 删除按钮内容
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(deleteButtonWidth)
                .fillMaxHeight()
                .clickable {
                    scope.launch {
                        onExpandChange(false)
                        onDelete()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "删除",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        // 前景内容（可滑动）
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .fillMaxHeight()
                .background(SurfaceWhite, shape = RoundedCornerShape(20.dp))
                .pointerInput(itemId) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            if (!isExpanded) {
                                onExpandChange(false)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -deleteButtonWidthPx / 2) {
                                    offsetX.animateTo(-deleteButtonWidthPx, animationSpec = tween(150))
                                    onExpandChange(true)
                                } else {
                                    offsetX.animateTo(0f, animationSpec = tween(150))
                                    onExpandChange(false)
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newOffset = (offsetX.value + dragAmount)
                                    .coerceIn(-deleteButtonWidthPx, 0f)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
        ) {
            content()
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
 * 格式化日期时间
 */
private fun formatDateTime(dateTimeStr: String): String {
    return try {
        val inputFormatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
        val outputFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val dateTime = java.time.LocalDateTime.parse(dateTimeStr, inputFormatter)
        val zonedDateTime = dateTime.atZone(java.time.ZoneId.of("UTC"))
            .withZoneSameInstant(java.time.ZoneId.systemDefault())
        outputFormatter.format(zonedDateTime)
    } catch (e: Exception) {
        dateTimeStr.replace("T", " ").take(16)
    }
}

/**
 * 面试记录内容组件
 */
@Composable
fun RecordItemContent(record: InterviewRecordResponse, onClick: () -> Unit) {
    val score = record.score
    val status = getStatusFromScore(score)
    val statusColor = getStatusColorFromScore(score)
    val icon = when {
        score == null -> Icons.Default.History
        score >= 90 -> Icons.Default.TrendingUp
        score >= 70 -> Icons.Default.BarChart
        else -> Icons.Default.Pending
    }
    
    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(24.dp), 
                tint = PrimaryBlue
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (score != null) "${score.toInt()} 分" else record.title, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp, 
                    color = TextDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.role_name ?: "面试",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDateTime(record.created_at), 
                        style = MaterialTheme.typography.bodySmall, 
                        color = TextGray
                    )
                }
            }
            
            // 状态标签
            Text(
                text = status, 
                style = MaterialTheme.typography.bodySmall, 
                color = statusColor, 
                fontWeight = FontWeight.Medium
            )
            
            // 箭头
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null, 
                tint = Color(0xFFE0E0E0), 
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(14.dp)
            )
        }
    }
}

// 保留原有的 RecordItem 以兼容旧代码
@Composable
fun RecordItem(record: InterviewRecordResponse, onClick: () -> Unit) {
    RecordItemContent(record = record, onClick = onClick)
}