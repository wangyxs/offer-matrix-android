package com.example.offermatrix.ui.screens.select_role

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import com.example.offermatrix.network.AssignRoleRequest
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.network.Role
import com.example.offermatrix.network.UserSession
import com.example.offermatrix.ui.theme.BackgroundWhite
import com.example.offermatrix.ui.theme.PrimaryBlue
import com.example.offermatrix.ui.theme.SurfaceWhite
import com.example.offermatrix.ui.theme.TextDark
import com.example.offermatrix.ui.theme.TextGray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectRolePage(onNextClick: () -> Unit, onBackPressed: () -> Unit) {
    var selectedCategory by remember { mutableStateOf("全部") }
    var searchQuery by remember { mutableStateOf("") }
    var roles by remember { mutableStateOf<List<Role>>(emptyList()) }
    var selectedRoleId by remember { mutableStateOf(-1) }
    val categories = listOf("全部", "后端", "前端", "产品", "AI", "测试", "运维")

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            roles = RetrofitClient.apiService.getRoles()
            if (roles.isNotEmpty()) {
                selectedRoleId = roles[0].id
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "加载角色失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "选择职位角色",
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundWhite
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(BackgroundWhite) // Ensure background covers list content if scrolling
            ) {
                Button(
                    onClick = {
                        if (selectedRoleId != -1) {
                            scope.launch {
                                try {
                                    val response = RetrofitClient.apiService.assignRole(
                                        AssignRoleRequest(UserSession.userId, selectedRoleId)
                                    )
                                    if (response.success) {
                                        // 更新 UserSession 的角色数据
                                        try {
                                            val userRoles = RetrofitClient.apiService.getUserRoles(UserSession.userId)
                                            UserSession.roles = userRoles.mapNotNull { it.role }
                                            // 如果之前没有当前角色（首次添加），则设置为新添加的角色
                                            if (UserSession.currentRole == null) {
                                                val newRole = roles.find { it.id == selectedRoleId }
                                                if (newRole != null) {
                                                    UserSession.currentRole = newRole
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        
                                        android.widget.Toast.makeText(context, "角色添加成功", android.widget.Toast.LENGTH_SHORT).show()
                                        onNextClick()
                                    } else {
                                        android.widget.Toast.makeText(context, response.message ?: "添加失败", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: retrofit2.HttpException) {
                                    // 解析错误响应
                                    val errorBody = e.response()?.errorBody()?.string()
                                    val errorMessage = try {
                                        // 尝试解析 JSON 错误信息
                                        val jsonObject = org.json.JSONObject(errorBody ?: "{}")
                                        jsonObject.optString("detail", "添加失败")
                                    } catch (ex: Exception) {
                                        "添加失败"
                                    }
                                    android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "网络错误: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = selectedRoleId != -1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    )
                ) {
                    Text(
                        text = "确认",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请选择您的模拟岗位",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "我们将根据岗位特性为您匹配面试题库",
                    fontSize = 14.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索职位", color = TextGray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceWhite,
                                labelColor = TextGray
                            ),
                            border = null, // Remove border for cleaner look
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val filteredRoles = roles.filter { role ->
                    val matchCategory = selectedCategory == "全部" || role.category == selectedCategory
                    val matchSearch = searchQuery.isEmpty() || role.name.contains(searchQuery, ignoreCase = true)
                    matchCategory && matchSearch
                }

                items(filteredRoles) { role ->
                    RoleCard(role = role, isSelected = selectedRoleId == role.id, onClick = { selectedRoleId = role.id })
                }
                // Removed AddRoleCard as requested

                // Add spacer at bottom to avoid content being hidden by floating button if needed
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun RoleCard(role: Role, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp) // Fixed height for consistency
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F0FE) else SurfaceWhite // Light blue tint when selected
        ),
        border = if (isSelected) BorderStroke(2.dp, PrimaryBlue) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section: Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color.White else BackgroundWhite,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Work, // Default icon
                        contentDescription = null,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(28.dp),
                        tint = if (isSelected) PrimaryBlue else TextDark
                    )
                }

                // Bottom section: Info
                Column {
                    Text(
                        role.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextDark,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        role.description ?: "暂无描述",
                        fontSize = 11.sp,
                        color = TextGray,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Small tag
                    Surface(
                        color = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else BackgroundWhite,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            role.category ?: "通用",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = if (isSelected) PrimaryBlue else TextGray
                        )
                    }
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(24.dp)
                )
            }
        }
    }
}
