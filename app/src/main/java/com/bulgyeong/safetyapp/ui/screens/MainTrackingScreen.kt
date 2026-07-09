package com.bulgyeong.safetyapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgyeong.safetyapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTrackingScreen(onEmergency: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Site", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FigmaYellow,
                    titleContentColor = FigmaBlack
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(FigmaYellow, RoundedCornerShape(1000.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(FigmaWhite, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = FigmaGray)
                    }
                    Text("홍길동", fontSize = 24.sp, color = FigmaBlack, fontWeight = FontWeight.Medium)
                }
            }
        },
        containerColor = FigmaWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 데드맨 스위치 타이머
            Card(
                colors = CardDefaults.cardColors(containerColor = FigmaLightGray),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("안전 타이머", color = FigmaGray)
                    Text(
                        text = "59:42",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = FigmaBlack,
                        letterSpacing = 4.sp
                    )
                    Text("PDR 추적 중", color = NeonGreen)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // SOS 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(AlertRed)
                    .clickable { onEmergency() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = "SOS", tint = FigmaWhite, modifier = Modifier.size(64.dp))
                    Text(
                        "긴급 구조 요청 (SOS)",
                        color = FigmaWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun MainTrackingScreenPreview() {
    BulgyeongSafetyAppTheme {
        MainTrackingScreen(onEmergency = {})
    }
}
