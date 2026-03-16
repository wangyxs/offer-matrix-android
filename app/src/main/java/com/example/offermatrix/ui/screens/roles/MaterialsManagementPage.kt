package com.example.offermatrix.ui.screens.roles

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import com.example.offermatrix.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.network.UserSession
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.MediaType.Companion.toMediaTypeOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsManagementPage(roleName: String, onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Find role ID
    val role = UserSession.roles.find { it.name == roleName }
    val roleId = role?.id ?: -1
    
    var documents by remember { mutableStateOf<List<com.example.offermatrix.network.Document>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Dialog States
    var showDialog by remember { mutableStateOf(false) }
    var dialogContent by remember { mutableStateOf("") }
    var dialogTitle by remember { mutableStateOf("") }
    var isDialogLoading by remember { mutableStateOf(false) }
    
    // Fetch documents
    fun fetchDocuments() {
        if (roleId == -1) return
        scope.launch {
            isLoading = true
            try {
                val docs = RetrofitClient.apiService.getDocuments(roleId, "MATERIAL")
                documents = docs
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
    
    // Fetch Content and Show
    fun openDocument(doc: com.example.offermatrix.network.Document) {
        scope.launch {
            isDialogLoading = true
            showDialog = true
            dialogTitle = doc.name
            dialogContent = "Loading..."
            try {
                 val res = RetrofitClient.apiService.getDocumentContent(doc.id)
                 if (res.success && res.data != null) {
                     dialogContent = res.data.content
                 } else {
                     dialogContent = "无法读取内容: ${res.message}"
                 }
            } catch (e: Exception) {
                dialogContent = "读取失败: ${e.message}"
            } finally {
                isDialogLoading = false
            }
        }
    }
    
    LaunchedEffect(roleId) {
        fetchDocuments()
    }

    // File Picker
    val launcher = rememberLauncherForActivityResult(
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
                        val requestFile = okhttp3.RequestBody.create("application/octet-stream".toMediaTypeOrNull(), byteArray)
                        // Try to get filename
                        var filename = "material_file"
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) filename = cursor.getString(nameIndex)
                            }
                        }
                        
                        val body = okhttp3.MultipartBody.Part.createFormData("file", filename, requestFile)
                        
                        val response = RetrofitClient.apiService.uploadDocument(roleId, "MATERIAL", body)
                        if (response.success) {
                            Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show()
                            fetchDocuments()
                        } else {
                            Toast.makeText(context, "上传失败: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "上传出错: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(dialogTitle, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
            text = {
                if (isDialogLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(400.dp)) {
                        item { Text(dialogContent ?: "No Content") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "学习资料 - $roleName", 
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
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        },
        containerColor = BackgroundWhite
    ) {
        Column(
            modifier = Modifier.padding(it).fillMaxSize().padding(20.dp)
        ) {
            Surface(
                onClick = { launcher.launch("*/*") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加资料", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(documents) { doc ->
                    MaterialItem(
                        file = doc, 
                        onClick = { openDocument(doc) },
                        onDelete = {
                             scope.launch {
                                 try {
                                     val res = RetrofitClient.apiService.deleteDocument(doc.id)
                                     if (res.success) {
                                         Toast.makeText(context, "删除成功", Toast.LENGTH_SHORT).show()
                                         fetchDocuments()
                                     }
                                 } catch (e: Exception) {
                                     Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
                                 }
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                 Text(
                    text = "ℹ️ 资料越多，AI 生成的面试题越贴合实际",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun MaterialItem(file: com.example.offermatrix.network.Document, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightBlue),
                contentAlignment = Alignment.Center
            ) {
                 Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(24.dp), tint = PrimaryBlue)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${file.size ?: "Unknown"} · ${file.date}", color = TextGray, fontSize = 12.sp)
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
            }
        }
    }
}