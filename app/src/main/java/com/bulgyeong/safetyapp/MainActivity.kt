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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private val _staticEmergencySignal = MutableSharedFlow<EmergencyType>(extraBufferCapacity = 1)
        val staticEmergencySignal = _staticEmergencySignal.asSharedFlow()

        fun triggerEmergency(type: EmergencyType) {
            _staticEmergencySignal.tryEmit(type)
        }
    }

    private var volumeDownPressJob: Job? = null

    // 처음 시작은 스플래시 화면이므로 기본값을 세팅해둡니다.
    private var currentRoute: String? = "splash"

    // 비상 신호를 전달하기 위한 간단한 SharedFlow입니다.
    private val _emergencySignal = MutableSharedFlow<EmergencyType>(extraBufferCapacity = 1)
    private val emergencySignal = _emergencySignal.asSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.bulgyeong.safetyapp.data.api.SessionManager.init(applicationContext)
        setContent {
            BulgyeongSafetyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SafetyAppNavigation(
                        emergencySignal = emergencySignal,
                        onRouteChanged = { route -> currentRoute = route },
                        onEmergencyTriggered = { type -> triggerEmergencyLogic(type) }
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && currentRoute == "main") {
            if (event?.repeatCount == 0) {
                volumeDownPressJob = CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@MainActivity, "⏳ SOS 3초 카운트다운 시작...", Toast.LENGTH_SHORT).show()
                    delay(3000)
                    triggerEmergencyLogic(EmergencyType.SOS)
                    volumeDownPressJob = null
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && currentRoute == "main") {
            if (volumeDownPressJob?.isActive == true) {
                Toast.makeText(this@MainActivity, "❌ SOS 취소됨", Toast.LENGTH_SHORT).show()
                volumeDownPressJob?.cancel()
                volumeDownPressJob = null
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun triggerEmergencyLogic(type: EmergencyType) {
        val isNetworkAvailable = checkNetworkStatus()

        if (isNetworkAvailable) {
            Toast.makeText(this@MainActivity, "📡 인터넷 연결됨: 서버 전송!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "🚨 통신 두절: BLE 가동!", Toast.LENGTH_SHORT).show()
        }

        _emergencySignal.tryEmit(type)
    }

    private fun checkNetworkStatus(): Boolean {
        return false
    }
}

@Composable
fun SafetyAppNavigation(
    emergencySignal: SharedFlow<EmergencyType>,
    onRouteChanged: (String?) -> Unit,
    onEmergencyTriggered: (EmergencyType) -> Unit
) {
    val navController = rememberNavController()

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            onRouteChanged(destination.route)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    LaunchedEffect(emergencySignal) {
        emergencySignal.collect { type ->
            navController.navigate("emergency/${type.name}")
        }
    }

    LaunchedEffect(Unit) {
        MainActivity.staticEmergencySignal.collect { type ->
            navController.navigate("emergency/${type.name}")
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onTimeout = { targetRoute ->
                    navController.navigate(targetRoute) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLogin = { user ->
                    navController.navigate("area_select") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("area_select") {
            AreaSelectScreen(
                onAreaSelected = { area ->
                    navController.navigate("loto/${area.id}")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
        composable("loto/{areaId}") { backStackEntry ->
            val areaId = backStackEntry.arguments?.getString("areaId") ?: ""
            LotoScreen(
                areaId = areaId,
                onNavigateToMain = {
                    navController.navigate("last_check")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
        composable("last_check") {
            LastCheckScreen(
                onNavigateToMain = {
                    navController.navigate("main") {
                        popUpTo("area_select") { inclusive = true }
                    }
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
        composable("main") {
            MainTrackingScreen(
                onEmergency = { type -> onEmergencyTriggered(type) },
                onWorkEnd = {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
        composable("emergency/{type}") { backStackEntry ->
            val typeStr = backStackEntry.arguments?.getString("type") ?: "SOS"
            val type = try {
                EmergencyType.valueOf(typeStr)
            } catch (e: Exception) {
                EmergencyType.SOS
            }
            EmergencyScreen(
                type = type,
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
    }
}