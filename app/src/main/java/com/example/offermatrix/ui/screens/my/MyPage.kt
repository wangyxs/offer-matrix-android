package com.example.offermatrix.ui.screens.my

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offermatrix.network.RetrofitClient
import com.example.offermatrix.network.UpdateProfileRequest
import com.example.offermatrix.network.UserSession
import com.example.offermatrix.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun MyPage(onLogout: () -> Unit) {
    var username by remember { mutableStateOf(UserSession.username) }
    var avatar by remember { mutableStateOf(UserSession.avatar ?: "default") }

    var showNameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(containerColor = BackgroundWhite) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Section
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.clickable { showAvatarDialog = true }) {
                Surface(
                    shape = CircleShape,
                    color = SurfaceWhite,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(100.dp)
                ) {
                    Image(
                        imageVector = getAvatarIcon(avatar),
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .background(Color.White, CircleShape)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFC107), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showNameDialog = true }
            ) {
                Text(username, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Name",
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(18.dp),
                    tint = TextGray
                )
            }
            // ID hidden as requested

            Spacer(modifier = Modifier.height(32.dp))

            // Settings - Account Management
            Text(
                "账号管理",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 8.dp),
                fontSize = 14.sp,
                color = TextGray
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsItem(icon = Icons.Default.Lock, title = "密码设置") {
                        showPasswordDialog = true
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings - General
            Text(
                "通用设置",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, bottom = 8.dp),
                fontSize = 14.sp,
                color = TextGray
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    SettingsItem(icon = Icons.Default.Info, title = "关于我们")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = {
                    UserSession.clear()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF0F0)), // Light red background
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4D4F)) // Red border
            ) {
                Text("退出登录", color = Color(0xFFFF4D4F), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showNameDialog) {
        var newName by remember { mutableStateOf(username) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("修改用户名", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        focusedLabelColor = PrimaryBlue,
                        cursorColor = PrimaryBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val response = RetrofitClient.apiService.updateProfile(UpdateProfileRequest(username = newName))
                                if (response.success && response.data != null) {
                                    username = response.data.username
                                    UserSession.username = response.data.username
                                    Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                                    showNameDialog = false
                                } else {
                                    Toast.makeText(context, "修改失败: ${response.message}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "错误: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNameDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextGray)
                ) { Text("取消") }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("重置密码", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                Column {
                    Text("请输入新密码，长度不少于3位", color = TextGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("新密码") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            focusedLabelColor = PrimaryBlue,
                            cursorColor = PrimaryBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.length < 3) {
                            Toast.makeText(context, "密码长度不能少于3位", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            try {
                                val response = RetrofitClient.apiService.updateProfile(UpdateProfileRequest(password = newPassword))
                                if (response.success) {
                                    Toast.makeText(context, "密码修改成功", Toast.LENGTH_SHORT).show()
                                    showPasswordDialog = false
                                } else {
                                    Toast.makeText(context, "修改失败: ${response.message}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "错误: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text("确认修改") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPasswordDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextGray)
                ) { Text("取消") }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showAvatarDialog) {
        val avatars = listOf("default", "face", "person", "star", "favorite", "home")
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = { Text("选择头像", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(avatars) { iconName ->
                        val isSelected = avatar == iconName
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) LightBlue else Color.Transparent)
                                .border(
                                    if (isSelected) 2.dp else 0.dp,
                                    if (isSelected) PrimaryBlue else Color.Transparent,
                                    CircleShape
                                )
                                .clickable {
                                    scope.launch {
                                        try {
                                            val response = RetrofitClient.apiService.updateProfile(UpdateProfileRequest(avatar = iconName))
                                            if (response.success && response.data != null) {
                                                avatar = response.data.avatar ?: "default"
                                                UserSession.avatar = response.data.avatar
                                                Toast
                                                    .makeText(context, "头像已更新", Toast.LENGTH_SHORT)
                                                    .show()
                                                showAvatarDialog = false
                                            }
                                        } catch (e: Exception) {
                                            Toast
                                                .makeText(context, "错误: ${e.message}", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getAvatarIcon(iconName),
                                contentDescription = null,
                                tint = if (isSelected) PrimaryBlue else TextGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAvatarDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextGray)
                ) { Text("关闭") }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

fun getAvatarIcon(name: String): ImageVector {
    return when (name) {
        "face" -> Icons.Default.Face
        "person" -> Icons.Default.Person
        "star" -> Icons.Default.Star
        "favorite" -> Icons.Default.Favorite
        "home" -> Icons.Default.Home
        else -> Icons.Default.AccountCircle
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, contentDescription = null, modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp), tint = PrimaryBlue
            )
            Text(title, fontSize = 16.sp, color = TextDark)
        }
        Icon(
            Icons.Default.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFFC4C4C4),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, trailingText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, contentDescription = null, modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp), tint = PrimaryBlue
            )
            Text(title, fontSize = 16.sp, color = TextDark)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(trailingText, color = TextGray, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFFC4C4C4),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}