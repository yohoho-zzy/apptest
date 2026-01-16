package com.example.quotepicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.quotepicker.ui.GateScreen
import com.example.quotepicker.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                var passed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                if (passed) {
                    MainScreen()
                } else {
                    GateScreen(onPassed = { passed = true })
                }
            }
        }
    }
}
