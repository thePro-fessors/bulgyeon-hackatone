package com.bulgyeong.safetyapp

import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bulgyeong.safetyapp.ui.screens.*
import com.bulgyeong.safetyapp.ui.theme.BulgyeongSafetyAppTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    private var volumeDownPressJob: Job? = null
    private val _emergencySignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BulgyeongSafetyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafetyAppNavigation()
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 현재 탑 레벨에서 route를 알기 어렵지만, 간단하게 구현
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event?.repeatCount == 0) {
                volumeDownPressJob = CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@MainActivity, "⏳ SOS 3초 카운트다운 시작...", Toast.LENGTH_SHORT).show()
                    delay(3000)
                    triggerEmergencyLogic()
                    volumeDownPressJob = null
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (volumeDownPressJob?.isActive == true) {
                Toast.makeText(this@MainActivity, "❌ SOS 취소됨", Toast.LENGTH_SHORT).show()
                volumeDownPressJob?.cancel()
                volumeDownPressJob = null
            }
            return true
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

        _emergencySignal.tryEmit(Unit)
    }

    private fun checkNetworkStatus(): Boolean {
        return false
    }
}

@Composable
fun SafetyAppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onTimeout = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLogin = {
                    navController.navigate("area_select") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("area_select") {
            AreaSelectScreen(
                onAreaSelected = { areaId ->
                    navController.navigate("loto/$areaId")
                }
            )
        }

        composable("loto/{areaId}") { backStackEntry ->
            val areaId = backStackEntry.arguments?.getString("areaId") ?: ""
            LotoScreen(
                areaId = areaId,
                onNavigateToMain = {
                    navController.navigate("last_check")
                }
            )
        }

        composable("last_check") {
            LastCheckScreen(
                onNavigateToMain = { duration ->
                    navController.navigate("main/$duration") {
                        popUpTo("area_select") { inclusive = true }
                    }
                }
            )
        }

        composable("main/{duration}") { backStackEntry ->
            val duration = backStackEntry.arguments?.getString("duration")?.toIntOrNull() ?: 60
            MainTrackingScreen(
                initialMinutes = duration,
                onEmergency = {
                    navController.navigate("emergency")
                }
            )
        }

        composable("dead_man") {
            DeadManScreen(
                onSafeConfirmed = {
                    navController.popBackStack()
                },
                onTimeoutExpired = {
                    navController.navigate("emergency") {
                        popUpTo("dead_man") { inclusive = true }
                    }
                }
            )
        }

        composable("emergency") {
            EmergencyScreen(
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
    }
}
