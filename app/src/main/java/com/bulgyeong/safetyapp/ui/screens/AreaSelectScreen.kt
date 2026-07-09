package com.bulgyeong.safetyapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bulgyeong.safetyapp.data.api.Area
import com.bulgyeong.safetyapp.data.api.RetrofitClient
import com.bulgyeong.safetyapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaSelectScreen(onAreaSelected: (Area) -> Unit) {
    var areas by remember { mutableStateOf<List<Area>>(emptyList()) }
    var showDangerDialog by remember { mutableStateOf(false) }
    var selectedDangerArea by remember { mutableStateOf<Area?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.api.getAreas()
            if (response.success) {
                areas = response.areas
            } else {
                Toast.makeText(context, "구역 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("작업구역 선택", fontWeight = FontWeight.Bold) },
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
                    Text(com.bulgyeong.safetyapp.data.api.SessionManager.currentUser?.name ?: "홍길동", fontSize = 24.sp, color = FigmaBlack, fontWeight = FontWeight.Medium)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 21.dp, vertical = 31.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(areas) { area ->
                    LocationItem(
                        name = area.name,
                        isDanger = area.isDanger == 1,
                        onClick = {
                            if (area.isDanger == 1) {
                                selectedDangerArea = area
                                showDangerDialog = true
                            } else {
                                com.bulgyeong.safetyapp.data.api.SessionManager.currentArea = area
                                onAreaSelected(area)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDangerDialog && selectedDangerArea != null) {
        Dialog(onDismissRequest = { showDangerDialog = false }) {
            Box(
                modifier = Modifier
                    .width(366.dp)
                    .background(FigmaWhite, RoundedCornerShape(20.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = "⚠️ 주의! ⚠️",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = FigmaBlack
                    )
                    
                    Text(
                        text = "선택한 작업구역은 위험 지역임.\n안전 장구를 점검할 것.",
                        fontSize = 24.sp,
                        color = FigmaBlack,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    
                    Button(
                        onClick = {
                            showDangerDialog = false
                            com.bulgyeong.safetyapp.data.api.SessionManager.currentArea = selectedDangerArea
                            onAreaSelected(selectedDangerArea!!)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(1000.dp)
                    ) {
                        Text("확인", fontSize = 20.sp, color = FigmaWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationItem(name: String, isDanger: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(71.dp)
            .background(
                color = if (isDanger) AlertRed else FigmaYellow,
                shape = RoundedCornerShape(1000.dp)
            )
            .clip(RoundedCornerShape(1000.dp))
            .clickable { onClick() }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isDanger) Icons.Default.Warning else Icons.Default.LocationOn,
                contentDescription = null,
                tint = FigmaBlack,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                fontSize = 24.sp,
                color = FigmaBlack
            )
        }
    }
}
