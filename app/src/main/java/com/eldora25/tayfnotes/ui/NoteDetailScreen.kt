package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eldora25.tayfnotes.shared.model.Note

import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    note: Note,
    fontSize: Float,
    fontFamily: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Not Detayı") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        DetailPane(
            note = note,
            fontSize = fontSize,
            fontFamily = fontFamily,
            onEdit = onEdit,
            onDelete = {
                onDelete()
                onBack()
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}
