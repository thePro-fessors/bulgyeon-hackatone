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

import android.widget.Toast

import com.bulgyeong.safetyapp.data.api.SessionManager
import com.bulgyeong.safetyapp.sensor.DeadManSwitchManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTrackingScreen(
    onEmergency: (com.bulgyeong.safetyapp.ui.screens.EmergencyType) -> Unit,
    onWorkEnd: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasPermissions by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf("") }

    // 작업 예정 시간 세션 데이터 로드
    var startTime by remember { mutableStateOf(SessionManager.getStartTimeMillis(context)) }
    var durationMins by remember { mutableStateOf(SessionManager.getDurationMinutes(context)) }
    var remainingSeconds by remember { mutableStateOf(0L) }

    // 데드맨 대화상자 관련 상태
    var showDeadManDialog by remember { mutableStateOf(false) }
    var deadManTimeoutSeconds by remember { mutableStateOf(60) }

    // 실시간 잔여시간 계산 루프
    LaunchedEffect(startTime, durationMins, showDeadManDialog) {
        while (true) {
            val endTime = startTime + durationMins * 60 * 1000L
            val current = System.currentTimeMillis()
            val diff = (endTime - current) / 1000
            
            if (diff <= 0) {
                remainingSeconds = 0L
                if (!showDeadManDialog) {
                    showDeadManDialog = true
                }
            } else {
                remainingSeconds = diff
            }
            delay(1000)
        }
    }

    // 데드맨 타임아웃 타이머
    LaunchedEffect(showDeadManDialog) {
        if (showDeadManDialog) {
            deadManTimeoutSeconds = 60
            while (deadManTimeoutSeconds > 0) {
                delay(1000)
                deadManTimeoutSeconds--
            }
            // 1분간 터치/움직임 미감지 시 -> 자동 비상 모드 가동
            showDeadManDialog = false
            onEmergency(com.bulgyeong.safetyapp.ui.screens.EmergencyType.DEADMAN)
        }
    }

    // 작업 시간 10분 연장 및 알림 해제 로직
    val handleDismissAndExtend = {
        showDeadManDialog = false
        SessionManager.addDuration(context, 10)
        durationMins = SessionManager.getDurationMinutes(context)
        Toast.makeText(context, "⏰ 작업 시간이 10분 연장되었습니다.", Toast.LENGTH_SHORT).show()
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))

        // 서버 연장 API 비동기 보고
        val employeeId = SessionManager.currentUser?.employeeId
        if (employeeId != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    com.bulgyeong.safetyapp.data.api.RetrofitClient.api.extendWork(
                        com.bulgyeong.safetyapp.data.api.ExtendWorkRequest(employeeId, 10)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // 가속도 센서를 이용한 움직임(흔들림) 감지 리스너 등록
    DisposableEffect(showDeadManDialog) {
        val manager = DeadManSwitchManager(context) {
            // 움직임(흔들림)이 감지되면 연장 처리
            if (showDeadManDialog) {
                handleDismissAndExtend()
            }
        }
        if (showDeadManDialog) {
            manager.start()
        }
        onDispose {
            manager.stop()
        }
    }

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

    // 타임스탬프 포맷팅 도구
    fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "00:00:00"
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDuration(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        .clickable {
                            // 자기 이름 누르면 로그아웃 (위치 서비스 정지 및 세션 초기화)
                            val intent = Intent(context, LocationTrackingService::class.java)
                            context.stopService(intent)
                            
                            // 서버 종료 API 비동기 보고
                            val employeeId = SessionManager.currentUser?.employeeId
                            if (employeeId != null) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        com.bulgyeong.safetyapp.data.api.RetrofitClient.api.endWork(
                                            com.bulgyeong.safetyapp.data.api.EndWorkRequest(employeeId)
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                            
                            SessionManager.endWork(context)
                            onWorkEnd()
                        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(21.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 남은 작업 시간 카드 (실시간 값 연동)
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
                        Text(formatDuration(remainingSeconds), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = FigmaWhite)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("시작시간 : ${formatTimestamp(startTime)}", fontSize = 15.sp, color = FigmaWhite)
                            Text("종료시간 : ${formatTimestamp(startTime + durationMins * 60 * 1000L)}", fontSize = 15.sp, color = FigmaWhite)
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

                        // 서버 종료 API 비동기 보고
                        val employeeId = SessionManager.currentUser?.employeeId
                        if (employeeId != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    com.bulgyeong.safetyapp.data.api.RetrofitClient.api.endWork(
                                        com.bulgyeong.safetyapp.data.api.EndWorkRequest(employeeId)
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        SessionManager.endWork(context)
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
                        .clickable { onEmergency(com.bulgyeong.safetyapp.ui.screens.EmergencyType.SOS) },
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

        // 데드맨 스위치 강제 알림 다이얼로그 오버레이 (의식 상실 경고)
        if (showDeadManDialog) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Box(
                    modifier = Modifier
                        .width(340.dp)
                        .height(400.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AlertRed)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Danger",
                            tint = FigmaWhite,
                            modifier = Modifier.size(80.dp)
                        )
                        Text(
                            text = "안전 확인 반응 대기",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = FigmaWhite
                        )
                        Text(
                            text = "작업 예정 시간이 만료되었습니다.\n의식이 있으신 경우 아래 버튼을 터치하거나 기기를 흔들어 주세요.",
                            fontSize = 14.sp,
                            color = FigmaWhite.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${deadManTimeoutSeconds}초 후 자동 구조 요청 발송",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FigmaYellow
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { handleDismissAndExtend() },
                            colors = ButtonDefaults.buttonColors(containerColor = FigmaWhite, contentColor = AlertRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("저 괜찮습니다 (안전 확인)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
