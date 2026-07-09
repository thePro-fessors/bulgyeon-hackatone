package com.bulgyeong.safetyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bulgyeong.safetyapp.ui.screens.*
import com.bulgyeong.safetyapp.ui.theme.BulgyeongSafetyAppTheme

class MainActivity : ComponentActivity() {
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
                onAreaSelected = { area ->
                    // area 정보를 넘길 수 있지만 디자인만 구현하므로 넘어감
                    navController.navigate("loto")
                }
            )
        }
        composable("loto") {
            LotoScreen(
                onNavigateToMain = {
                    navController.navigate("last_check")
                }
            )
        }
        composable("last_check") {
            LastCheckScreen(
                onNavigateToMain = {
                    navController.navigate("main") {
                        popUpTo("area_select") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainTrackingScreen(
                onEmergency = {
                    navController.navigate("emergency")
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
