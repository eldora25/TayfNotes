package com.eldora25.tayfnotes.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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

    // Sort: Unchecked first, then checked at the bottom
    val sortedItems = remember(items) {
        items.sortedWith(compareBy<ChecklistItem> { it.isChecked }.thenBy { it.position })
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
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
                val alphaFactor by animateFloatAsState(if (item.isChecked) 0.5f else 1f, label = "alpha")

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
                        onItemsChanged(items.map { if (it.id == item.id) it.copy(isChecked = isChecked) else it })
                    },
                    onDelete = { onItemsChanged(items.filter { it.id != item.id }) },
                    onUpdateItem = { updated -> onItemsChanged(items.map { if (it.id == updated.id) updated else it }) },
                    onDragStart = { reorderState.draggedItemIndex = index },
                    onDrag = { reorderState.draggedItemOffset += it },
                    onDragEnd = { reorderState.draggedItemIndex = null; reorderState.draggedItemOffset = 0f }
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

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DragIndicator,
            contentDescription = "Sürükle",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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
            onCheckedChange = onToggle
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
                    Icon(Icons.Default.Alarm, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(reminder)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
        
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Anımsatıcı") },
                leadingIcon = { Icon(Icons.Default.Alarm, null) },
                onClick = {
                    showMenu = false
                    val cal = Calendar.getInstance()
                    DatePickerDialog(context, { _, y, m, d ->
                        TimePickerDialog(context, { _, h, min ->
                            cal.set(y, m, d, h, min)
                            onUpdateItem(item.copy(reminderTimestamp = cal.timeInMillis))
                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }
            )
            DropdownMenuItem(
                text = { Text("Alt Görev") },
                leadingIcon = { Icon(Icons.Default.SubdirectoryArrowRight, null) },
                onClick = { 
                    showMenu = false
                    onUpdateItem(item.copy(subItems = item.subItems + ChecklistItem(text = "Yeni alt görev")))
                }
            )
            DropdownMenuItem(
                text = { Text("Sil") },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = { showMenu = false; onDelete() }
            )
        }
    }
}
