package com.eldora25.tayfnotes.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp)
        ) {
            itemsIndexed(sortedItems, key = { _, item -> item.id }) { index, item ->
                // Ana Görev
                TodoItemRowWithSubtasks(
                    item = item,
                    onToggle = { isChecked ->
                        onItemsChanged(items.map { if (it.id == item.id) it.copy(isChecked = isChecked) else it })
                    },
                    onDelete = { onItemsChanged(items.filter { it.id != item.id }) },
                    onUpdate = { updated -> onItemsChanged(items.map { if (it.id == updated.id) updated else it }) }
                )
            }
        }
    }
}

@Composable
fun TodoItemRowWithSubtasks(
    item: ChecklistItem,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onUpdate: (ChecklistItem) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var newSubtaskText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.isChecked, onCheckedChange = onToggle)
            Text(
                text = item.text,
                modifier = Modifier.weight(1f).clickable { isExpanded = !isExpanded },
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                    fontWeight = FontWeight.Bold
                ),
                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
        }

        if (isExpanded) {
            Column(modifier = Modifier.padding(start = 48.dp, end = 16.dp, bottom = 12.dp)) {
                // Alt Görevler
                item.subItems.forEach { sub ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = sub.isChecked,
                            onCheckedChange = { checked ->
                                val newSubs = item.subItems.map { if (it.id == sub.id) it.copy(isChecked = checked) else it }
                                onUpdate(item.copy(subItems = newSubs))
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                        Text(
                            sub.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (sub.isChecked) TextDecoration.LineThrough else null
                            )
                        )
                    }
                }
                
                // Alt Görev Ekleme
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text("Alt adım ekle...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                    )
                    IconButton(onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            onUpdate(item.copy(subItems = item.subItems + ChecklistItem(text = newSubtaskText)))
                            newSubtaskText = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
