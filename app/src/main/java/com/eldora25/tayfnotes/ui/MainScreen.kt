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
import androidx.compose.ui.draw.blur
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
    onUndoDelete: (String) -> Unit = {}, // Add this
    onNoteClick: (Note) -> Unit = {},
    selectedNoteId: String? = null,
    fontSize: Float = 16f,
    fontFamily: String = "Roboto",
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
                onSortSelected = { sortType = it }
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isWideScreen = this@BoxWithConstraints.maxWidth > 600.dp
            val selectedNote = remember(selectedNoteId, notes) {
                notes.find { it.id == selectedNoteId }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT SIDE: LIST
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (sortedNotes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (searchQuery.isEmpty()) "Henüz not yok." else "Sonuç bulunamadı.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(sortedNotes, key = { _, note -> note.id }) { index, note ->
                                val isManualSort = sortType == SortType.MANUAL
                                val isDragging = reorderState.draggedItemIndex == index && isManualSort
                                
                                val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "elevation")
                                val scaleFactor by animateFloatAsState(if (isDragging) 1.02f else 1f, label = "scale")
                                val alphaFactor by animateFloatAsState(if (isDragging) 0.9f else 1f, label = "alpha")

                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                            onDeleteNote(note)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Not çöpe taşındı",
                                                    actionLabel = "Geri Al",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    onUndoDelete(note.id)
                                                }
                                            }
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            translationY = if (isManualSort) reorderState.calculateCurrentOffset(index) else 0f
                                            scaleX = scaleFactor
                                            scaleY = scaleFactor
                                        }
                                        .zIndex(if (isDragging) 1f else 0f)
                                        .alpha(alphaFactor)
                                        .pointerInput(isManualSort) {
                                            if (isManualSort) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { reorderState.draggedItemIndex = index },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        reorderState.draggedItemOffset += dragAmount.y
                                                        
                                                        val itemHeight = 300f 
                                                        if (reorderState.draggedItemOffset > itemHeight && index < sortedNotes.lastIndex) {
                                                            onMoveNote(index, index + 1)
                                                            reorderState.draggedItemIndex = index + 1
                                                            reorderState.draggedItemOffset -= itemHeight
                                                        } else if (reorderState.draggedItemOffset < -itemHeight && index > 0) {
                                                            onMoveNote(index, index - 1)
                                                            reorderState.draggedItemIndex = index - 1
                                                            reorderState.draggedItemOffset += itemHeight
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        reorderState.draggedItemIndex = null
                                                        reorderState.draggedItemOffset = 0f
                                                    },
                                                    onDragCancel = {
                                                        reorderState.draggedItemIndex = null
                                                        reorderState.draggedItemOffset = 0f
                                                    }
                                                )
                                            }
                                        }
                                ) {
                                    SwipeToDismissBox(
                                        state = dismissState,
                                        enableDismissFromStartToEnd = false,
                                        backgroundContent = {
                                            val color by androidx.compose.animation.animateColorAsState(
                                                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 
                                                    MaterialTheme.colorScheme.error 
                                                else 
                                                    Color.Transparent,
                                                animationSpec = tween(300), label = "swipeColor"
                                            )
                                            val iconScale by animateDpAsState(
                                                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 28.dp else 20.dp, label = "iconScale"
                                            )

                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .padding(vertical = 4.dp)
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(color)
                                                    .padding(end = 24.dp),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Sil",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(iconScale)
                                                )
                                            }
                                        }
                                    ) {
                                        NoteGridItem(
                                            note = note,
                                            onClick = { if (isWideScreen) onNoteClick(note) else onEditNote(note) },
                                            elevation = elevation,
                                            modifier = if (note.id == selectedNoteId) {
                                                Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
                                            } else Modifier
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT SIDE: PREVIEW
                if (isWideScreen) {
                    VerticalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                    Box(modifier = Modifier.weight(1.5f).fillMaxHeight()) {
                        if (selectedNote != null) {
                            DetailPane(
                                note = selectedNote,
                                fontSize = fontSize,
                                fontFamily = fontFamily,
                                onEdit = { onEditNote(selectedNote) },
                                onDelete = { onDeleteNote(selectedNote) }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Görüntülemek için soldan bir not seçin", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    tonalElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingActionPill(icon = Icons.Default.Add, label = "Not", color = MaterialTheme.colorScheme.primary, onClick = onAddNote)
                        FloatingActionPill(icon = Icons.Default.Checklist, label = "Liste", color = MaterialTheme.colorScheme.secondary, onClick = onAddChecklist)
                        FloatingActionPill(icon = Icons.Default.Gesture, label = "Sketch", color = MaterialTheme.colorScheme.tertiary, onClick = onAddSketch)
                    }
                }
            }
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
    onSortSelected: (SortType) -> Unit
) {
    if (isSearchActive) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Notlarda ara...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara", tint = Color.Gray) },
                trailingIcon = {
                    IconButton(onClick = { 
                        onSearchQueryChanged("")
                        onSearchToggle(false) 
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }
            )
        }
    } else {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TayfNotes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("buildv01.${BuildConfig.BUILD_NO}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            actions = {
                IconButton(onClick = { onSearchToggle(true) }) {
                    Icon(Icons.Default.Search, contentDescription = "Ara")
                }
                Box {
                    IconButton(onClick = { onSortMenuToggle(true) }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sırala")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { onSortMenuToggle(false) }) {
                        DropdownMenuItem(text = { Text("Düzenlenme Zamanı") }, onClick = { onSortSelected(SortType.DATE_MODIFIED); onSortMenuToggle(false) })
                        DropdownMenuItem(text = { Text("Oluşturulma Zamanı") }, onClick = { onSortSelected(SortType.DATE_CREATED); onSortMenuToggle(false) })
                        DropdownMenuItem(text = { Text("Alfabetik") }, onClick = { onSortSelected(SortType.ALPHABETICAL); onSortMenuToggle(false) })
                        DropdownMenuItem(text = { Text("Renge Göre") }, onClick = { onSortSelected(SortType.COLOR); onSortMenuToggle(false) })
                    }
                }
            }
        )
    }
}

@Composable
fun FloatingActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
