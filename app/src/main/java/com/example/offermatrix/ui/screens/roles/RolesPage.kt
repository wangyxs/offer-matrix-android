package com.example.offermatrix.ui.screens.roles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import com.example.offermatrix.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import android.widget.Toast
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.network.UserRole
import com.example.offermatrix.network.UserSession
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolesPage(
    onAddRoleClick: () -> Unit,
    onResumeClick: (String) -> Unit,
    onMaterialsClick: (String) -> Unit
) {
    var userRoles by remember { mutableStateOf<List<UserRole>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    fun fetchRoles() {
        scope.launch {
            try {
                userRoles = RetrofitClient.apiService.getUserRoles(UserSession.userId)
            } catch (e: Exception) {
                e.printStackTrace()
                // Toast.makeText(context, "加载角色失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchRoles()
    }
    
    fun updateUserSessionRoles() {
        scope.launch {
            try {
                val userRoles = RetrofitClient.apiService.getUserRoles(UserSession.userId)
                // 提取 Role 对象列表
                UserSession.roles = userRoles.mapNotNull { it.role }
                // 如果当前角色被删除了，切换到第一个角色
                if (UserSession.currentRole != null && 
                    UserSession.roles.none { it.id == UserSession.currentRole?.id }) {
                    UserSession.currentRole = UserSession.roles.firstOrNull()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteRole(roleId: Int) {
        scope.launch {
            try {
                val response = RetrofitClient.apiService.removeRole(UserSession.userId, roleId)
                if (response.success) {
                    Toast.makeText(context, "角色删除成功", Toast.LENGTH_SHORT).show()
                    fetchRoles()
                    // 更新 UserSession 的角色数据
                    updateUserSessionRoles()
                } else {
                    Toast.makeText(context, response.message ?: "删除失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: retrofit2.HttpException) {
                // 解析错误响应
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = try {
                    // 尝试解析 JSON 错误信息
                    val jsonObject = org.json.JSONObject(errorBody ?: "{}")
                    jsonObject.optString("detail", "删除失败")
                } catch (ex: Exception) {
                    "删除失败"
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "我的角色库", 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    ) 
                },
                actions = {
                    IconButton(onClick = onAddRoleClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Role", tint = PrimaryBlue)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        },
        containerColor = BackgroundWhite
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(userRoles) { userRole ->
                UserRoleCard(
                    userRole = userRole,
                    onResumeClick = { onResumeClick(userRole.role?.name ?: "") },
                    onMaterialsClick = { onMaterialsClick(userRole.role?.name ?: "") },
                    onDeleteClick = { 
                         userRole.role_id.let { id -> deleteRole(id) }
                    }
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ℹ️ 简历与学习资料将帮助 AI 更好地理解您的背景并优化面试体验",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun UserRoleCard(
    userRole: UserRole,
    onResumeClick: () -> Unit,
    onMaterialsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFEEF2FF), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Code, // Placeholder icon
                        contentDescription = null, 
                        modifier = Modifier.size(28.dp), 
                        tint = PrimaryBlue
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(userRole.role?.name ?: "未知角色", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFFF0F2F5),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                         Text(
                             userRole.role?.description ?: "默认配置", 
                             fontSize = 10.sp, 
                             color = TextGray, 
                             modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                         )
                    }
                }
                
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = "Delete", 
                        tint = Color(0xFFFF5252), // Red for delete
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoleActionButton(
                    text = "简历管理",
                    icon = Icons.Default.Description,
                    onClick = onResumeClick,
                    modifier = Modifier.weight(1f)
                )
                RoleActionButton(
                    text = "学习资料",
                    icon = Icons.Default.Source,
                    onClick = onMaterialsClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RoleActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F9FA),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
        }
    }
}