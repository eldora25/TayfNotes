package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.ui.components.NoteGridItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    title: String,
    notes: List<Note>,
    onBack: () -> Unit,
    onEditNote: (Note) -> Unit,
    onRestoreNote: (String) -> Unit = {},
    onUnarchiveNote: (String) -> Unit = {}, // Madde 4
    onBulkRestore: (Set<String>) -> Unit = {},
    onBulkDelete: (Set<String>) -> Unit = {},
    onEmptyTrash: (() -> Unit)? = null,
    onMenuClick: () -> Unit
) {
    var noteToRestore by remember { mutableStateOf<Note?>(null) }
    var noteToUnarchive by remember { mutableStateOf<Note?>(null) } // Madde 4
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) Text("${selectedIds.size} Seçildi")
                    else Text(title) 
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "İptal")
                        }
                    } else {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menü")
                        }
                    }
                },
                actions = {
                    if (onEmptyTrash != null) {
                        if (isSelectionMode) {
                            IconButton(onClick = { onBulkRestore(selectedIds); selectedIds = emptySet() }) {
                                Icon(Icons.Default.SettingsBackupRestore, contentDescription = "Seçilenleri Geri Yükle")
                            }
                            IconButton(onClick = { onBulkDelete(selectedIds); selectedIds = emptySet() }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Seçilenleri Sil")
                            }
                        } else {
                            IconButton(onClick = { selectedIds = notes.map { it.id }.toSet() }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Tümünü Seç")
                            }
                            IconButton(onClick = onEmptyTrash) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Çöpü Boşalt")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Burada not bulunmuyor.", color = Color.Gray)
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(notes) { note ->
                    val isSelected = selectedIds.contains(note.id)
                    NoteGridItem(
                        note = note, 
                        onClick = { 
                            if (isSelectionMode) {
                                selectedIds = if (isSelected) selectedIds - note.id else selectedIds + note.id
                            } else if (onEmptyTrash != null) {
                                noteToRestore = note
                            } else if (title == "Arşiv") { // Madde 4
                                noteToUnarchive = note
                            } else {
                                onEditNote(note)
                            }
                        },
                        modifier = if (isSelected) Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)) else Modifier
                    )
                }
            }
        }
    }

    noteToRestore?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToRestore = null },
            title = { Text("Notu Geri Yükle") },
            text = { Text("Bu notu çöp kutusundan geri yüklemek istiyor musunuz?") },
            confirmButton = {
                TextButton(onClick = {
                    onRestoreNote(note.id)
                    noteToRestore = null
                }) { Text("Geri Yükle") }
            },
            dismissButton = {
                TextButton(onClick = { noteToRestore = null }) { Text("İptal") }
            }
        )
    }

    noteToUnarchive?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToUnarchive = null },
            title = { Text("Notu Arşivden Çıkar") },
            text = { Text("Bu notu ana listeye geri yüklemek istiyor musunuz?") },
            confirmButton = {
                TextButton(onClick = {
                    onUnarchiveNote(note.id)
                    noteToUnarchive = null
                }) { Text("Geri Yükle") }
            },
            dismissButton = {
                TextButton(onClick = { noteToUnarchive = null }) { Text("İptal") }
            }
        )
    }
}
