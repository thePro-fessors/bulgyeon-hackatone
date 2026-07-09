package com.bulgyeong.safetyapp.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bulgyeong.safetyapp.location.LocationTrackingService
import com.bulgyeong.safetyapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTrackingScreen(
    onEmergency: () -> Unit,
    onWorkEnd: () -> Unit
) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf("") }

    val foregroundPermissions = remember {
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val backgroundPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            emptyList()
        }
    }

    fun startLocationService() {
        val intent = Intent(context, LocationTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            hasPermissions = true
            permissionMessage = "모든 권한이 허용되었습니다."
            startLocationService()
        } else {
            hasPermissions = false
            permissionMessage = "백그라운드 위치 권한이 필요합니다. 설정에서 권한을 허용해주세요."
        }
    }

    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            if (backgroundPermissions.isNotEmpty()) {
                permissionMessage = "백그라운드 위치 권한을 추가로 허용해야 합니다."
                backgroundPermissionLauncher.launch(backgroundPermissions.toTypedArray())
            } else {
                hasPermissions = true
                startLocationService()
            }
        } else {
            hasPermissions = false
            permissionMessage = "위치 및 블루투스 권한을 허용해야 추적이 가능합니다."
        }
    }

    fun checkForegroundPermissions(): Boolean {
        return foregroundPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkBackgroundPermission(): Boolean {
        return backgroundPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

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
                    Text(com.bulgyeong.safetyapp.data.api.SessionManager.currentUser?.name ?: "홍길동", fontSize = 24.sp, color = FigmaBlack, fontWeight = FontWeight.Medium)
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

            Button(
                onClick = {
                    when {
                        !checkForegroundPermissions() -> {
                            permissionMessage = "먼저 위치 및 블루투스 권한을 요청합니다."
                            foregroundPermissionLauncher.launch(foregroundPermissions.toTypedArray())
                        }
                        backgroundPermissions.isNotEmpty() && !checkBackgroundPermission() -> {
                            permissionMessage = "백그라운드 위치 권한을 요청합니다."
                            backgroundPermissionLauncher.launch(backgroundPermissions.toTypedArray())
                        }
                        else -> {
                            hasPermissions = true
                            startLocationService()
                        }
                    }
                },
                modifier = Modifier
                    .width(251.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FigmaBlack),
                shape = RoundedCornerShape(1000.dp)
            ) {
                Text("위치 추적 시작", fontSize = 20.sp, color = FigmaWhite)
            }

            if (permissionMessage.isNotEmpty()) {
                Text(
                    permissionMessage,
                    color = FigmaBlack,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val intent = Intent(context, LocationTrackingService::class.java)
                    context.stopService(intent)
                    com.bulgyeong.safetyapp.data.api.SessionManager.endWork(context)
                    onWorkEnd()
                },
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
        MainTrackingScreen(onEmergency = {}, onWorkEnd = {})
    }
}
