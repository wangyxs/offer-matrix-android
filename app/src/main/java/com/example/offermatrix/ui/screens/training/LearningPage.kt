package com.example.offermatrix.ui.screens.training

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offermatrix.network.QuestionDetail
import com.example.offermatrix.network.QuestionSetDetail
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LearningPage(questionSetId: String, onBackPressed: () -> Unit) {
    var questionSetDetail by remember { mutableStateOf<QuestionSetDetail?>(null) }
    var isBookmarked by remember { mutableStateOf(false) }
    var isFlipped by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // 加载题组数据
    LaunchedEffect(questionSetId) {
        isLoading = true
        try {
            val setId = questionSetId.toIntOrNull() ?: return@LaunchedEffect
            questionSetDetail = RetrofitClient.apiService.getQuestionSetDetail(setId)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "加载题组失败: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(questionSetDetail?.title ?: "题组 #$questionSetId") },
                navigationIcon = { IconButton(onClick = onBackPressed) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (questionSetDetail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("加载失败", color = TextGray)
            }
        } else if (questionSetDetail!!.questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("暂无题目", color = TextGray)
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { questionSetDetail!!.questions.size })

            // 当页面变化时，重置翻转状态并检查收藏状态
            LaunchedEffect(pagerState.currentPage) {
                isFlipped = false
                try {
                    val currentQuestionId = questionSetDetail!!.questions[pagerState.currentPage].id
                    val favoriteStatus = RetrofitClient.apiService.checkFavoriteStatus(currentQuestionId)
                    isBookmarked = favoriteStatus["is_favorited"] == true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).padding(16.dp), contentAlignment = Alignment.Center) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        // 仅当是当前页时才处理翻转，避免其他页状态混乱
                        val currentQuestion = questionSetDetail!!.questions[page]
                        // 使用 key 确保每页状态独立
                        key(currentQuestion.id) {
                            // 这里我们使用 pagerState.currentPage == page 来控制是否显示翻转后的状态
                            // 但实际上 HorizontalPager 的 item 会被重用，所以最好还是只依赖 isFlipped 状态
                            // 不过因为 isFlipped 是 hoist 到父组件的，所以所有卡片共享一个翻转状态
                            // 为了正确效果，我们只对当前页应用 isFlipped
                            val showAnswer = if (pagerState.currentPage == page) isFlipped else false
                            
                            FlipCard(
                                question = currentQuestion,
                                isFlipped = showAnswer,
                                onFlip = { 
                                    if (pagerState.currentPage == page) {
                                        isFlipped = !isFlipped
                                    }
                                }
                            )
                        }
                    }
                }
                
                BottomNavBar(
                    currentIndex = pagerState.currentPage,
                    totalCount = questionSetDetail!!.questions.size,
                    isBookmarked = isBookmarked,
                    onBookmarkToggle = {
                        val currentQuestion = questionSetDetail!!.questions[pagerState.currentPage]
                        scope.launch {
                            try {
                                if (isBookmarked) {
                                    RetrofitClient.apiService.removeFavorite(currentQuestion.id)
                                    Toast.makeText(context, "已取消收藏", Toast.LENGTH_SHORT).show()
                                } else {
                                    RetrofitClient.apiService.addFavorite(currentQuestion.id)
                                    Toast.makeText(context, "收藏成功", Toast.LENGTH_SHORT).show()
                                }
                                isBookmarked = !isBookmarked
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "收藏操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onPrevious = {
                         scope.launch {
                             if (pagerState.currentPage > 0) {
                                 pagerState.animateScrollToPage(pagerState.currentPage - 1)
                             }
                         }
                    },
                    onNext = {
                        scope.launch {
                            if (pagerState.currentPage < questionSetDetail!!.questions.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FlipCard(question: QuestionDetail, isFlipped: Boolean, onFlip: () -> Unit) {
    val rotationY by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f, label = "")
    val density = LocalDensity.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp) // 增加高度以适应更多内容
            .graphicsLayer {
                this.rotationY = rotationY
                this.cameraDistance = 8 * density.density
            }
            .clickable(onClick = onFlip),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        if (rotationY <= 90f) {
            QuestionSide(question = question)
        } else {
            AnswerSide(answer = question.answer_text ?: "暂无答案", modifier = Modifier.graphicsLayer { this.rotationY = 180f })
        }
    }
}

@Composable
fun QuestionSide(question: QuestionDetail) {
    val difficultyColor = when(question.difficulty) {
        "easy" -> SuccessGreen
        "hard" -> ErrorRed
        else -> WarningYellow
    }
    val difficultyText = when(question.difficulty) {
        "easy" -> "简单"
        "hard" -> "困难"
        else -> "中等"
    }
    
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = RoundedCornerShape(8.dp), color = difficultyColor.copy(alpha = 0.1f)) {
                    Text(
                        difficultyText,
                        color = difficultyColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (question.category != null) {
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = PrimaryBlue.copy(alpha = 0.1f)) {
                        Text(
                            question.category,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = question.question_text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = TextDark,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Lock, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("点击翻转查看答案", color = TextGray, fontSize = 14.sp)
        }
    }
}

@Composable
fun AnswerSide(answer: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                "参考答案",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = answer,
                fontSize = 16.sp,
                color = TextDark,
                lineHeight = 26.sp
            )
        }
    }
}


@Composable
fun BottomNavBar(
    currentIndex: Int,
    totalCount: Int,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundWhite)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
         Text(
            "${currentIndex + 1} / $totalCount",
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            textAlign = TextAlign.Center,
            color = TextGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Previous Button
            Button(
                onClick = onPrevious,
                enabled = currentIndex > 0,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceWhite,
                    disabledContainerColor = SurfaceWhite.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "上一题",
                    tint = if (currentIndex > 0) PrimaryBlue else TextGray
                )
            }

            // Bookmark Button
            Button(
                onClick = onBookmarkToggle,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBookmarked) PrimaryBlue.copy(alpha = 0.1f) else SurfaceWhite
                ),
                border = BorderStroke(1.dp, if (isBookmarked) PrimaryBlue else Color(0xFFE0E0E0)),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(
                    if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "收藏",
                    tint = PrimaryBlue
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    if (isBookmarked) "已收藏" else "收藏题目", 
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    fontSize = 16.sp
                )
            }

            // Next Button
            Button(
                onClick = onNext,
                enabled = currentIndex < totalCount - 1,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceWhite,
                    disabledContainerColor = SurfaceWhite.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "下一题",
                    tint = if (currentIndex < totalCount - 1) PrimaryBlue else TextGray
                )
            }
        }
    }
}