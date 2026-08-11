package com.eldora25.tayfnotes.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Security
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
import com.eldora25.tayfnotes.ui.theme.TayfTheme
import com.eldora25.tayfnotes.ui.components.DropboxAuthLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    // Senkronizasyon
    isSyncing: Boolean,
    activeCloudProvider: String?,
    onConnectGoogleDrive: () -> Unit, // YENİ: Gerçek OAuth tetikleyicisi
    onConnectDropbox: (String) -> Unit,     // YENİ: Gerçek OAuth tetikleyicisi
    onDisconnectCloud: () -> Unit,
    onAuthSuccess: (String) -> Unit,
    onAuthError: (Exception) -> Unit,
    // Tema ve Görünüm
    currentTheme: TayfTheme,
    onThemeSelected: (TayfTheme) -> Unit,
    isDarkMode: Boolean?,
    onDarkModeChanged: (Boolean?) -> Unit,
    // YENİ: Tipografi
    currentFontSize: Float,
    onFontSizeChanged: (Float) -> Unit,
    // Güvenlik ve Veri
    isBiometricEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    onFullBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // --- 1. GERÇEK BULUT SENKRONİZASYONU ---
            item { SectionHeader("Bulut & Senkronizasyon (Gerçek Zamanlı)") }
            item {
                val context = LocalContext.current
                val gso = remember {
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                        .build()
                }
                val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        try {
                            val account = task.getResult(Exception::class.java)
                            account?.email?.let { onAuthSuccess(it) }
                        } catch (e: Exception) {
                            onAuthError(e)
                        }
                    }
                }

                PremiumCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (activeCloudProvider != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (activeCloudProvider != null) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = activeCloudProvider ?: "Bağlı Hesap Yok", 
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (activeCloudProvider != null) "Cihazlar arası otomatik eşitleniyor" else "Verileriniz sadece bu cihazda",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (activeCloudProvider == null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { launcher.launch(googleSignInClient.signInIntent) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                                ) { Text("Google Drive", color = Color.White) }
                                
                                DropboxAuthLauncher(
                                    appKey = "ctnqddduaepcw33", // Console'dan aldığınız kod
                                    onAuthSuccess = { token ->
                                        onConnectDropbox(token) 
                                    },
                                    onAuthError = {
                                        // Hata yönetimi
                                    }
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = onDisconnectCloud,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) { Text("Bağlantıyı Kes", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }

            // --- 2. GÖRÜNÜM VE TİPOGRAFİ ---
            item { SectionHeader("Görünüm & Okunabilirlik") }
            item {
                PremiumCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        
                        // Font Büyüklüğü (Slider)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Yazı Tipi Boyutu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("A", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                            Slider(
                                value = currentFontSize,
                                onValueChange = onFontSizeChanged,
                                valueRange = 12f..24f,
                                steps = 5,
                                modifier = Modifier.weight(1f)
                            )
                            Text("A", fontSize = 24.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        
                        // Tema Paleti
                        Text("Vurgu Rengi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(TayfTheme.entries) { theme ->
                                val isSelected = theme == currentTheme
                                val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "")
                                
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(getThemePreviewColor(theme))
                                        .border(3.dp, borderColor, CircleShape)
                                        .clickable { onThemeSelected(theme) }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        
                        // Karanlık Mod Geçişi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Karanlık Mod", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Switch(checked = isDarkMode == true, onCheckedChange = { onDarkModeChanged(it) })
                        }
                    }
                }
            }

            // --- 3. GÜVENLİK VE YEREL VERİ ---
            item { SectionHeader("Güvenlik & Yerel Yedekleme") }
            item {
                PremiumCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Biyometrik Kilit (Parmak İzi)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Switch(checked = isBiometricEnabled, onCheckedChange = onBiometricToggle)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        SettingClickableItem(title = "Tam Yedek Al (ZIP Export)", subtitle = "Notlar, çizimler ve medyaları cihaza kaydet", onClick = onFullBackupClick)
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        SettingClickableItem(title = "Yedekten Geri Yükle", subtitle = "Daha önce alınan yerel yedeği içe aktar", onClick = onImportBackupClick)
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun PremiumCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        content = content
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
    )
}

@Composable
fun SettingClickableItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Temaların önizleme renklerini döndüren yardımcı fonksiyon
fun getThemePreviewColor(theme: TayfTheme): Color {
    return when(theme) {
        TayfTheme.MIDNIGHT -> Color(0xFFD4AF37)
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
