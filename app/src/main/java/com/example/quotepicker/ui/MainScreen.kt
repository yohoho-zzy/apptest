package com.example.quotepicker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding

@Composable
fun MainScreen() {
    var currentTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.LocalOffer, contentDescription = null) },
                    label = { Text("标签") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("角色") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                    label = { Text("资源") }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Casino, contentDescription = null) },
                    label = { Text("随机") }
                )
            }
        }
    ) { inner ->
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier) {
            when (currentTab) {
                0 -> TagScreen(modifier = Modifier.padding(inner))
                1 -> CharacterScreen(modifier = Modifier.padding(inner))
                2 -> ResourceScreen(modifier = Modifier.padding(inner))
                else -> RandomScreen(modifier = Modifier.padding(inner))
            }
        }
    }
}
