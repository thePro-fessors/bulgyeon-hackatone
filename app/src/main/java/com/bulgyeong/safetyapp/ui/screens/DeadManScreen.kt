package com.bulgyeong.safetyapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun DeadManScreen(onSafeConfirmed: () -> Unit, onTimeoutExpired: () -> Unit) {
    var countDownSeconds by remember { mutableStateOf(60) } // 60초 카운트다운

    // 1초마다 숫자가 자동으로 줄어드는 타이머 로직
    LaunchedEffect(Unit) {
        while (countDownSeconds > 0) {
            delay(1000)
            countDownSeconds--
        }
        // 0초가 되면(미반응) 5번 SOS 비상 화면으로 이동
        onTimeoutExpired()
    }

    // 데드맨 타임아웃 경고 화면 레이아웃
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF121214)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "⚠️ 님 뒤짐", 
                color = Color(0xFFFFCC00), 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = String.format("00:%02d", countDownSeconds), 
                color = Color.White, 
                fontSize = 54.sp, 
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            // 생존 인증 버튼
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(Color(0xFF2C2C2E), shape = CircleShape)
                    .clickable { onSafeConfirmed() }, // 누르면 원래 화면으로 복귀
                contentAlignment = Alignment.Center
            ) {
                Text("안전함 인증\n(터치)", color = Color(0xFF00C781), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}