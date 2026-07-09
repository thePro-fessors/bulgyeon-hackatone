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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
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
import com.bulgyeong.safetyapp.data.api.Loto
import com.bulgyeong.safetyapp.data.api.RetrofitClient
import com.bulgyeong.safetyapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotoScreen(areaId: String, onNavigateToMain: () -> Unit) {
    var lotos by remember { mutableStateOf<List<Loto>>(emptyList()) }
    var checkedSet by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current

    LaunchedEffect(areaId) {
        try {
            val response = RetrofitClient.api.getLotos(areaId)
            if (response.success) {
                lotos = response.lotos
            } else {
                Toast.makeText(context, "LOTO 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    val allChecked = lotos.isNotEmpty() && checkedSet.size == lotos.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LOTO CHECK", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FigmaBlack)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 21.dp, vertical = 31.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(lotos) { loto ->
                        val isChecked = checkedSet.contains(loto.id)
                        LotoQrItem(
                            text = loto.text,
                            checked = isChecked,
                            onScanClick = {
                                // 임시로 바로 체크되게 처리 (실제로는 카메라/QR 연동 필요)
                                checkedSet = checkedSet + loto.id
                            }
                        )
                    }
                }

                Button(
                    onClick = onNavigateToMain,
                    enabled = allChecked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FigmaBlack,
                        disabledContainerColor = FigmaGray
                    ),
                    shape = RoundedCornerShape(1000.dp)
                ) {
                    Text("안전 조치 완료", fontSize = 20.sp, color = FigmaWhite)
                }
            }
        }
    }
}

@Composable
fun LotoQrItem(text: String, checked: Boolean, onScanClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(if (checked) NeonGreen else FigmaWhite, RoundedCornerShape(1000.dp))
            .border(BorderStroke(2.dp, FigmaBlack), RoundedCornerShape(1000.dp))
            .clip(RoundedCornerShape(1000.dp))
            .clickable(enabled = !checked) { onScanClick() }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = FigmaBlack,
            fontSize = 22.sp
        )
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(FigmaBlack, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(Icons.Default.Check, contentDescription = "Checked", tint = FigmaWhite, modifier = Modifier.size(32.dp))
            } else {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = FigmaWhite, modifier = Modifier.size(28.dp))
            }
        }
    }
}
