package com.bulgyeong.safetyapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgyeong.safetyapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastCheckScreen(onNavigateToMain: () -> Unit) {
    var check1 by remember { mutableStateOf(false) }
    var check2 by remember { mutableStateOf(false) }
    var workTime by remember { mutableStateOf("") }
    
    val allChecked = check1 && check2 && workTime.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("최종점검", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("작업시간 설정", fontSize = 28.sp, color = FigmaBlack)
                TextField(
                    value = workTime,
                    onValueChange = { workTime = it },
                    placeholder = { Text("작업 시간을 분단위로 입력하세요", color = FigmaGray, fontSize = 15.sp) },
                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null, tint = FigmaBlack) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(BorderStroke(1.dp, FigmaBlack), RoundedCornerShape(1000.dp)),
                    shape = RoundedCornerShape(1000.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FigmaWhite,
                        unfocusedContainerColor = FigmaWhite,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                
                // Time adjustment buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeButton(text = "+ 30분", modifier = Modifier.weight(1f)) {
                        val current = workTime.toIntOrNull() ?: 0
                        workTime = (current + 30).toString()
                    }
                    TimeButton(text = "- 30분", modifier = Modifier.weight(1f)) {
                        val current = workTime.toIntOrNull() ?: 0
                        workTime = maxOf(0, current - 30).toString()
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeButton(text = "+ 1시간", modifier = Modifier.weight(1f)) {
                        val current = workTime.toIntOrNull() ?: 0
                        workTime = (current + 60).toString()
                    }
                    TimeButton(text = "- 1시간", modifier = Modifier.weight(1f)) {
                        val current = workTime.toIntOrNull() ?: 0
                        workTime = maxOf(0, current - 60).toString()
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("체크리스트", fontSize = 28.sp, color = FigmaBlack)
                
                LastCheckItem(
                    text = "안전 장비를 점검했나요?",
                    checked = check1,
                    onToggle = { check1 = !check1 }
                )
                LastCheckItem(
                    text = "안전줄을 잘 연결했나요?",
                    checked = check2,
                    onToggle = { check2 = !check2 }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNavigateToMain,
                enabled = allChecked,
                modifier = Modifier
                    .width(251.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FigmaBlack,
                    disabledContainerColor = FigmaGray
                ),
                shape = RoundedCornerShape(1000.dp)
            ) {
                Text("작업 시작", fontSize = 20.sp, color = FigmaWhite)
            }
        }
    }
}

@Composable
fun TimeButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .background(FigmaBlack, RoundedCornerShape(1000.dp))
            .clip(RoundedCornerShape(1000.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = FigmaWhite, fontSize = 20.sp)
    }
}

@Composable
fun LastCheckItem(text: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(71.dp)
            .background(FigmaWhite, RoundedCornerShape(1000.dp))
            .border(BorderStroke(1.dp, FigmaBlack), RoundedCornerShape(1000.dp))
            .clip(RoundedCornerShape(1000.dp))
            .clickable { onToggle() }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, color = FigmaBlack, fontSize = 20.sp)
        
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(if (checked) FigmaGreen else FigmaDangerRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (checked) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = FigmaBlack,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun LastCheckScreenPreview() {
    BulgyeongSafetyAppTheme {
        LastCheckScreen(onNavigateToMain = {})
    }
}
