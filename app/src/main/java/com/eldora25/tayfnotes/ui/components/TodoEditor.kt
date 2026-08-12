package com.eldora25.tayfnotes.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
fun TodoEditor(
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
        // Add Todo Row
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    placeholder = { Text("Yapılacak bir görev ekle...", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                IconButton(onClick = {
                    if (newItemText.isNotBlank()) {
                        onItemsChanged(items + ChecklistItem(text = newItemText, position = items.size))
                        newItemText = ""
                    }
                }) {
                    Icon(Icons.Default.AddCircle, contentDescription = "Ekle", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp)
        ) {
            itemsIndexed(sortedItems, key = { _, item -> item.id }) { index, item ->
                val isDragging = reorderState.draggedItemIndex == index
                val elevation by animateDpAsState(if (isDragging) 12.dp else 0.dp, label = "elevation")
                
                // Strike-through and fade animation
                val alpha by animateFloatAsState(if (item.isChecked) 0.5f else 1f, label = "alpha")
                
                TodoItemRow(
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = reorderState.calculateCurrentOffset(index)
                            scaleX = if (isDragging) 1.05f else 1f
                            scaleY = if (isDragging) 1.05f else 1f
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .shadow(elevation, RoundedCornerShape(12.dp))
                        .alpha(alpha),
                    item = item,
                    onToggle = { isChecked ->
                        onItemsChanged(items.map { if (it.id == item.id) it.copy(isChecked = isChecked) else it })
                    },
                    onDelete = { onItemsChanged(items.filter { it.id != item.id }) },
                    onUpdate = { updated -> onItemsChanged(items.map { if (it.id == updated.id) updated else it }) },
                    onDragStart = { reorderState.draggedItemIndex = index },
                    onDrag = { reorderState.draggedItemOffset += it },
                    onDragEnd = { reorderState.draggedItemIndex = null; reorderState.draggedItemOffset = 0f }
                )
            }
        }
    }
}

@Composable
fun TodoItemRow(
    item: ChecklistItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onUpdate: (ChecklistItem) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showReminderSheet by remember { mutableStateOf(false) }
    
    // Microsoft To-Do style line through animation
    val textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
    val textColor = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Sürükle",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDrag = { change, dragAmount -> change.consume(); onDrag(dragAmount.y) },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    )
                }
            )
            
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = textDecoration,
                        fontWeight = if (item.isChecked) FontWeight.Normal else FontWeight.SemiBold
                    ),
                    color = textColor
                )
                
                if (item.reminderTimestamp != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        val dateText = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(item.reminderTimestamp!!))
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menü", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Anımsatıcı Kur") },
                    leadingIcon = { Icon(Icons.Default.Alarm, null) },
                    onClick = {
                        showMenu = false
                        showReminderSheet = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sil") },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }

    if (showReminderSheet) {
        PremiumReminderSheet(
            initialTimestamp = item.reminderTimestamp,
            initialRepeat = item.reminderRepeat,
            onDismiss = { showReminderSheet = false },
            onSave = { timestamp, repeat ->
                onUpdate(item.copy(reminderTimestamp = timestamp, reminderRepeat = repeat))
                AlarmHelper.scheduleItemReminder(context, "TODO", item.id, item.text, timestamp)
                showReminderSheet = false
            }
        )
    }
}
