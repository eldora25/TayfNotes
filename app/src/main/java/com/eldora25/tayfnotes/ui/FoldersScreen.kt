package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.Folder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoldersScreen(
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit,
    onAddFolder: (String, String) -> Unit,
    onUpdateFolder: (Folder) -> Unit,
    onMoveFolder: (Int, Int) -> Unit = { _, _ -> }
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<Folder?>(null) }
    var folderNameInput by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#757575") }
    
    var sortType by remember { mutableStateOf(SortType.ALPHABETICAL) }
    var showSortMenu by remember { mutableStateOf(false) }

    val colors = listOf("#757575", "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722")

    val sortedFolders = remember(folders, sortType) {
        when (sortType) {
            SortType.ALPHABETICAL -> folders.sortedBy { it.name.lowercase() }
            SortType.COLOR -> folders.sortedBy { it.colorHex }
            SortType.DATE_MODIFIED -> folders.sortedByDescending { it.lastModified }
            SortType.DATE_CREATED -> folders.sortedByDescending { it.createdAt }
            SortType.MANUAL -> folders.sortedBy { it.position }
            else -> folders
        }
    }

    if (showAddDialog || folderToEdit != null) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                folderToEdit = null
                folderNameInput = ""
            },
            title = { Text(if (showAddDialog) "Yeni Klasör" else "Klasörü Düzenle") },
            text = {
                Column {
                    TextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        placeholder = { Text("Klasör Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Klasör Rengi", style = MaterialTheme.typography.labelSmall)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        colors.forEach { colorHex ->
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorHex)))
                                    .border(if (selectedColorHex == colorHex) 2.dp else 0.dp, Color.Black, CircleShape)
                                    .clickable { selectedColorHex = colorHex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (folderNameInput.isNotEmpty()) {
                        if (showAddDialog) {
                            onAddFolder(folderNameInput, selectedColorHex)
                        } else {
                            folderToEdit?.let {
                                onUpdateFolder(it.copy(name = folderNameInput, colorHex = selectedColorHex))
                            }
                        }
                    }
                    showAddDialog = false
                    folderToEdit = null
                    folderNameInput = ""
                }) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    folderToEdit = null
                    folderNameInput = ""
                }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Klasörler", style = MaterialTheme.typography.headlineMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sırala")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("Düzenlenme Zamanı") }, onClick = { sortType = SortType.DATE_MODIFIED; showSortMenu = false })
                            DropdownMenuItem(text = { Text("Oluşturulma Zamanı") }, onClick = { sortType = SortType.DATE_CREATED; showSortMenu = false })
                            DropdownMenuItem(text = { Text("Alfabetik") }, onClick = { sortType = SortType.ALPHABETICAL; showSortMenu = false })
                            DropdownMenuItem(text = { Text("Renge Göre") }, onClick = { sortType = SortType.COLOR; showSortMenu = false })
                            DropdownMenuItem(text = { Text("Manuel (Sürükle)") }, onClick = { sortType = SortType.MANUAL; showSortMenu = false })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Klasör Ekle")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(sortedFolders, key = { _, folder -> folder.id }) { index, folder ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFolderClick(folder) }
                        .then(if (sortType == SortType.MANUAL) {
                            Modifier.pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDrag = { _, _ -> },
                                    onDragEnd = { }
                                )
                            }
                        } else Modifier),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Folder, 
                                contentDescription = null,
                                tint = try { Color(android.graphics.Color.parseColor(folder.colorHex)) } catch(_: Exception) { Color.Gray },
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                folderToEdit = folder
                                folderNameInput = folder.name
                                selectedColorHex = folder.colorHex
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Düzenle", modifier = Modifier.size(20.dp))
                            }
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("${folder.noteCount}", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
