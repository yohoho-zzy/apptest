package com.example.quotepicker.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private enum class MainTab(val title: String, val icon: ImageVector) {
    TAG("标签", Icons.Default.LocalOffer),
    CHARACTER("角色", Icons.Default.Person),
    RESOURCE("资源", Icons.Default.Folder),
    EXECUTION("执行", Icons.Default.Casino)
}

@Composable
fun MainScreen() {
    var currentTab by remember { mutableStateOf(MainTab.TAG) }
    val tabs = remember { MainTab.values().toList() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { inner ->
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(inner)
        ) {
            when (currentTab) {
                MainTab.TAG -> TagScreen()
                MainTab.CHARACTER -> CharacterScreen()
                MainTab.RESOURCE -> ResourceScreen()
                MainTab.EXECUTION -> ExecutionScreen()
            }
        }
    }
}
