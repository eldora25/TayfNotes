package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.shared.model.ChecklistItem
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import kotlinx.serialization.json.Json

@Composable
fun NoteGridItem(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: androidx.compose.ui.unit.Dp = 0.dp
) {
    val noteColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val backgroundColor = noteColor.copy(alpha = 0.15f)
    val contentColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, noteColor.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(noteColor)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note.title.ifEmpty { "Başlıksız Not" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        ),
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = noteColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (note.type == NoteType.CHECKLIST) {
                    val items = try { Json.decodeFromString<List<ChecklistItem>>(note.content) } catch(e: Exception) { emptyList() }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items.take(3).forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (item.isChecked) noteColor.copy(alpha = 0.5f) else Color.Transparent)
                                        .border(1.5.dp, if (item.isChecked) Color.Transparent else contentColor.copy(alpha = 0.3f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                                    ),
                                    color = if (item.isChecked) contentColor.copy(alpha = 0.4f) else contentColor.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (items.size > 3) {
                            Text(
                                text = "+ ${items.size - 3} öğe daha", 
                                style = MaterialTheme.typography.labelSmall,
                                color = noteColor, 
                                modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.75f),
                        lineHeight = 20.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (note.sketchData?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier
                            .background(noteColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Gesture, contentDescription = null, tint = noteColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sketch İçeriyor", style = MaterialTheme.typography.labelMedium, color = noteColor)
                    }
                }
            }
        }
    }
}
