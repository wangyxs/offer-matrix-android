package com.example.offermatrix.ui.screens.training

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.offermatrix.network.QuestionDetail
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFavoritesPage(onBackPressed: () -> Unit, onFavoriteClick: (Int) -> Unit) {
    var favorites by remember { mutableStateOf<List<QuestionDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadFavorites() {
        scope.launch {
            isLoading = true
            try {
                favorites = RetrofitClient.apiService.getFavorites()
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "加载失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadFavorites()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("我的收藏") },
                navigationIcon = { IconButton(onClick = onBackPressed) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        // 追踪当前展开删除按钮的条目ID
        var expandedItemId by remember { mutableStateOf<Int?>(null) }
        
        // 监听列表滚动状态 - 放在条件外部
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        
        // 当列表滚动时收起删除按钮
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                expandedItemId = null
            }
        }
        
        if (isLoading) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (errorMessage != null) {
             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = TextGray)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        expandedItemId = null
                    }, 
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                 if (favorites.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                            Text("暂无收藏的题目", color = TextGray)
                        }
                    }
                } else {
                    items(favorites, key = { it.id }) { item ->
                        SwipeToRevealDeleteItem(
                            itemId = item.id,
                            isExpanded = expandedItemId == item.id,
                            onExpandChange = { isExpanded ->
                                expandedItemId = if (isExpanded) item.id else null
                            },
                            onDelete = {
                                scope.launch {
                                    try {
                                        val response = RetrofitClient.apiService.removeFavorite(item.id)
                                        if (response.success) {
                                            expandedItemId = null
                                            Toast.makeText(context, "已移除收藏", Toast.LENGTH_SHORT).show()
                                            favorites = favorites.filter { it.id != item.id }
                                        } else {
                                            Toast.makeText(context, "移除失败: ${response.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "移除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            content = {
                                FavoriteListItem(item, onClick = { 
                                    expandedItemId = null
                                    onFavoriteClick(item.id) 
                                })
                            }
                        )
                    }
                     item {
                        Text(
                            text = "向左滑动显示删除按钮",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = TextGray,
                            fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                    }
                }
            }
        }
    }
}

/**
 * 滑动显示删除按钮的组件
 * @param itemId 条目ID，用于状态管理
 * @param isExpanded 是否展开删除按钮
 * @param onExpandChange 展开状态变化回调
 * @param onDelete 删除回调
 * @param content 内容组件
 */
@Composable
fun SwipeToRevealDeleteItem(
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
            .clip(RoundedCornerShape(16.dp))
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
        
        // 前景内容（可滑动）- 添加背景色覆盖
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .fillMaxHeight()
                .background(Color.White, shape = RoundedCornerShape(16.dp))
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

@Composable
fun FavoriteListItem(favorite: QuestionDetail, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 星星图标（无背景）
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    favorite.question_text,
                    color = TextDark,
                    fontSize = 15.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DifficultyChip(favorite.difficulty)
                    if (favorite.category != null) {
                        CategoryChip(favorite.category)
                    }
                }
            }
            
            Icon(
                Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFBDBDBD),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 难度标签 - 根据难度显示不同颜色，并将英文转为中文
 */
@Composable
private fun DifficultyChip(difficulty: String) {
    // 根据难度确定颜色和中文显示文本
    val (backgroundColor, textColor, displayText) = when {
        difficulty.contains("简单", ignoreCase = true) || difficulty.contains("Easy", ignoreCase = true) ->
            Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "简单")
        difficulty.contains("中等", ignoreCase = true) || difficulty.contains("Medium", ignoreCase = true) ->
            Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "中等")
        difficulty.contains("困难", ignoreCase = true) || difficulty.contains("Hard", ignoreCase = true) ->
            Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "困难")
        else -> Triple(Color(0xFFF5F5F5), TextGray, difficulty) // 未知难度原样显示
    }
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Text(
            text = displayText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * 分类标签
 */
@Composable
private fun CategoryChip(category: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFE3F2FD)
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            color = PrimaryBlue
        )
    }
}