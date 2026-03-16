package com.example.offermatrix.ui.screens.interview

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.offermatrix.interview.InterviewCallActivity

@Composable
fun InterviewPage(navController: NavController) {
    val context = LocalContext.current
    var hasLaunched by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startAIInterview(context)
            // 返回上一页，防止用户按返回键回到这个空白页
            navController.popBackStack()
        } else {
            permissionDenied = true
            Toast.makeText(context, "请授予录音权限以进行面试", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    // 页面加载时自动检查权限并启动面试
    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            hasLaunched = true
            val permissionsToCheck = arrayOf(
                android.Manifest.permission.RECORD_AUDIO
            )
            val missingPermissions = permissionsToCheck.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missingPermissions.isEmpty()) {
                startAIInterview(context)
                navController.popBackStack()
            } else {
                permissionLauncher.launch(permissionsToCheck)
            }
        }
    }

    // 显示加载状态
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "正在启动面试...",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun startAIInterview(context: Context) {
    val intent = Intent(context, InterviewCallActivity::class.java)
    context.startActivity(intent)
}
