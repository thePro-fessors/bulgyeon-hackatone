package com.bulgyeong.safetyapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgyeong.safetyapp.data.api.Checklist
import com.bulgyeong.safetyapp.data.api.RetrofitClient
import com.bulgyeong.safetyapp.data.api.SessionManager
import com.bulgyeong.safetyapp.data.api.StartWorkRequest
import com.bulgyeong.safetyapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastCheckScreen(onNavigateToMain: () -> Unit) {
    var checklists by remember { mutableStateOf<List<Checklist>>(emptyList()) }
    var checkedSet by remember { mutableStateOf(setOf<String>()) }
    var workTime by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isStarting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.getChecklists()
            if (response.success) {
                checklists = response.checklists
            } else {
                Toast.makeText(context, "체크리스트를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    val allChecked = checklists.isNotEmpty() && checkedSet.size == checklists.size && workTime.isNotBlank() && !isStarting

    val handleStartWork = {
        if (allChecked) {
            isStarting = true
            val employeeId = SessionManager.currentUser?.employeeId ?: ""
            val areaId = SessionManager.currentArea?.id ?: ""
            val durationMinutes = workTime.toIntOrNull() ?: 0

            coroutineScope.launch {
                try {
                    val res = RetrofitClient.api.startWork(StartWorkRequest(employeeId, areaId, durationMinutes))
                    if (res.success) {
                        onNavigateToMain()
                    } else {
                        Toast.makeText(context, "작업 시작 실패: ${res.message}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isStarting = false
                }
            }
        }
    }

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
                    Text(SessionManager.currentUser?.name ?: "홍길동", fontSize = 24.sp, color = FigmaBlack, fontWeight = FontWeight.Medium)
                }
            }
        },
        containerColor = FigmaWhite
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FigmaBlack)
            }
        } else {
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
                    
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(checklists) { chk ->
                            val isChecked = checkedSet.contains(chk.id)
                            LastCheckItem(
                                text = chk.text,
                                checked = isChecked,
                                onToggle = {
                                    if (isChecked) {
                                        checkedSet = checkedSet - chk.id
                                    } else {
                                        checkedSet = checkedSet + chk.id
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = handleStartWork,
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
                    if (isStarting) {
                        CircularProgressIndicator(color = FigmaWhite, modifier = Modifier.size(24.dp))
                    } else {
                        Text("작업 시작", fontSize = 20.sp, color = FigmaWhite)
                    }
                }
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
