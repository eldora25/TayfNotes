package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.ChecklistItem
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import com.eldora25.tayfnotes.ui.components.NoteGridItem
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

data class CalendarItem(
    val timestamp: Long,
    val note: Note,
    val checklistItem: ChecklistItem? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    notes: List<Note>,
    onEditNote: (Note) -> Unit
) {
    val calendarItems = remember(notes) {
        val items = mutableListOf<CalendarItem>()
        notes.forEach { note ->
            if (note.reminderTimestamp != null) {
                items.add(CalendarItem(note.reminderTimestamp!!, note))
            }
            if (note.type == NoteType.CHECKLIST) {
                try {
                    val checklist = Json.decodeFromString<List<ChecklistItem>>(note.content)
                    checklist.forEach { item ->
                        if (item.reminderTimestamp != null) {
                            items.add(CalendarItem(item.reminderTimestamp!!, note, item))
                        }
                    }
                } catch (e: Exception) { }
            }
        }
        items.sortedBy { it.timestamp }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Takvim & Hatırlatıcılar") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = SimpleDateFormat("MMMM yyyy", Locale("tr")).format(Date()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (calendarItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Yaklaşan hatırlatıcı bulunmuyor.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(calendarItems) { item ->
                        val dateStr = SimpleDateFormat("dd MMMM, HH:mm", Locale("tr")).format(Date(item.timestamp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (item.checklistItem != null) "Görev: ${item.checklistItem.text}" else "Not: ${item.note.title}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Dosya: ${item.note.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                                Button(
                                    onClick = { onEditNote(item.note) },
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Text("Görüntüle")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
