package com.eldora25.tayfnotes.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.eldora25.tayfnotes.BuildConfig
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.ui.components.NoteGridItem
import com.eldora25.tayfnotes.ui.components.rememberReorderState
import kotlinx.coroutines.launch

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
    onDeleteNote: (Note) -> Unit = {},
    onArchiveNote: (String, Boolean) -> Unit = { _, _ -> },
    onUndoDelete: (String) -> Unit = {},
    onNoteClick: (Note) -> Unit = {},
    selectedNoteId: String? = null,
    fontSize: Float = 16f,
    fontFamily: String = "Roboto",
    onMenuClick: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {}
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var sortType by remember { mutableStateOf(SortType.DATE_MODIFIED) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val reorderState = rememberReorderState(listState)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
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
            PremiumTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                onSearchToggle = { isSearchActive = it },
                showSortMenu = showSortMenu,
                onSortMenuToggle = { showSortMenu = it },
                onSortSelected = { sortType = it },
                onMenuClick = onMenuClick
            )
        },
        floatingActionButton = {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionPill(Icons.Default.Add, "Not", MaterialTheme.colorScheme.primary, onAddNote)
                    FloatingActionPill(Icons.Default.FactCheck, "Yapılacaklar", MaterialTheme.colorScheme.secondary, onAddChecklist)
                    FloatingActionPill(Icons.Default.Gesture, "Sketch", MaterialTheme.colorScheme.tertiary, onAddSketch)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)
        ) {
            val isWideScreen = this@BoxWithConstraints.maxWidth > 600.dp
            val selectedNote = remember(selectedNoteId, notes) { notes.find { it.id == selectedNoteId } }

            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (sortedNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (searchQuery.isEmpty()) "Henüz not yok." else "Sonuç bulunamadı.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(sortedNotes, key = { _, note -> note.id }) { index, note ->
                                val isManualSort = sortType == SortType.MANUAL
                                val isDragging = reorderState.draggedItemIndex == index && isManualSort
                                
                                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                                val scale by animateFloatAsState(if (isDragging) 1.02f else 1f)

                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                            onDeleteNote(note)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Not çöpe taşındı",
                                                    actionLabel = "Geri Al",
                                                    duration = SnackbarDuration.Long
                                                )
                                                if (result == SnackbarResult.ActionPerformed) onUndoDelete(note.id)
                                            }
                                            true
                                        } else if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                            onArchiveNote(note.id, true)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Not arşivlendi",
                                                    actionLabel = "Geri Al",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) onArchiveNote(note.id, false)
                                            }
                                            true
                                        } else false
                                    }
                                )

                                Box(
                                    modifier = Modifier.fillMaxWidth().graphicsLayer {
                                        translationY = if (isManualSort) reorderState.calculateCurrentOffset(index) else 0f
                                        scaleX = scale
                                        scaleY = scale
                                    }.zIndex(if (isDragging) 1f else 0f).pointerInput(isManualSort) {
                                        if (isManualSort) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { reorderState.draggedItemIndex = index },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    reorderState.draggedItemOffset += dragAmount.y
                                                    val threshold = 200f
                                                    if (reorderState.draggedItemOffset > threshold && index < sortedNotes.lastIndex) {
                                                        onMoveNote(index, index + 1)
                                                        reorderState.draggedItemIndex = index + 1
                                                        reorderState.draggedItemOffset -= threshold
                                                    } else if (reorderState.draggedItemOffset < -threshold && index > 0) {
                                                        onMoveNote(index, index - 1)
                                                        reorderState.draggedItemIndex = index - 1
                                                        reorderState.draggedItemOffset += threshold
                                                    }
                                                },
                                                onDragEnd = { reorderState.draggedItemIndex = null; reorderState.draggedItemOffset = 0f },
                                                onDragCancel = { reorderState.draggedItemIndex = null; reorderState.draggedItemOffset = 0f }
                                            )
                                        }
                                    }
                                ) {
                                    SwipeToDismissBox(
                                        state = dismissState,
                                        enableDismissFromStartToEnd = true, // Madde 4: Archive
                                        backgroundContent = {
                                            val direction = dismissState.dismissDirection
                                            val color = when (direction) {
                                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondary
                                                else -> Color.Transparent
                                            }
                                            val alignment = when (direction) {
                                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                                else -> Alignment.Center
                                            }
                                            val icon = when (direction) {
                                                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Archive
                                                else -> Icons.Default.Delete
                                            }

                                            Box(Modifier.fillMaxSize().padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp)).background(color).padding(horizontal = 24.dp), contentAlignment = alignment) {
                                                Icon(icon, null, tint = Color.White)
                                            }
                                        }
                                    ) {
                                        NoteGridItem(
                                            note = note,
                                            onClick = { onNoteClick(note) },
                                            onTitleClick = { onEditNote(note) },
                                            elevation = elevation,
                                            modifier = if (note.id == selectedNoteId) Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp)) else Modifier
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isWideScreen) {
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                    Box(modifier = Modifier.weight(1.5f).fillMaxHeight()) {
                        if (selectedNote != null) {
                            DetailPane(note = selectedNote, fontSize = fontSize, fontFamily = fontFamily, onEdit = { onEditNote(selectedNote) }, onDelete = { onDeleteNote(selectedNote) })
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Görüntülemek için soldan bir not seçin", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            // Removed local action buttons Box as they are now in the FAB
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    showSortMenu: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    onSortSelected: (SortType) -> Unit,
    onMenuClick: () -> Unit = {}
) {
    if (isSearchActive) {
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 4.dp) {
            TextField(value = searchQuery, onValueChange = onSearchQueryChanged, placeholder = { Text("Notlarda ara...", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) }, trailingIcon = { IconButton(onClick = { onSearchQueryChanged(""); onSearchToggle(false) }) { Icon(Icons.Default.Close, null) } })
        }
    } else {
        CenterAlignedTopAppBar(
            navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null) } },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TayfNotes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("buildv01.${BuildConfig.BUILD_NO}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            actions = {
                IconButton(onClick = { onSearchToggle(true) }) { Icon(Icons.Default.Search, null) }
                Box {
                    IconButton(onClick = { onSortMenuToggle(true) }) { Icon(Icons.AutoMirrored.Filled.Sort, null) }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { onSortMenuToggle(false) }) {
                        DropdownMenuItem(text = { Text("Düzenlenme Zamanı") }, onClick = { onSortSelected(SortType.DATE_MODIFIED); onSortMenuToggle(false) }, leadingIcon = { Icon(Icons.Default.Update, null) })
                        DropdownMenuItem(text = { Text("Oluşturulma Zamanı") }, onClick = { onSortSelected(SortType.DATE_CREATED); onSortMenuToggle(false) }, leadingIcon = { Icon(Icons.Default.AddCircleOutline, null) })
                        DropdownMenuItem(text = { Text("Alfabetik") }, onClick = { onSortSelected(SortType.ALPHABETICAL); onSortMenuToggle(false) }, leadingIcon = { Icon(Icons.Default.SortByAlpha, null) })
                        DropdownMenuItem(text = { Text("Renge Göre") }, onClick = { onSortSelected(SortType.COLOR); onSortMenuToggle(false) }, leadingIcon = { Icon(Icons.Default.Palette, null) })
                        DropdownMenuItem(text = { Text("Manuel (Sürükle)") }, onClick = { onSortSelected(SortType.MANUAL); onSortMenuToggle(false) }, leadingIcon = { Icon(Icons.Default.DragIndicator, null) })
                    }
                }
            }
        )
    }
}

@Composable
fun FloatingActionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.15f)).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
