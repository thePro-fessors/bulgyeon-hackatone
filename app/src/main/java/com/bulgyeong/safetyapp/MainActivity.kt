package com.bulgyeong.safetyapp

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bulgyeong.safetyapp.ui.screens.*
import com.bulgyeong.safetyapp.ui.theme.BulgyeongSafetyAppTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainActivity : ComponentActivity() {

    private var volumeDownPressJob: Job? = null
    
    // 💡 처음 시작은 스플래시 화면이므로 기본값을 세팅해둡니다.
    private var currentRoute: String? = "splash"

    // 💡 친구의 순정 설계와 맞추기 위해 간단한 신호(Unit) 통로로 원복합니다.
    private val _emergencySignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val emergencySignal = _emergencySignal.asSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BulgyeongSafetyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafetyAppNavigation(
                        emergencySignal = emergencySignal,
                        onRouteChanged = { route -> currentRoute = route } // 실시간 화면 감시
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // 💡 오직 노란색 헤더의 메인 화면("main")일 때만 SOS 타이머 가동!
            if (currentRoute == "main") {
                if (event?.repeatCount == 0) {
                    volumeDownPressJob = CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(this@MainActivity, "⏳ SOS 3초 카운트다운 시작...", Toast.LENGTH_SHORT).show()
                        delay(3000)
                        triggerEmergencyLogic()
                        volumeDownPressJob = null // 성공 후 타이머 리셋
                    }
                }
                return true // 메인 화면일 땐 폰 시스템 볼륨 바가 뜨지 않게 차단
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (currentRoute == "main") {
                if (volumeDownPressJob?.isActive == true) {
                    Toast.makeText(this@MainActivity, "❌ SOS 취소됨", Toast.LENGTH_SHORT).show()
                    volumeDownPressJob?.cancel()
                }
                volumeDownPressJob = null
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun triggerEmergencyLogic() {
        val isNetworkAvailable = checkNetworkStatus()

        if (isNetworkAvailable) {
            Toast.makeText(this@MainActivity, "📡 인터넷 연결됨: 서버 전송!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "🚨 통신 두절: BLE 가동!", Toast.LENGTH_SHORT).show()
        }

        // 화면 쪽에 비상 신호 발생 알림!
        _emergencySignal.tryEmit(Unit)
    }

    private fun checkNetworkStatus(): Boolean {
        return false 
    }
}


// ====================================================================
// 친구의 원래 내비게이션 규칙을 100% 순정 복원한 코드입니다.
// ====================================================================
@Composable
fun SafetyAppNavigation(
    emergencySignal: SharedFlow<Unit>,
    onRouteChanged: (String?) -> Unit
) {
    val navController = rememberNavController()
    
    // 실시간 화면 위치 추적 레이더
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            onRouteChanged(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    // 비상 신호 감시 및 화면 이동 처리
    LaunchedEffect(emergencySignal) {
        emergencySignal.collect {
            // 💡 친구분이 만든 원래 순정 주소인 "emergency"로 안전하게 이동시킵니다!
            navController.navigate("emergency")
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onTimeout = { navController.navigate("login") { popUpTo("splash") { inclusive = true } } })
        }
        composable("login") {
            LoginScreen(onLogin = { navController.navigate("area_select") { popUpTo("login") { inclusive = true } } })
        }
        composable("area_select") {
            AreaSelectScreen(onAreaSelected = { area -> navController.navigate("loto") })
        }
        composable("loto") {
            LotoScreen(onNavigateToMain = { navController.navigate("last_check") })
        }
        composable("last_check") {
            LastCheckScreen(onNavigateToMain = { navController.navigate("main") { popUpTo("area_select") { inclusive = true } } })
        }
        composable("main") {
            // 노란색 헤더의 작업 추적 화면 (여기서만 볼륨 버튼 작동)
            MainTrackingScreen(onEmergency = { navController.navigate("emergency") })
        }
        composable("emergency") {
            // 💡 친구분이 만들어둔 순정 비상 화면을 그대로 띄웁니다!
            // 세 번째 SOS 화면 디자인은 친구가 이 화면 내부에서 알아서 처리해 줄 것입니다.
            EmergencyScreen(onCancel = { navController.popBackStack() })
        }
    }
}