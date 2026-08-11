package com.eldora25.tayfnotes.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eldora25.tayfnotes.shared.model.Folder
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import com.eldora25.tayfnotes.util.ClippedContent
import com.eldora25.tayfnotes.util.WebClipperHelper
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WebClipperScreen(
    url: String,
    folders: List<Folder>,
    onSave: (Note) -> Unit,
    onCancel: () -> Unit,
    predefinedTitle: String? = null,
    predefinedContent: String? = null
) {
    var isLoading by remember { mutableStateOf(predefinedContent == null) }
    var clippedData by remember { 
        mutableStateOf<ClippedContent?>(
            predefinedContent?.let { 
                ClippedContent(predefinedTitle ?: "Kırpılan İçerik", it, url) 
            }
        ) 
    }
    
    // Etiket ve Klasör State'leri
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var tagInput by remember { mutableStateOf("") }

    // Ekran açıldığında yerel ayrıştırıcıyı (Jsoup) çalıştır (eğer önceden veri gelmediyse)
    LaunchedEffect(url) {
        if (predefinedContent == null) {
            isLoading = true
            val result = WebClipperHelper.clipArticleLocally(url)
            if (result.isSuccess) {
                clippedData = result.getOrNull()
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web Clipper", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "İptal")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            clippedData?.let { data ->
                                val newNote = Note(
                                    id = UUID.randomUUID().toString(),
                                    title = data.title,
                                    content = data.bodyText,
                                    type = NoteType.TEXT,
                                    folderId = selectedFolderId,
                                    tags = tags,
                                    sourceUrl = url,
                                    imageUris = data.mainImageUrl?.let { listOf(it) } ?: emptyList(),
                                    lastModified = System.currentTimeMillis()
                                )
                                onSave(newNote)
                            }
                        },
                        enabled = !isLoading && clippedData != null
                    ) {
                        Text("Kaydet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            modifier = Modifier.padding(paddingValues),
            label = "ClipperState"
        ) { loading ->
            if (loading) {
                // Şık Yükleme Ekranı
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("İçerik cihaz üzerinde ayrıştırılıyor...", color = Color.Gray)
                }
            } else {
                clippedData?.let { data ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Premium Önizleme Kartı (Glassmorphism & Gölgeli)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                if (!data.mainImageUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = data.mainImageUrl,
                                        contentDescription = "Ana Görsel",
                                        modifier = Modifier.fillMaxWidth().height(180.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = data.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = data.sourceUrl,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = data.bodyText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Klasör Seçimi
                        Text("Klasör", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box {
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { showFolderMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(folders.find { it.id == selectedFolderId }?.name ?: "Klasör Seçin")
                                }
                            }
                            DropdownMenu(expanded = showFolderMenu, onDismissRequest = { showFolderMenu = false }) {
                                DropdownMenuItem(text = { Text("Klasör Yok") }, onClick = { selectedFolderId = null; showFolderMenu = false })
                                folders.forEach { folder ->
                                    DropdownMenuItem(text = { Text(folder.name) }, onClick = { selectedFolderId = folder.id; showFolderMenu = false })
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Etiket (Tag) Sistemi
                        Text("Etiketler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        TextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            placeholder = { Text("Etiket yazıp 'Boşluk' veya 'Bitti' tuşuna basın") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (tagInput.isNotBlank() && !tags.contains(tagInput.trim())) {
                                        tags = tags + tagInput.trim()
                                        tagInput = ""
                                    }
                                }
                            ),
                            leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, tint = Color.Gray) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                InputChip(
                                    selected = false,
                                    onClick = { tags = tags - tag }, // Tıklayınca silinir
                                    label = { Text("#$tag") },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Kaldır", modifier = Modifier.size(16.dp)) },
                                    colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
