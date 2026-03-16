package com.example.offermatrix.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/**
 * 语音波纹动画组件
 * @param isListening 是否正在聆听用户说话
 * @param isSpeaking 是否正在播放AI回复
 * @param modifier Modifier
 */
@Composable
fun VoiceWaveAnimation(
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isActive = isListening || isSpeaking
    val primaryColor = if (isListening) Color(0xFF4CAF50) else Color(0xFF4D6FFF)
    
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveProgress"
    )

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // 外圈脉冲光晕
        if (isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                val maxRadius = size.minDimension / 2
                
                // 多层脉冲圆环
                for (i in 0..2) {
                    val phase = (animatedProgress + i * 0.33f) % 1f
                    val radius = maxRadius * (0.4f + 0.6f * phase)
                    val alpha = (1f - phase) * 0.3f
                    
                    drawCircle(
                        color = primaryColor.copy(alpha = alpha),
                        radius = radius,
                        center = center
                    )
                }
            }
        }

        // 中心渐变圆球
        Canvas(modifier = Modifier.size(120.dp)) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2
            
            val scale = if (isActive) 0.9f + 0.1f * sin(animatedProgress * Math.PI * 2).toFloat() else 1f
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.6f),
                        Color(0xFF1A1A2E)
                    ),
                    center = center,
                    radius = radius * scale
                ),
                radius = radius * scale,
                center = center
            )
        }

        // 波形条动画
        if (isActive) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(5) { i ->
                    val heightScale = if (isActive) {
                        0.3f + 0.7f * abs(sin((animatedProgress * 10 + i * 0.8f).toDouble())).toFloat()
                    } else {
                        0.3f
                    }
                    
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(50.dp * heightScale)
                            .background(
                                color = Color.White.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
        }
    }
}

/**
 * 简化的圆形脉冲动画 - 用于空闲状态
 */
@Composable
fun IdlePulseAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "idle")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * pulseScale
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4D6FFF).copy(alpha = 0.3f),
                        Color(0xFF1A1A2E)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            
            drawCircle(
                color = Color(0xFF4D6FFF),
                radius = radius * 0.5f,
                center = center
            )
        }
    }
}
