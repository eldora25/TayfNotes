package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
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
    onRestoreNote: (String) -> Unit = {}, // Add this
    onEmptyTrash: (() -> Unit)? = null
) {
    var noteToRestore by remember { mutableStateOf<Note?>(null) } // Add this

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (onEmptyTrash != null) {
                        IconButton(onClick = onEmptyTrash) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Çöpü Boşalt")
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
                    NoteGridItem(
                        note = note, 
                        onClick = { 
                            if (onEmptyTrash != null) noteToRestore = note
                            else onEditNote(note)
                        }
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
}
