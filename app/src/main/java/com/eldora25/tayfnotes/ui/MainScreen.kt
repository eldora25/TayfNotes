package com.eldora25.tayfnotes.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.BuildConfig
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.ui.components.NoteGridItem

enum class SortType { DATE_MODIFIED, DATE_CREATED, ALPHABETICAL, COLOR, MANUAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    notes: List<Note>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onAddNote: () -> Unit,
    onAddChecklist: () -> Unit,
    onAddSketch: () -> Unit,
    onEditNote: (Note) -> Unit,
    onMoveNote: (Int, Int) -> Unit,
    isMasterDetail: Boolean = false,
    selectedNoteId: String? = null
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf(SortType.DATE_MODIFIED) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    val sortedNotes = remember(notes, sortType) {
        when (sortType) {
            SortType.DATE_MODIFIED -> notes.sortedByDescending { it.lastModified }
            SortType.DATE_CREATED -> notes.sortedByDescending { it.createdAt }
            SortType.ALPHABETICAL -> notes.sortedBy { it.title.lowercase() }
            SortType.COLOR -> notes.sortedBy { it.colorHex }
            SortType.MANUAL -> notes.sortedBy { it.position }
        }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChanged,
                            placeholder = { Text("Notlarda ara...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            isSearchActive = false
                            onSearchQueryChanged("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TayfNotes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "buildv01.${BuildConfig.BUILD_NO} Tayfun YAMAK©", 
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Ara")
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sırala")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(text = { Text("Düzenlenme Zamanı") }, onClick = { sortType = SortType.DATE_MODIFIED; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Oluşturulma Zamanı") }, onClick = { sortType = SortType.DATE_CREATED; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Alfabetik") }, onClick = { sortType = SortType.ALPHABETICAL; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Renge Göre") }, onClick = { sortType = SortType.COLOR; showSortMenu = false })
                                DropdownMenuItem(text = { Text("Sürükle-Bırak (Aktif)") }, onClick = { sortType = SortType.MANUAL; showSortMenu = false })
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AddActionButton(
                        icon = Icons.Default.Description,
                        label = "Not",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onAddNote
                    )
                    AddActionButton(
                        icon = Icons.Default.Checklist,
                        label = "Liste",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onAddChecklist
                    )
                    AddActionButton(
                        icon = Icons.Default.Gesture,
                        label = "Sketch",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = onAddSketch
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (sortedNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isEmpty()) "Henüz not yok." else "Sonuç bulunamadı.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(sortedNotes, key = { _, note -> note.id }) { index, note ->
                        val isSelected = note.id == selectedNoteId
                        
                        NoteGridItem(
                            note = note,
                            onClick = { onEditNote(note) },
                            modifier = if (sortType == SortType.MANUAL) {
                                Modifier.pointerInput(index) {
                                    detectDragGesturesAfterLongPress(
                                        onDrag = { _, _ -> /* Feedback */ },
                                        onDragEnd = { /* Reorder */ }
                                    )
                                }
                            } else Modifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color, 
            contentColor = if (color.luminance() > 0.5f) Color.Black else Color.White
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
