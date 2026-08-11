package com.eldora25.tayfnotes.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.ChecklistItem

@Composable
fun ChecklistEditor(
    items: List<ChecklistItem>,
    onItemsChanged: (List<ChecklistItem>) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }

    val sortedItems = remember(items) {
        items.sortedWith(compareBy<ChecklistItem> { it.isChecked }.thenBy { it.position })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Add Item Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sortedItems, key = { it.id }) { item ->
                val alpha by animateFloatAsState(targetValue = if (item.isChecked) 0.4f else 1f, label = "alpha")
                
                ChecklistItemRow(
                    modifier = Modifier.alpha(alpha),
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
    modifier: Modifier = Modifier
) {
    var showSubTaskInput by remember { mutableStateOf(false) }
    var subTaskText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Sürükle",
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 4.dp)
            )
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onToggle
            )
            Text(
                text = item.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                    color = if (item.isChecked) Color.Gray else Color.Unspecified
                )
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
                    color = if (subItem.isChecked) Color.Gray else Color.Unspecified
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
