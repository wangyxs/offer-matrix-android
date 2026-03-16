package com.example.offermatrix.ui.screens.upload_resume

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offermatrix.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadResumePage(onConfirmClick: () -> Unit, onBackPressed: () -> Unit) {
    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "上传简历", 
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "解析您的简历", 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "AI 将根据您的经历生成个性化面试问题", 
                    fontSize = 14.sp, 
                    color = TextGray
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Upload Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(
                    2.dp, 
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(TextGray.copy(alpha = 0.3f), TextGray.copy(alpha = 0.3f))
                    )
                ) // Note: Dashed border requires custom drawing, using solid light gray for now as approximation
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.UploadFile, 
                            contentDescription = "Upload", 
                            tint = PrimaryBlue,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "点击或拖拽上传 PDF/Word", 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "支持文件大小不超过 10MB", 
                        fontSize = 12.sp, 
                        color = TextGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Recent Uploads
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "最近上传", 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                TextButton(onClick = {}) {
                    Text("查看全部", color = PrimaryBlue, fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mock Recent File Item
            RecentFileItem(
                fileName = "My_Resume_2024.pdf",
                fileInfo = "2.4 MB · 2024-10-24 14:20",
                fileType = "pdf"
            )
            Spacer(modifier = Modifier.height(12.dp))
            RecentFileItem(
                fileName = "Backend_Dev_Resume.docx",
                fileInfo = "1.8 MB · 2024-09-15 09:45",
                fileType = "word"
            )
            
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onConfirmClick, 
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    "确认并开始解析", 
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun RecentFileItem(fileName: String, fileInfo: String, fileType: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (fileType == "pdf") Color(0xFFFFF4E5) else Color(0xFFE8F2FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (fileType == "pdf") Icons.Default.PictureAsPdf else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (fileType == "pdf") Color(0xFFFF9F43) else PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fileName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(fileInfo, fontSize = 12.sp, color = TextGray)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextGray)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UploadResumePagePreview() {
    UploadResumePage({}, {})
}
