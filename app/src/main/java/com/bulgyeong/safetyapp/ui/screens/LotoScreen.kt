package com.bulgyeong.safetyapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgyeong.safetyapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotoScreen(onNavigateToMain: () -> Unit) {
    var loto1Checked by remember { mutableStateOf(false) }
    var loto2Checked by remember { mutableStateOf(false) }
    val allChecked = loto1Checked && loto2Checked

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LOTO 체크리스트", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FigmaYellow,
                    titleContentColor = FigmaBlack
                )
            )
        },
        containerColor = FigmaWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("B-Deck 메인 밸브실", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FigmaBlack)
            Text("작업 전 아래 항목들을 QR 스캔하여 안전을 확인하세요.", color = FigmaGray)

            Spacer(modifier = Modifier.height(16.dp))

            LotoQrItem(
                text = "메인 전원 차단 (Lockout)",
                checked = loto1Checked,
                onScanClick = { loto1Checked = true }
            )
            LotoQrItem(
                text = "가스 밸브 잠금 및 Tagout",
                checked = loto2Checked,
                onScanClick = { loto2Checked = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNavigateToMain,
                enabled = allChecked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FigmaBlack,
                    disabledContainerColor = FigmaGray
                ),
                shape = RoundedCornerShape(1000.dp)
            ) {
                Text(
                    "작업 시작",
                    fontSize = 20.sp,
                    color = FigmaWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LotoQrItem(text: String, checked: Boolean, onScanClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (checked) NeonGreen.copy(alpha = 0.2f) else FigmaLightGray, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                color = FigmaBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            if (checked) {
                Text("확인 완료", color = NeonGreen, fontWeight = FontWeight.Bold)
            }
        }
        
        if (checked) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Checked", tint = NeonGreen, modifier = Modifier.size(40.dp))
        } else {
            IconButton(
                onClick = onScanClick,
                modifier = Modifier
                    .background(FigmaYellow, RoundedCornerShape(8.dp))
                    .size(48.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = FigmaBlack)
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun LotoScreenPreview() {
    BulgyeongSafetyAppTheme {
        LotoScreen(onNavigateToMain = {})
    }
}
