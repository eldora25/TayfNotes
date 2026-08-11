package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eldora25.tayfnotes.shared.model.Folder
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import com.eldora25.tayfnotes.util.ClippedContent
import com.eldora25.tayfnotes.util.WebClipperHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebClipperScreen(
    url: String,
    folders: List<Folder>,
    onSave: (Note) -> Unit,
    onCancel: () -> Unit
) {
    var clippedContent by remember { mutableStateOf<ClippedContent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    var title by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var showFolderMenu by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(url) {
        WebClipperHelper.clipArticleLocally(url).onSuccess { content ->
            clippedContent = content
            title = content.title
            isLoading = false
        }.onFailure { e ->
            error = e.message ?: "İçerik çekilemedi"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Web Clipper", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Vazgeç") }
                },
                actions = {
                    if (!isLoading && error == null) {
                        IconButton(onClick = {
                            val note = Note(
                                id = System.currentTimeMillis().toString(),
                                title = title,
                                content = clippedContent?.bodyText ?: "",
                                sourceUrl = url,
                                tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                folderId = selectedFolderId,
                                type = NoteType.TEXT,
                                imageUris = clippedContent?.mainImageUrl?.let { listOf(it) } ?: emptyList()
                            )
                            onSave(note)
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Kaydet", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(error!!, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onCancel, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Geri Dön")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    clippedContent?.mainImageUrl?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Başlık") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Etiketler (virgülle ayırın)") },
                        placeholder = { Text("web, makale, oku...") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box {
                        OutlinedCard(
                            onClick = { showFolderMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(folders.find { it.id == selectedFolderId }?.name ?: "Klasör Seçilmedi")
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
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
                    
                    Text("İçerik Önizleme", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(clippedContent?.bodyText ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 10)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun Modifier.size(size: androidx.compose.ui.unit.Dp): Modifier = this.then(Modifier.width(size).height(size))
