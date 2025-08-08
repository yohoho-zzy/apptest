package com.example.quotepicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.quotepicker.ui.GateScreen
import com.example.quotepicker.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "gate") {
                    composable("gate") {
                        GateScreen(onPassed = {
                            nav.navigate("main") { popUpTo("gate") { inclusive = true } }
                        })
                    }
                    composable("main") { MainScreen() }
                }
            }
        }
    }
}