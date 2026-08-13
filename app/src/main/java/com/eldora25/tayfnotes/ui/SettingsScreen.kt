package com.eldora25.tayfnotes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.ui.theme.TayfFonts
import com.eldora25.tayfnotes.ui.theme.TayfTheme
import com.eldora25.tayfnotes.ui.components.DropboxAuthLauncher
import com.eldora25.tayfnotes.ui.components.GoogleDriveAuthLauncher
import com.eldora25.tayfnotes.ui.components.OneDriveAuthLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    isSyncing: Boolean,
    activeCloudProvider: String?,
    onConnectDropbox: (String) -> Unit,
    onConnectOneDrive: (String) -> Unit,
    onDisconnectCloud: () -> Unit,
    onAuthSuccess: (String) -> Unit,
    onAuthError: (String) -> Unit,
    currentTheme: TayfTheme,
    onThemeSelected: (TayfTheme) -> Unit,
    isDarkMode: Boolean?,
    onDarkModeChanged: (Boolean?) -> Unit,
    currentFontSize: Float,
    onFontSizeChanged: (Float) -> Unit,
    currentFontFamily: String,
    onFontFamilyChanged: (String) -> Unit,
    isBiometricEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    onFullBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menü")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionHeader("Görünüm & Okunabilirlik") }
            item {
                PremiumCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FontSettingsSection(
                            currentFontSize = currentFontSize,
                            onFontSizeChanged = onFontSizeChanged,
                            currentFontFamily = currentFontFamily,
                            onFontFamilyChanged = onFontFamilyChanged
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        
                        Text("Tema Seçimi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(TayfTheme.entries) { theme ->
                                val isSelected = theme == currentTheme
                                val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(getThemePreviewColor(theme)).border(3.dp, borderColor, CircleShape).clickable { onThemeSelected(theme) })
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Karanlık Mod", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Switch(checked = isDarkMode == true, onCheckedChange = onDarkModeChanged)
                        }
                    }
                }
            }

            item { SectionHeader("Bulut Senkronizasyonu") }
            item {
                PremiumCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (activeCloudProvider == null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GoogleDriveAuthLauncher(onAuthSuccess = onAuthSuccess, onAuthError = onAuthError)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudDone, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bağlı: $activeCloudProvider", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onDisconnectCloud, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Text("Bağlantıyı Kes", color = Color.White)
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Güvenlik & Yedekleme") }
            item {
                PremiumCard {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Biyometrik Kilit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Switch(checked = isBiometricEnabled, onCheckedChange = onBiometricToggle)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        SettingClickableItem(title = "Tam Yedek Al", subtitle = "ZIP dosyası olarak dışa aktar", onClick = onFullBackupClick)
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        SettingClickableItem(title = "Yedekten Geri Yükle", subtitle = "Daha önce alınan yedeği içe aktar", onClick = onImportBackupClick)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun FontSettingsSection(
    currentFontSize: Float,
    onFontSizeChanged: (Float) -> Unit,
    currentFontFamily: String,
    onFontFamilyChanged: (String) -> Unit
) {
    val fontNames = TayfFonts.keys.toList()
    var showFontMenu by remember { mutableStateOf(false) }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Font Family Selection
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { showFontMenu = true }, modifier = Modifier.fillMaxWidth()) { 
                    Text(currentFontFamily, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) 
                }
                DropdownMenu(
                    expanded = showFontMenu, 
                    onDismissRequest = { showFontMenu = false },
                    modifier = Modifier.heightIn(max = 350.dp)
                ) {
                    fontNames.forEach { fontName -> 
                        DropdownMenuItem(
                            text = { Text(fontName, fontFamily = TayfFonts[fontName]) }, 
                            onClick = { onFontFamilyChanged(fontName); showFontMenu = false }
                        ) 
                    }
                }
            }
            
            // Font Size Display
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "${currentFontSize.toInt()} sp", 
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Font Size Slider (8-50)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FormatSize, null, tint = MaterialTheme.colorScheme.primary)
            Slider(
                value = currentFontSize,
                onValueChange = onFontSizeChanged,
                valueRange = 8f..50f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Görünüm Önizleme\nBu metin nasıl görünüyor?",
                    fontSize = currentFontSize.sp,
                    fontFamily = TayfFonts[currentFontFamily] ?: androidx.compose.ui.text.font.FontFamily.Default,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = (currentFontSize * 1.3).sp
                )
            }
        }
    }
}

@Composable
fun PremiumCard(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), content = content)
}

@Composable
fun SectionHeader(title: String) {
    Text(text = title.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, top = 8.dp))
}

@Composable
fun SettingClickableItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun getThemePreviewColor(theme: TayfTheme): Color {
    return when(theme) {
        TayfTheme.MIDNIGHT -> Color(0xFF1A1C1E)
        TayfTheme.SUNSET -> Color(0xFFFF5722)
        TayfTheme.FOREST -> Color(0xFF2E7D32)
        TayfTheme.OCEAN -> Color(0xFF0277BD)
        TayfTheme.LAVENDER -> Color(0xFF7E57C2)
        TayfTheme.ROSE -> Color(0xFFD81B60)
        TayfTheme.SLATE -> Color(0xFF455A64)
        TayfTheme.EMERALD -> Color(0xFF00695C)
        TayfTheme.ROYAL -> Color(0xFFFBC02D)
        TayfTheme.CRIMSON -> Color(0xFFC62828)
    }
}
