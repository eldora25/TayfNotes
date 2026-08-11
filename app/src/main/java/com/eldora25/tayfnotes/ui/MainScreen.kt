package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.ui.components.NoteGridItem

enum class SortType { DATE_MODIFIED, DATE_CREATED, ALPHABETICAL, COLOR, MANUAL }

@Composable
fun MainScreen(
    notes: List<Note>,
    searchQuery: String,
    onEditNote: (Note) -> Unit,
    selectedNoteId: String? = null,
    sortType: SortType = SortType.DATE_MODIFIED
) {
    val sortedNotes = remember(notes, sortType) {
        when (sortType) {
            SortType.DATE_MODIFIED -> notes.sortedByDescending { it.lastModified }
            SortType.DATE_CREATED -> notes.sortedByDescending { it.createdAt }
            SortType.ALPHABETICAL -> notes.sortedBy { it.title.lowercase() }
            SortType.COLOR -> notes.sortedBy { it.colorHex }
            SortType.MANUAL -> notes.sortedBy { it.position }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (sortedNotes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (searchQuery.isEmpty()) "Henüz not yok." else "Sonuç bulunamadı.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(sortedNotes, key = { _, note -> note.id }) { _, note ->
                    val isSelected = note.id == selectedNoteId
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sortType == SortType.MANUAL) {
                            Icon(
                                Icons.Default.DragHandle, 
                                contentDescription = "Taşı",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        NoteGridItem(
                            note = note,
                            onClick = { onEditNote(note) },
                            modifier = if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)) else Modifier
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
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color, 
            contentColor = if (color.luminance() > 0.5f) Color.Black else Color.White
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
