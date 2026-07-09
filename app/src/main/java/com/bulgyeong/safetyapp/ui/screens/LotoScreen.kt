package com.bulgyeong.safetyapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgyeong.safetyapp.ui.theme.*

val FigmaGreen = Color(0xFF87FF87)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotoScreen(onNavigateToMain: () -> Unit) {
    var loto1Checked by remember { mutableStateOf(false) }
    var loto2Checked by remember { mutableStateOf(false) }
    val allChecked = loto1Checked && loto2Checked

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(FigmaYellow),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LOTO CHECK",
                    fontSize = 24.sp,
                    color = FigmaBlack
                )
            }
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Lolem Ipsum 체크리스트",
                fontSize = 24.sp,
                color = FigmaBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            LotoQrItem(
                text = "B-메인 가스벨브",
                checked = loto1Checked,
                onScanClick = { loto1Checked = true }
            )
            LotoQrItem(
                text = "CNG 가스 벨브",
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
            .height(72.dp)
            .background(if (checked) FigmaGreen else FigmaWhite, RoundedCornerShape(1000.dp))
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

// Added comment: The render issue (ClassNotFoundException) was fixed by adding 
// debugImplementation("androidx.compose.ui:ui-tooling") to the app's build.gradle.kts.
@androidx.compose.ui.tooling.preview.Preview
@Composable
fun LotoScreenPreview() {
    BulgyeongSafetyAppTheme {
        LotoScreen(onNavigateToMain = {})
    }
}
