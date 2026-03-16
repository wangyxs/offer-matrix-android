package com.example.offermatrix.ui.screens.analysis

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offermatrix.network.InterviewRecordResponse
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ConversationMessage(
    val role: String,
    val content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisPage(recordId: Int, onBackPressed: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var record by remember { mutableStateOf<InterviewRecordResponse?>(null) }
    var messages by remember { mutableStateOf<List<ConversationMessage>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    
    var strengths by remember { mutableStateOf<List<String>>(emptyList()) }
    var weaknesses by remember { mutableStateOf<List<String>>(emptyList()) }
    var dimensionScores by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    
    // 触发分析的函数
    suspend fun triggerAnalysis(): Boolean {
        return try {
            Log.i("AnalysisPage", "Triggering analysis for record $recordId")
            val analyzeResponse = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.analyzeInterviewRecord(recordId)
            }
            if (analyzeResponse.success && analyzeResponse.data != null) {
                Log.i("AnalysisPage", "Analysis completed: score=${analyzeResponse.data.score}")
                // 重新获取记录
                val updatedRecord = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getInterviewRecordDetail(recordId)
                }
                record = updatedRecord
                true
            } else {
                Log.e("AnalysisPage", "Analysis failed: ${analyzeResponse.message}")
                false
            }
        } catch (e: Exception) {
            Log.e("AnalysisPage", "Error triggering analysis", e)
            false
        }
    }
    
    LaunchedEffect(recordId) {
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.getInterviewRecordDetail(recordId)
            }
            record = response
            
            // 如果没有分析过（score为0或null），自动触发分析
            if (response.score == null || response.score == 0f) {
                isAnalyzing = true
                val success = triggerAnalysis()
                isAnalyzing = false
                if (success) {
                    // 使用更新后的record
                    record?.let { updatedRecord ->
                        // 重新解析feedback
                        updatedRecord.feedback?.let { feedbackJson ->
                            try {
                                val feedbackObj = org.json.JSONObject(feedbackJson)
                                if (feedbackObj.has("strengths")) {
                                    val arr = feedbackObj.getJSONArray("strengths")
                                    strengths = (0 until arr.length()).map { arr.getString(it) }
                                }
                                if (feedbackObj.has("weaknesses")) {
                                    val arr = feedbackObj.getJSONArray("weaknesses")
                                    weaknesses = (0 until arr.length()).map { arr.getString(it) }
                                }
                                if (feedbackObj.has("dimension_scores")) {
                                    val scores = feedbackObj.getJSONObject("dimension_scores")
                                    val map = mutableMapOf<String, Int>()
                                    scores.keys().forEach { key ->
                                        map[key] = scores.getInt(key)
                                    }
                                    dimensionScores = map
                                }
                            } catch (e: Exception) {
                                Log.e("AnalysisPage", "Failed to parse updated feedback", e)
                            }
                        }
                    }
                }
            }
            
            response.content?.let { contentJson ->
                try {
                    val jsonArray = JSONArray(contentJson)
                    val parsedMessages = mutableListOf<ConversationMessage>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        parsedMessages.add(ConversationMessage(
                            role = obj.getString("role"),
                            content = obj.getString("content")
                        ))
                    }
                    messages = parsedMessages
                } catch (e: Exception) {
                    Log.e("AnalysisPage", "Failed to parse conversation", e)
                }
            }
            
            response.feedback?.let { feedbackJson ->
                Log.d("AnalysisPage", "Raw feedback JSON: $feedbackJson")
                try {
                    val feedbackObj = org.json.JSONObject(feedbackJson)
                    
                    if (feedbackObj.has("strengths")) {
                        val arr = feedbackObj.getJSONArray("strengths")
                        strengths = (0 until arr.length()).map { arr.getString(it) }
                        Log.d("AnalysisPage", "Parsed strengths: $strengths")
                    }
                    
                    if (feedbackObj.has("weaknesses")) {
                        val arr = feedbackObj.getJSONArray("weaknesses")
                        weaknesses = (0 until arr.length()).map { arr.getString(it) }
                        Log.d("AnalysisPage", "Parsed weaknesses: $weaknesses")
                    }
                    
                    if (feedbackObj.has("dimension_scores")) {
                        val scores = feedbackObj.getJSONObject("dimension_scores")
                        val map = mutableMapOf<String, Int>()
                        scores.keys().forEach { key ->
                            map[key] = scores.getInt(key)
                        }
                        dimensionScores = map
                        Log.d("AnalysisPage", "Parsed dimension_scores: $dimensionScores")
                    } else {
                        Log.w("AnalysisPage", "No dimension_scores in feedback")
                    }
                } catch (e: Exception) {
                    Log.e("AnalysisPage", "Failed to parse feedback", e)
                }
            } ?: Log.w("AnalysisPage", "feedback is null")
            
            isLoading = false
        } catch (e: Exception) {
            Log.e("AnalysisPage", "Failed to load record", e)
            error = e.message
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "面试分析与回顾", 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    ) 
                },
                navigationIcon = { 
                    IconButton(onClick = onBackPressed) { 
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextDark) 
                    } 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->
        if (isLoading || isAnalyzing) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    if (isAnalyzing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("正在分析面试记录...", color = TextGray, fontSize = 14.sp)
                    }
                }
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("加载失败: $error", color = TextGray)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 分析报告标题
                Text(
                    text = "分析报告",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                
                // 雷达图卡片
                RadarChartCard(
                    score = record?.score?.toInt() ?: 0,
                    dimensionScores = dimensionScores,
                    strengths = strengths,
                    weaknesses = weaknesses
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 对话回顾标题
                Text(
                    text = "对话回顾",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // 对话列表
                ConversationSection(messages = messages)
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun RadarChartCard(
    score: Int,
    dimensionScores: Map<String, Int>,
    strengths: List<String>,
    weaknesses: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 雷达图
            RadarChart(
                score = score,
                dimensionScores = dimensionScores,
                modifier = Modifier.size(240.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 优势和待提升
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 优势
                FeedbackBox(
                    title = "优势",
                    items = strengths.take(1),
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                
                // 待提升
                FeedbackBox(
                    title = "待提升",
                    items = weaknesses.take(1),
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RadarChart(
    score: Int,
    dimensionScores: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    // 6个维度的标签和对应的key
    val dimensions = listOf(
        "逻辑表达" to "logic_expression",
        "技术深度" to "technical_depth",
        "稳定性" to "stability",
        "方案能力" to "solution_ability",
        "抗压能力" to "stress_resistance",
        "沟通技巧" to "communication"
    )
    
    // 获取分数，如果没有则使用总分
    val scores = dimensions.map { (_, key) ->
        dimensionScores[key] ?: dimensionScores["expression"] ?: dimensionScores["logic"] ?: score
    }
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = minOf(centerX, centerY) * 0.65f
            val labelRadius = radius * 1.35f
            
            // 绘制背景网格（3层）
            for (level in 1..3) {
                val levelRadius = radius * level / 3
                val path = Path()
                for (i in dimensions.indices) {
                    val angle = (-PI / 2 + 2 * PI * i / dimensions.size).toFloat()
                    val x = centerX + levelRadius * cos(angle)
                    val y = centerY + levelRadius * sin(angle)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(
                    path = path,
                    color = Color(0xFF4A90E2).copy(alpha = 0.2f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            
            // 绘制轴线
            for (i in dimensions.indices) {
                val angle = (-PI / 2 + 2 * PI * i / dimensions.size).toFloat()
                val endX = centerX + radius * cos(angle)
                val endY = centerY + radius * sin(angle)
                drawLine(
                    color = Color(0xFF4A90E2).copy(alpha = 0.3f),
                    start = Offset(centerX, centerY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            // 绘制数据区域
            val dataPath = Path()
            for (i in dimensions.indices) {
                val angle = (-PI / 2 + 2 * PI * i / dimensions.size).toFloat()
                val value = scores[i].coerceIn(0, 100) / 100f
                val dataRadius = radius * value
                val x = centerX + dataRadius * cos(angle)
                val y = centerY + dataRadius * sin(angle)
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()
            
            // 填充区域
            drawPath(
                path = dataPath,
                color = Color(0xFF4A90E2).copy(alpha = 0.3f)
            )
            
            // 绘制边框
            drawPath(
                path = dataPath,
                color = Color(0xFF4A90E2),
                style = Stroke(width = 2.dp.toPx())
            )
            
            // 绘制数据点
            for (i in dimensions.indices) {
                val angle = (-PI / 2 + 2 * PI * i / dimensions.size).toFloat()
                val value = scores[i].coerceIn(0, 100) / 100f
                val dataRadius = radius * value
                val x = centerX + dataRadius * cos(angle)
                val y = centerY + dataRadius * sin(angle)
                drawCircle(
                    color = Color(0xFF4A90E2),
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            // 绘制维度标签
            val paint = android.graphics.Paint().apply {
                textSize = 11.sp.toPx()
                color = android.graphics.Color.parseColor("#666666")
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            
            for (i in dimensions.indices) {
                val angle = (-PI / 2 + 2 * PI * i / dimensions.size).toFloat()
                val x = centerX + labelRadius * cos(angle)
                val y = centerY + labelRadius * sin(angle)
                
                drawContext.canvas.nativeCanvas.drawText(
                    dimensions[i].first,
                    x,
                    y + paint.textSize / 3,
                    paint
                )
            }
        }
        
        // 中心分数
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Text(
                text = "综合评分",
                fontSize = 12.sp,
                color = TextGray
            )
        }
    }
}

@Composable
fun FeedbackBox(
    title: String,
    items: List<String>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (items.isEmpty()) {
                Text(
                    text = "暂无",
                    fontSize = 13.sp,
                    color = TextGray
                )
            } else {
                items.forEach { item ->
                    Text(
                        text = item,
                        fontSize = 13.sp,
                        color = TextDark,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationSection(messages: List<ConversationMessage>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (messages.isEmpty()) {
            Text("暂无对话记录", color = TextGray, fontSize = 14.sp)
        } else {
            messages.forEach { message ->
                val isUser = message.role == "user"
                MessageBubble(isUser = isUser, message = message.content)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(isUser: Boolean, message: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 角色标签在右边（用户）或左边（面试官）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isUser) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = if (isUser) "我" else "面试官",
                fontSize = 12.sp,
                color = TextGray
            )
            if (isUser) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // 消息气泡
        val bubbleColor = if (isUser) PrimaryBlue else Color(0xFFF5F5F5)
        val textColor = if (isUser) Color.White else TextDark
        val bubbleShape = if (isUser) {
            RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
        } else {
            RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
        }
        
        // 根据消息长度确定气泡宽度
        // 短消息用较窄的气泡，长消息才占满宽度
        val messageLength = message.length
        val bubbleModifier = when {
            messageLength <= 10 -> Modifier.widthIn(max = 150.dp)  // 很短的消息
            messageLength <= 25 -> Modifier.widthIn(max = 220.dp)  // 短消息
            messageLength <= 50 -> Modifier.widthIn(max = 280.dp)  // 中等消息
            else -> Modifier.fillMaxWidth(0.88f)                    // 长消息
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Surface(
                shape = bubbleShape,
                color = bubbleColor,
                modifier = bubbleModifier
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(14.dp),
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}