package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.shared.model.RepeatInterval
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumReminderSheet(
    initialTimestamp: Long?,
    initialRepeat: RepeatInterval?,
    onDismiss: () -> Unit,
    onSave: (Long, RepeatInterval) -> Unit
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance().apply { 
        initialTimestamp?.let { timeInMillis = it }
    }) }
    var selectedRepeat by remember { mutableStateOf(initialRepeat ?: RepeatInterval.NONE) }
    
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Anımsatıcı Ayarla",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Date & Time Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                onClick = {
                    // Logic to open standard pickers or custom ones
                    android.app.DatePickerDialog(context, { _, y, m, d ->
                        selectedDate.set(y, m, d)
                        android.app.TimePickerDialog(context, { _, h, min ->
                            selectedDate.set(Calendar.HOUR_OF_DAY, h)
                            selectedDate.set(Calendar.MINUTE, min)
                        }, selectedDate.get(Calendar.HOUR_OF_DAY), selectedDate.get(Calendar.MINUTE), true).show()
                    }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Event, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        val dateText = SimpleDateFormat("dd MMMM yyyy", Locale("tr")).format(selectedDate.time)
                        val timeText = SimpleDateFormat("HH:mm", Locale("tr")).format(selectedDate.time)
                        Text(dateText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(timeText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Tekrar Et", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RepeatInterval.entries) { interval ->
                    val isSelected = selectedRepeat == interval
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedRepeat = interval },
                        label = { Text(getRepeatLabel(interval)) },
                        leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { onSave(selectedDate.timeInMillis, selectedRepeat) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Anımsatıcıyı Kaydet", modifier = Modifier.padding(8.dp))
            }
        }
    }
}

private fun getRepeatLabel(interval: RepeatInterval): String {
    return when(interval) {
        RepeatInterval.NONE -> "Yok"
        RepeatInterval.DAILY -> "Her Gün"
        RepeatInterval.WEEKLY -> "Her Hafta"
        RepeatInterval.MONTHLY -> "Her Ay"
    }
}
