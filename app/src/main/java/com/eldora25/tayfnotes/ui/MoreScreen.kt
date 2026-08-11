package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onScreenChange: (Screen) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Diğer", style = MaterialTheme.typography.headlineMedium) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle, 
                        contentDescription = null, 
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Tayfun Yamak", style = MaterialTheme.typography.titleLarge)
                        Text("tayfunyamak@gmail.com", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val menuItems = listOf(
                MenuItem("Tüm notlar", Icons.Default.Description, Screen.Main),
                MenuItem("Arşiv", Icons.Default.Archive, Screen.Archive),
                MenuItem("Çöp Kutusu", Icons.Default.Delete, Screen.Trash),
                MenuItem("Dahili Tarayıcı", Icons.Default.Public, Screen.InternalBrowser),
                MenuItem("Tema", Icons.Default.Palette, Screen.ThemeSelection), // Fixed Madde 11
                MenuItem("Ayarlar", Icons.Default.Settings, Screen.Settings)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(menuItems) { item ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onScreenChange(item.screen) }
                            .padding(8.dp)
                    ) {
                        Icon(item.icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.title, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

data class MenuItem(val title: String, val icon: ImageVector, val screen: Screen)
