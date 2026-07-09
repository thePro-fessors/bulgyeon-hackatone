package com.bulgyeong.safetyapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgyeong.safetyapp.ui.theme.*

@Composable
fun EmergencyScreen(onCancel: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlertRed.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = FigmaWhite, modifier = Modifier.size(120.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "비상 상황 발동!",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = FigmaWhite
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "낙상이 감지되었거나 데드맨 스위치 타이머가 만료되었습니다.\n\n주변 작업자에게 구조 신호(BLE P2P)를 송신 중입니다.",
                fontSize = 20.sp,
                color = FigmaWhite,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = FigmaBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(1000.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = FigmaWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text("오작동 해제 (스와이프)", fontSize = 18.sp, color = FigmaWhite)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun EmergencyScreenPreview() {
    BulgyeongSafetyAppTheme {
        EmergencyScreen(onCancel = {})
    }
}
