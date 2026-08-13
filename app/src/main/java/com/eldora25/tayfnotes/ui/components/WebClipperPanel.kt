package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.shared.model.Folder
import com.eldora25.tayfnotes.ui.viewmodel.WebClipperViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebClipperPanel(
    url: String,
    initialMode: String,
    viewModel: WebClipperViewModel,
    capturedRawData: String? = null,
    bookmarkDataJson: String? = null,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedMode by remember { mutableStateOf(initialMode) }
    var title by remember { mutableStateOf("Yükleniyor...") }
    var description by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }
    var tagsInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    val folders by viewModel.folders.collectAsState()

    // UI State for active cards
    val isArticleSelected = selectedMode == "Article"
    val isFullPageSelected = selectedMode == "FullPage"
    val isSimplifiedSelected = selectedMode == "Simplified"
    val isBookmarkSelected = selectedMode == "Bookmark"

    // Title initialization
    LaunchedEffect(url, capturedRawData, bookmarkDataJson) {
        if (isBookmarkSelected && bookmarkDataJson != null) {
            title = "Yer İmi Kaydı"
        } else if (capturedRawData != null && !isSimplifiedSelected) {
            val doc = org.jsoup.Jsoup.parse(capturedRawData, url)
            title = doc.title().ifEmpty { "Web İçeriği" }
        } else {
            title = "Web Notu"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Kırpma Modu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ModeCard("Makale", Icons.AutoMirrored.Filled.Article, isArticleSelected) { selectedMode = "Article" }
            ModeCard("Basit", Icons.Default.TextFields, isSimplifiedSelected) { selectedMode = "Simplified" }
            ModeCard("Tam Sayfa", Icons.Default.Fullscreen, isFullPageSelected) { selectedMode = "FullPage" }
            ModeCard("Yer İzi", Icons.Default.Bookmark, isBookmarkSelected) { selectedMode = "Bookmark" }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Organize Et", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Başlık") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Açıklama (Notlarınız)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Defter Seçimi", style = MaterialTheme.typography.labelLarge)
        LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
            items(folders) { folder ->
                FilterChip(
                    selected = selectedFolder?.id == folder.id,
                    onClick = { selectedFolder = if (selectedFolder?.id == folder.id) null else folder },
                    label = { Text(folder.name) },
                    modifier = Modifier.padding(end = 8.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Folder, 
                            contentDescription = null, 
                            tint = Color(android.graphics.Color.parseColor(folder.colorHex))
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = tagsInput,
            onValueChange = { tagsInput = it },
            label = { Text("Etiketler (virgülle ayırın)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Tag, null) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    isSaving = true
                    viewModel.processAndSave(
                        mode = selectedMode,
                        rawData = capturedRawData ?: url,
                        url = url,
                        folderId = selectedFolder?.id,
                        tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        userDescription = description,
                        providedTitle = title,
                        bookmarkJson = bookmarkDataJson
                    )
                    isSaving = false
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && (capturedRawData != null || bookmarkDataJson != null),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("TayfNotes'a Kaydet", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ModeCard(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
        }
    }
}
