package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    onMenuClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Uygulama Bilgisi", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menü")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard(
                title = "Gerçekten Yapabildikleri",
                subtitle = "Mevcut Yetenekler",
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF4CAF50),
                items = listOf(
                    "Vektörel Çizim Motoru: Sınırsız zoom ve yüksek kalite.",
                    "PDF Markup: PDF dosyalarını içe aktarma ve üzerinde çizim yapma.",
                    "Gelişmiş To-Do: Alt görev desteği ve Microsoft To-Do tarzı sıralama.",
                    "Zengin Metin Editörü: Kalın, italik ve renkli metin desteği.",
                    "Lokal Veritabanı: Verileriniz tamamen cihazınızda güvendedir.",
                    "Biyometrik Kilit: Parmak izi ile not koruması.",
                    "Premium Temalar: 10 farklı renk paleti seçeneği.",
                    "Yedekleme: ZIP formatında tam yedek alma ve geri yükleme."
                )
            )

            InfoCard(
                title = "Geliştirme Aşamasında",
                subtitle = "Kısıtlamalar ve Planlar",
                icon = Icons.Default.Construction,
                iconColor = Color(0xFFFF9800),
                items = listOf(
                    "Büyük PDF Performansı: Çok sayfalı PDF'lerde bellek optimizasyonu devam ediyor.",
                    "Gelişmiş Rich Text: Liste içinde karmaşık HTML desteği henüz kısıtlı.",
                    "Cloud Sync: Dropbox ve GDrive senkronizasyonu stabilite aşamasındadır.",
                    "Arama Derinliği: Çizimler içindeki metinlerin aranması henüz mümkün değildir.",
                    "OCR: Resimden metne dönüştürme özelliği planlama aşamasındadır."
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Build v0.1.${BuildConfig.BUILD_NO} - Tayfun YAMAK©",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    items: List<String>
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = iconColor)
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            
            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", modifier = Modifier.padding(end = 8.dp), color = iconColor, fontWeight = FontWeight.Bold)
                    Text(item, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                }
            }
        }
    }
}
