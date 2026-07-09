package com.bulgyeong.safetyapp.ui.screens

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgyeong.safetyapp.ui.theme.*

enum class EmergencyType(val message: String) {
    SOS("SOS 버튼 작동!"),
    DEADMAN("데드맨 스위치 작동!"),
    FALL("낙상이 감지되었습니다!")
}

@Composable
fun EmergencyScreen(type: EmergencyType = EmergencyType.SOS, onCancel: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(type) {
        val employeeId = com.bulgyeong.safetyapp.data.api.SessionManager.currentUser?.employeeId ?: return@LaunchedEffect
        try {
            com.bulgyeong.safetyapp.data.api.RetrofitClient.api.reportEmergency(com.bulgyeong.safetyapp.data.api.EmergencyRequest(employeeId, type.name))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "비상 보고 실패: \${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "siren")
    val bgColor by infiniteTransition.animateColor(
        initialValue = AlertRed,
        targetValue = Color(0xFFA00000), // 조금 어두운 빨간색 (깜빡임 효과)
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = FigmaWhite,
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "비상 상황 발생!",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = FigmaWhite,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = type.message,
                fontSize = 24.sp,
                color = FigmaWhite,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "구조 요청 전파 중...",
                fontSize = 20.sp,
                color = FigmaWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Gemma 2B AI 구조 마니페스트 박스
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x44000000), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = """
                        === 🤖 edge-gemma-2b: RESCUE_MANIFEST ===
                        [위급 상태 분석 리포트]
                        - 검출 이벤트: ${type.message}
                        - 최종 작업자: ${com.bulgyeong.safetyapp.data.api.SessionManager.currentUser?.name ?: "작업자"} (${com.bulgyeong.safetyapp.data.api.SessionManager.currentUser?.employeeId ?: "ID 미확인"})
                        - 비상 통신: BLE P2P Broadcast 자동 가동 완료
                        - 최후 추측 위치: Lat 35.1595, Lon 129.0430 (부산 기지)
                        
                        [대원 전파 구조 지침]
                        1. 대상 구역의 2차 전원 차단을 필히 유선 재차 확인하십시오.
                        2. 폰에서 송출 중인 BLE 비상 RSSI 신호 세기(TxPower: HIGH)를 스캐너로 수집하여 실시간 거리 역삼각측량을 시도하십시오.
                    """.trimIndent(),
                    fontSize = 11.sp,
                    color = FigmaWhite,
                    lineHeight = 16.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onCancel,
            modifier = Modifier
                .width(251.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FigmaBlack),
            shape = RoundedCornerShape(1000.dp)
        ) {
            Text("오작동 해제(안전함)", fontSize = 20.sp, color = FigmaWhite)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun EmergencyScreenPreview() {
    BulgyeongSafetyAppTheme {
        EmergencyScreen(type = EmergencyType.DEADMAN, onCancel = {})
    }
}
