package com.bulgyeong.safetyapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgyeong.safetyapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var employeeId by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FigmaYellow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            
            // Logo
            Icon(
                imageVector = Icons.Default.Engineering,
                contentDescription = "Logo",
                modifier = Modifier.size(150.dp),
                tint = FigmaBlack
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Safety Site",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = FigmaBlack
            )
            
            Spacer(modifier = Modifier.height(80.dp))
            
            // Input Field
            TextField(
                value = employeeId,
                onValueChange = { employeeId = it },
                placeholder = { 
                    Text(
                        "사원 번호를 입력하세요", 
                        color = FigmaGray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(1000.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FigmaWhite,
                    unfocusedContainerColor = FigmaWhite,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = FigmaBlack)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Login Button
            Button(
                onClick = onLogin,
                modifier = Modifier
                    .width(251.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FigmaBlack),
                shape = RoundedCornerShape(1000.dp)
            ) {
                Text(
                    text = "로그인",
                    color = FigmaWhite,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun LoginScreenPreview() {
    BulgyeongSafetyAppTheme {
        LoginScreen(onLogin = {})
    }
}
