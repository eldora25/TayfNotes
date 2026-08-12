package com.eldora25.tayfnotes.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.eldora25.tayfnotes.Screen

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onScreenChange: (Screen) -> Unit,
    onNotesClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Notlar") },
            label = { Text("Notlar") },
            selected = currentScreen is Screen.Main,
            onClick = { 
                onNotesClick()
                onScreenChange(Screen.Main) 
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = "Klasörler") },
            label = { Text("Klasörler") },
            selected = currentScreen is Screen.Folders,
            onClick = { onScreenChange(Screen.Folders) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Takvim") },
            label = { Text("Takvim") },
            selected = currentScreen is Screen.Calendar,
            onClick = { onScreenChange(Screen.Calendar) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
            label = { Text("Ara") },
            selected = false,
            onClick = { 
                // Toggle search in main screen if already there, or go to main
                onScreenChange(Screen.Main)
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Menu, contentDescription = "Menü") },
            label = { Text("Menü") },
            selected = false,
            onClick = onMenuClick
        )
    }
}
