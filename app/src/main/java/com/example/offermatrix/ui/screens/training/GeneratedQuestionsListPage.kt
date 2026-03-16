package com.example.offermatrix.ui.screens.training

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offermatrix.network.QuestionSetResponse
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.ui.theme.*
import com.example.offermatrix.ui.viewmodel.TrainingViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratedQuestionsListPage(onQuestionSetClick: (String) -> Unit, onBackPressed: () -> Unit) {
    val viewModel: TrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Silent refresh on resume
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("我的题组", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackPressed) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundWhite)
            )
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        val questionSets = uiState.questionSets
        // 追踪当前展开删除按钮的条目ID
        var expandedItemId by remember { mutableStateOf<Int?>(null) }
        
        // 监听列表滚动状态
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        
        // 当列表滚动时收起删除按钮
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                expandedItemId = null
            }
        }
        
        if (questionSets.isEmpty() && uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        expandedItemId = null
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (questionSets.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                            Text("暂无生成的题组", color = TextGray)
                        }
                    }
                } else {
                    items(
                        items = questionSets,
                        key = { it.id }
                    ) { set ->
                        SwipeToRevealDeleteQuestionSet(
                            itemId = set.id,
                            isExpanded = expandedItemId == set.id,
                            onExpandChange = { isExpanded ->
                                expandedItemId = if (isExpanded) set.id else null
                            },
                            onDelete = {
                                scope.launch {
                                    try {
                                        val response = RetrofitClient.apiService.deleteQuestionSet(set.id)
                                        if (response.success) {
                                            expandedItemId = null
                                            viewModel.loadData()
                                            Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            content = {
                                QuestionSetItemContent(
                                    set = set,
                                    onClick = { 
                                        expandedItemId = null
                                        onQuestionSetClick(set.id.toString()) 
                                    }
                                )
                            }
                        )
                    }
                    item {
                        Text(
                            text = "向左滑动显示删除按钮",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = TextGray
                        )
                    }
                }
            }
        }
    }
}

/**
 * 滑动显示删除按钮的题组组件
 * @param itemId 条目ID，用于状态管理
 * @param isExpanded 是否展开删除按钮
 * @param onExpandChange 展开状态变化回调
 * @param onDelete 删除回调
 * @param content 内容组件
 */
@Composable
fun SwipeToRevealDeleteQuestionSet(
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
            // 关闭时使用更快的动画，避免卡顿
            offsetX.animateTo(0f, animationSpec = tween(100))
        }
    }
    
    // 确保偏移量与展开状态同步（处理动画被打断的情况）
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
        // 删除按钮内容（固定在右侧）
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
                                // 根据当前偏移量决定是展开还是关闭
                                if (offsetX.value < -deleteButtonWidthPx / 2) {
                                    // 展开：先执行动画，再更新状态
                                    offsetX.animateTo(-deleteButtonWidthPx, animationSpec = tween(150))
                                    onExpandChange(true)
                                } else {
                                    // 关闭：先执行动画，再更新状态
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
 * 题组内容组件（不带长按删除，用于滑动删除包装）
 */
@Composable
fun QuestionSetItemContent(set: QuestionSetResponse, onClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(0.dp), // 移除阴影避免滑动时跳动
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
            // Unread Indicator
            if (!set.is_viewed) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(8.dp)
                        .background(PrimaryBlue, androidx.compose.foundation.shape.CircleShape)
                )
            }

            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(24.dp), tint = PrimaryBlue)
            Spacer(modifier = Modifier.padding(start = if (!set.is_viewed) 8.dp else 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(set.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${set.question_count}道题目", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                    Text(
                        formattedDate, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = TextGray
                    )
                }
            }
            Text(
                set.role_name ?: "未知角色", 
                style = MaterialTheme.typography.bodySmall, 
                color = PrimaryBlue, 
                fontWeight = FontWeight.Medium
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuestionSetItem(set: QuestionSetResponse, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unread Indicator
            if (!set.is_viewed) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(8.dp)
                        .background(PrimaryBlue, androidx.compose.foundation.shape.CircleShape)
                )
            }

            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(24.dp), tint = PrimaryBlue)
            Spacer(modifier = Modifier.padding(start = if (!set.is_viewed) 8.dp else 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(set.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${set.question_count}道题目", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
                    Text(
                        formattedDate, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = TextGray
                    )
                }
            }
            Text(
                set.role_name ?: "未知角色", 
                style = MaterialTheme.typography.bodySmall, 
                color = PrimaryBlue, 
                fontWeight = FontWeight.Medium
            )
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