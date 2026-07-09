package com.bulgyeong.safetyapp.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgyeong.safetyapp.data.api.LoginRequest
import com.bulgyeong.safetyapp.data.api.RetrofitClient
import com.bulgyeong.safetyapp.data.api.User
import com.bulgyeong.safetyapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLogin: (User) -> Unit) {
    var employeeId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

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
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = FigmaBlack),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Login Button
            Button(
                onClick = {
                    if (employeeId.isBlank()) return@Button
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.api.login(LoginRequest(employeeId))
                            if (response.success && response.user != null) {
                                com.bulgyeong.safetyapp.data.api.SessionManager.currentUser = response.user
                                onLogin(response.user)
                            } else {
                                Toast.makeText(context, "로그인 실패: ${response.message}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .width(251.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FigmaBlack),
                shape = RoundedCornerShape(1000.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = FigmaWhite, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "로그인",
                        color = FigmaWhite,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}
