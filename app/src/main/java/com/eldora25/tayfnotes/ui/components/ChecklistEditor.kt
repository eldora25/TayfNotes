package com.eldora25.tayfnotes.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.eldora25.tayfnotes.shared.model.ChecklistItem
import com.eldora25.tayfnotes.shared.model.RepeatInterval
import com.eldora25.tayfnotes.util.AlarmHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChecklistEditor(
    items: List<ChecklistItem>,
    onItemsChanged: (List<ChecklistItem>) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val reorderState = rememberReorderState(listState)

    // Microsoft To-Do style: Sort checked items to bottom
    val sortedItems = remember(items) {
        items.sortedWith(compareBy<ChecklistItem> { it.isChecked }.thenBy { it.position })
    }
    
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            itemsIndexed(sortedItems, key = { _, item -> item.id }) { index, item ->
                
                val isDragging = reorderState.draggedItemIndex == index
                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                val scaleFactor by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "scale")
                
                // Animation for moving checked items
                val alphaFactor by animateFloatAsState(
                    targetValue = if (item.isChecked) 0.5f else 1f,
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
                    onUpdateItem = { updatedItem ->
                        val updated = items.map {
                            if (it.id == updatedItem.id) updatedItem else it
                        }
                        onItemsChanged(updated)
                    },
                    onDragStart = { reorderState.draggedItemIndex = index },
                    onDrag = { dragAmountY ->
                        reorderState.draggedItemOffset += dragAmountY
                        // Note: Manual reordering might conflict with auto-sorting
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
    onUpdateItem: (ChecklistItem) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

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
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                        fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.Medium
                    ),
                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
                
                val reminder = item.reminderTimestamp
                if (reminder != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        val dateText = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(reminder))
                        Text(
                            text = dateText + if (item.reminderRepeat != null && item.reminderRepeat != RepeatInterval.NONE) " (Tekrar)" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Daha Fazla", modifier = Modifier.size(20.dp))
            }
            
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Anımsatıcı Ekle") },
                    leadingIcon = { Icon(Icons.Default.Alarm, null) },
                    onClick = {
                        showMenu = false
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            TimePickerDialog(context, { _, h, min ->
                                calendar.set(y, m, d, h, min)
                                onUpdateItem(item.copy(reminderTimestamp = calendar.timeInMillis))
                                // Note: itemId + noteId for uniqueness
                                AlarmHelper.scheduleItemReminder(context, "CHECKLIST", item.id, item.text, calendar.timeInMillis)
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Alt Görev Ekle") },
                    leadingIcon = { Icon(Icons.Default.SubdirectoryArrowRight, null) },
                    onClick = { 
                        showMenu = false
                        // Existing logic for sub-items
                        onUpdateItem(item.copy(subItems = item.subItems + ChecklistItem(text = "Yeni alt görev")))
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sil") },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = { 
                        showMenu = false
                        onDelete() 
                    }
                )
            }
        }

        // Sub Items (Simplified for brevity)
        item.subItems.forEach { subItem ->
            Row(
                modifier = Modifier.padding(start = 32.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = subItem.isChecked,
                    onCheckedChange = { isChecked ->
                        onUpdateItem(item.copy(subItems = item.subItems.map { if (it.id == subItem.id) it.copy(isChecked = isChecked) else it }))
                    },
                    modifier = Modifier.scale(0.8f)
                )
                Text(
                    text = subItem.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (subItem.isChecked) TextDecoration.LineThrough else null
                    ),
                    color = if (subItem.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
