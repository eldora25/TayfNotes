package com.eldora25.tayfnotes.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eldora25.tayfnotes.shared.model.ChecklistItem

@Composable
fun ChecklistEditor(
    items: List<ChecklistItem>,
    onItemsChanged: (List<ChecklistItem>) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderState(listState)

    // Manual reordering works best without auto-sorting by checked status
    // We use the items as they are in the list (ordered by position/manual drag)
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Add Item Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                placeholder = { Text("Yeni görev ekle...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            IconButton(onClick = {
                if (newItemText.isNotEmpty()) {
                    onItemsChanged(items + ChecklistItem(text = newItemText, position = items.size))
                    newItemText = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Ekle")
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                
                // Drag & Drop visual effects
                val isDragging = reorderState.draggedItemIndex == index
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                val scaleFactor by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "scale")
                val alphaFactor by animateFloatAsState(
                    targetValue = when {
                        isDragging -> 0.9f
                        item.isChecked -> 0.4f
                        else -> 1f
                    },
                    label = "alpha"
                )

                ChecklistItemRow(
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = reorderState.calculateCurrentOffset(index)
                            scaleX = scaleFactor
                            scaleY = scaleFactor
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .shadow(elevation, RoundedCornerShape(8.dp))
                        .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .alpha(alphaFactor),
                    item = item,
                    onToggle = { isChecked ->
                        val updated = items.map {
                            if (it.id == item.id) it.copy(isChecked = isChecked) else it
                        }
                        onItemsChanged(updated)
                    },
                    onDelete = {
                        onItemsChanged(items.filter { it.id != item.id })
                    },
                    onAddSubItem = { text ->
                         val updated = items.map {
                            if (it.id == item.id) it.copy(subItems = it.subItems + ChecklistItem(text = text)) else it
                        }
                        onItemsChanged(updated)
                    },
                    onSubItemToggle = { subItemId, isChecked ->
                        val updated = items.map {
                            if (it.id == item.id) {
                                it.copy(subItems = it.subItems.map { sub ->
                                    if (sub.id == subItemId) sub.copy(isChecked = isChecked) else sub
                                })
                            } else it
                        }
                        onItemsChanged(updated)
                    },
                    onDragStart = { reorderState.draggedItemIndex = index },
                    onDrag = { dragAmountY ->
                        reorderState.draggedItemOffset += dragAmountY
                        
                        val itemHeight = 150f // Approximate threshold for swapping
                        if (reorderState.draggedItemOffset > itemHeight && index < items.lastIndex) {
                            // Move down
                            val newList = items.toMutableList()
                            val movedItem = newList.removeAt(index)
                            newList.add(index + 1, movedItem)
                            onItemsChanged(newList)
                            
                            reorderState.draggedItemIndex = index + 1
                            reorderState.draggedItemOffset -= itemHeight
                        } else if (reorderState.draggedItemOffset < -itemHeight && index > 0) {
                            // Move up
                            val newList = items.toMutableList()
                            val movedItem = newList.removeAt(index)
                            newList.add(index - 1, movedItem)
                            onItemsChanged(newList)
                            
                            reorderState.draggedItemIndex = index - 1
                            reorderState.draggedItemOffset += itemHeight
                        }
                    },
                    onDragEnd = {
                        reorderState.draggedItemIndex = null
                        reorderState.draggedItemOffset = 0f
                    }
                )
            }
        }
    }
}

@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onAddSubItem: (String) -> Unit,
    onSubItemToggle: (String, Boolean) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSubTaskInput by remember { mutableStateOf(false) }
    var subTaskText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Sürükle",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    }
            )
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = item.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                ),
                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { showSubTaskInput = !showSubTaskInput }) {
                Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = "Alt Görev", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Sil", modifier = Modifier.size(20.dp))
            }
        }

        // Sub Items
        item.subItems.forEach { subItem ->
            Row(
                modifier = Modifier.padding(start = 32.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = subItem.isChecked,
                    onCheckedChange = { onSubItemToggle(subItem.id, it) },
                    modifier = Modifier.scale(0.8f)
                )
                Text(
                    text = subItem.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (subItem.isChecked) TextDecoration.LineThrough else null
                    ),
                    color = if (subItem.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (showSubTaskInput) {
            Row(modifier = Modifier.padding(start = 32.dp).fillMaxWidth()) {
                TextField(
                    value = subTaskText,
                    onValueChange = { subTaskText = it },
                    placeholder = { Text("Alt adım...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = {
                    if (subTaskText.isNotEmpty()) {
                        onAddSubItem(subTaskText)
                        subTaskText = ""
                        showSubTaskInput = false
                    }
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Ekle")
                }
            }
        }
    }
}
