package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.BuildConfig
import com.eldora25.tayfnotes.R
import com.eldora25.tayfnotes.Screen

@Composable
fun NavigationDrawerContent(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    onPlaceholderSelected: (String) -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            DrawerHeader()

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            // Menu Items
            DrawerItem(
                icon = Icons.AutoMirrored.Filled.List,
                label = "Notlar",
                isSelected = currentScreen is Screen.Main,
                onClick = { onScreenSelected(Screen.Main) }
            )
            DrawerItem(
                icon = Icons.Default.Gesture,
                label = "Skeçler",
                isSelected = false,
                onClick = { onPlaceholderSelected("Skeçler") }
            )
            DrawerItem(
                icon = Icons.Default.Checklist,
                label = "Kontrol Listesi",
                isSelected = false,
                onClick = { onPlaceholderSelected("Kontrol Listesi") }
            )
            DrawerItem(
                icon = Icons.Default.Folder,
                label = "Klasörler",
                isSelected = currentScreen is Screen.Folders,
                onClick = { onScreenSelected(Screen.Folders) }
            )
            DrawerItem(
                icon = Icons.Default.CalendarMonth,
                label = "Takvim",
                isSelected = currentScreen is Screen.Calendar,
                onClick = { onScreenSelected(Screen.Calendar) }
            )
            DrawerItem(
                icon = Icons.Default.Search,
                label = "Arama",
                isSelected = false,
                onClick = { onScreenSelected(Screen.Main) } // Search is in Main top bar
            )

            Spacer(modifier = Modifier.height(8.dp))
            DrawerSectionHeader("Kütüphane")

            DrawerItem(
                icon = Icons.Default.AllInclusive,
                label = "Tüm Notlar",
                isSelected = false,
                onClick = { onPlaceholderSelected("Tüm Notlar") }
            )
            DrawerItem(
                icon = Icons.Default.Archive,
                label = "Arşiv",
                isSelected = currentScreen is Screen.Archive,
                onClick = { onScreenSelected(Screen.Archive) }
            )
            DrawerItem(
                icon = Icons.Default.Delete,
                label = "Çöp Kutusu",
                isSelected = currentScreen is Screen.Trash,
                onClick = { onScreenSelected(Screen.Trash) }
            )

            Spacer(modifier = Modifier.height(8.dp))
            DrawerSectionHeader("Araçlar")

            DrawerItem(
                icon = Icons.Default.Language,
                label = "Dahili Tarayıcı",
                isSelected = currentScreen is Screen.InternalBrowser,
                onClick = { onScreenSelected(Screen.InternalBrowser) }
            )
            DrawerItem(
                icon = Icons.Default.ContentCut,
                label = "TayfNotes Web Clipper",
                isSelected = false,
                onClick = { onPlaceholderSelected("Web Clipper") }
            )

            Spacer(modifier = Modifier.height(8.dp))
            DrawerSectionHeader("Ayarlar")

            DrawerItem(
                icon = Icons.Default.Palette,
                label = "Temalar",
                isSelected = currentScreen is Screen.ThemeSelection,
                onClick = { onScreenSelected(Screen.ThemeSelection) }
            )
            DrawerItem(
                icon = Icons.Default.Settings,
                label = "Ayarlar",
                isSelected = currentScreen is Screen.Settings,
                onClick = { onScreenSelected(Screen.Settings) }
            )
            DrawerItem(
                icon = Icons.Default.CloudSync,
                label = "Senkronizasyon",
                isSelected = false,
                onClick = { onScreenSelected(Screen.Settings) } // GDrive sync is in settings
            )

            Spacer(modifier = Modifier.height(8.dp))
            DrawerSectionHeader("Veri")

            DrawerItem(
                icon = Icons.Default.IosShare,
                label = "Paylaş/Dışa Aktar",
                isSelected = false,
                onClick = { onPlaceholderSelected("Dışa Aktar") }
            )
            DrawerItem(
                icon = Icons.Default.FileDownload,
                label = "İçe Aktar",
                isSelected = false,
                onClick = { onPlaceholderSelected("İçe Aktar") }
            )

            Spacer(modifier = Modifier.height(8.dp))
            DrawerSectionHeader("Destek")

            DrawerItem(
                icon = Icons.Default.BugReport,
                label = "Hata Kayıtları (Log)",
                isSelected = false,
                onClick = { onPlaceholderSelected("Log Kayıtları") }
            )
            DrawerItem(
                icon = Icons.Default.Feedback,
                label = "Hata/İstek Bildir",
                isSelected = false,
                onClick = { onPlaceholderSelected("Geri Bildirim") }
            )
            DrawerItem(
                icon = Icons.AutoMirrored.Filled.Help,
                label = "İnfo",
                isSelected = false,
                onClick = { onPlaceholderSelected("Bilgi") }
            )

            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            DrawerFooter()
        }
    }
}

@Composable
fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.StickyNote2,
                contentDescription = "App Icon",
                modifier = Modifier.padding(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "TayfNotes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Build v0.1.${BuildConfig.BUILD_NO}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DrawerFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        Text(
            text = "V0.1.${BuildConfig.BUILD_NO}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Text(
            text = "Tayfun YAMAK©",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun DrawerItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
        )
    )
}
