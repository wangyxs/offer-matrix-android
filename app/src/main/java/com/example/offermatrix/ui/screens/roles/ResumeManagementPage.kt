package com.example.offermatrix.ui.screens.roles

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.network.UserSession
import com.example.offermatrix.ui.theme.PrimaryBlue
import com.example.offermatrix.ui.theme.SurfaceWhite
import com.example.offermatrix.ui.theme.TextDark
import com.example.offermatrix.ui.theme.TextGray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeManagementPage(roleName: String, onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Find role ID
    val role = UserSession.roles.find { it.name == roleName }
    val roleId = role?.id ?: -1

    var documents by remember { mutableStateOf<List<com.example.offermatrix.network.Document>>(emptyList()) }
    var contentText by remember { mutableStateOf<String?>(null) }

    // Fetch documents
    fun fetchDocuments() {
        if (roleId == -1) return
        scope.launch {
            try {
                val docs = RetrofitClient.apiService.getDocuments(roleId, "RESUME")
                documents = docs
                if (docs.isNotEmpty()) {
                    // Fetch content for the first resume
                    try {
                        val res = RetrofitClient.apiService.getDocumentContent(docs[0].id)
                        if (res.success && res.data != null) {
                            contentText = res.data.content
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    contentText = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(roleId) {
        fetchDocuments()
    }

    // File Picker
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val contentResolver = context.contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    val byteArray = inputStream?.readBytes()
                    inputStream?.close()

                    if (byteArray != null) {
                        val requestFile = okhttp3.RequestBody.create("application/pdf".toMediaTypeOrNull(), byteArray)
                        // Try to get filename
                        var filename = "resume.pdf"
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) filename = cursor.getString(nameIndex)
                            }
                        }

                        val body = okhttp3.MultipartBody.Part.createFormData("file", filename, requestFile)

                        val response = RetrofitClient.apiService.uploadDocument(roleId, "RESUME", body)
                        if (response.success) {
                            android.widget.Toast.makeText(context, "上传成功", android.widget.Toast.LENGTH_SHORT).show()
                            fetchDocuments()
                        } else {
                            android.widget.Toast.makeText(context, "上传失败: ${response.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "上传出错: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("上传简历") },
                navigationIcon = { IconButton(onClick = onBackPressed) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("解析您的简历 - $roleName", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextDark)
            Text("AI 将根据您的经历生成个性化面试问题", color = TextGray)
            Spacer(modifier = Modifier.height(32.dp))

            // Upload Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp) // Reduced height
                    .border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)) // Dashed border simulated
                    .clickable { launcher.launch("application/pdf") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(36.dp))
                    Text("点击上传 PDF", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    Text("支持文件大小不超过 10MB", color = TextGray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("简历内容", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (documents.isNotEmpty()) {
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                val res = RetrofitClient.apiService.deleteDocument(documents[0].id)
                                if (res.success) {
                                    android.widget.Toast.makeText(context, "删除成功", android.widget.Toast.LENGTH_SHORT).show()
                                    fetchDocuments()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "删除失败", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                if (documents.isNotEmpty() && contentText != null) {
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        item {
                            Text(
                                text = contentText!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextDark
                            )
                        }
                    }
                } else if (documents.isNotEmpty() && contentText == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无简历内容，请上传", color = TextGray)
                    }
                }
            }
        }
    }
}
