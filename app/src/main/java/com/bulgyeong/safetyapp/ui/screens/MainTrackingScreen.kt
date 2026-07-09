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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgyeong.safetyapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTrackingScreen(onEmergency: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Site", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
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
                .padding(21.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 남은 작업 시간 카드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(FigmaBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("남은 작업 시간", fontSize = 24.sp, color = FigmaWhite)
                    Text("NN : NN : NN", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = FigmaWhite)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("시작시간 : NN:NN:NN", fontSize = 15.sp, color = FigmaWhite)
                        Text("종료시간 : NN:NN:NN", fontSize = 15.sp, color = FigmaWhite)
                    }
                }
            }

            // 위치 추적 및 센서 정보 카드
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(FigmaBlack),
                contentAlignment = Alignment.Center
            ) {
                Text("위치 추적 및 센서 정보", fontSize = 24.sp, color = FigmaWhite, textAlign = TextAlign.Center)
            }
            
            Spacer(modifier = Modifier.weight(1f))

            // 작업 종료 버튼
            Button(
                onClick = { /* 작업 종료 로직 */ },
                modifier = Modifier
                    .width(251.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FigmaBlack),
                shape = RoundedCornerShape(1000.dp)
            ) {
                Text("작업 종료", fontSize = 20.sp, color = FigmaWhite)
            }

            // SOS 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AlertRed)
                    .clickable { onEmergency() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "SOS", tint = FigmaWhite, modifier = Modifier.size(64.dp))
                    Text(
                        "긴급구조요청 (SOS)",
                        color = FigmaWhite,
                        fontSize = 32.sp,
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
