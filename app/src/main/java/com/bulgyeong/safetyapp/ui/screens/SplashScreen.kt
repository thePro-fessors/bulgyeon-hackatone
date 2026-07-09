package com.bulgyeong.safetyapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgyeong.safetyapp.ui.theme.FigmaBlack
import com.bulgyeong.safetyapp.ui.theme.FigmaYellow
import com.bulgyeong.safetyapp.ui.theme.BulgyeongSafetyAppTheme
import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalContext
import com.bulgyeong.safetyapp.data.api.SessionManager

@Composable
fun SplashScreen(onTimeout: (String) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        delay(1500)
        val target = when {
            SessionManager.isWorking(context) -> "main"
            SessionManager.isLoggedIn(context) -> "area_select"
            else -> "login"
        }
        onTimeout(target)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FigmaYellow),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun SplashScreenPreview() {
    BulgyeongSafetyAppTheme {
        SplashScreen(onTimeout = {})
    }
}
