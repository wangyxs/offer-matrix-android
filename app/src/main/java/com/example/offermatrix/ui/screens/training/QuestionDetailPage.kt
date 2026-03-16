package com.example.offermatrix.ui.screens.training

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offermatrix.network.QuestionDetail
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailPage(questionId: String, onBackPressed: () -> Unit) {
    var question by remember { mutableStateOf<QuestionDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(questionId) {
        try {
            val id = questionId.toIntOrNull()
            if (id != null) {
                question = RetrofitClient.apiService.getQuestionById(id)
            } else {
                error = "无效的题目 ID"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            error = "加载失败: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("题目详情") },
                navigationIcon = { IconButton(onClick = onBackPressed) { Icon(Icons.Default.ArrowBack, null) } }
            )
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = PrimaryBlue)
                error != null -> Text(error!!, color = TextGray)
                question != null -> {
                    val q = question!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Title / Question
                        Text(
                            text = q.question_text,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            lineHeight = 28.sp
                        )
                        
                        // Tags
                        Row(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DifficultyChip(q.difficulty)
                            if (q.category != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CategoryChip(q.category)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        androidx.compose.material3.HorizontalDivider(
                            thickness = 0.5.dp,
                            color = androidx.compose.ui.graphics.Color(0xFFE0E0E0)
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Answer
                        Text(
                            "参考答案",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = q.answer_text ?: "暂无答案",
                            fontSize = 16.sp,
                            color = TextDark,
                            lineHeight = 26.sp
                        )
                        
                        // Extra bottom padding
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
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
        else -> Triple(Color(0xFFF5F5F5), TextGray, difficulty)
    }
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor
    ) {
        Text(
            text = displayText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
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
            fontWeight = FontWeight.Medium,
            color = PrimaryBlue
        )
    }
}
