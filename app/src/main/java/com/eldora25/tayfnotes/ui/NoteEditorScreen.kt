package com.eldora25.tayfnotes.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eldora25.tayfnotes.shared.model.ChecklistItem
import com.eldora25.tayfnotes.shared.model.Folder
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import com.eldora25.tayfnotes.ui.components.ColorSelector
import com.eldora25.tayfnotes.ui.components.DrawingCanvas
import com.eldora25.tayfnotes.ui.components.TodoEditor
import com.eldora25.tayfnotes.ui.theme.EditorNeonIcon
import com.eldora25.tayfnotes.ui.theme.TayfFonts
import com.eldora25.tayfnotes.util.AudioRecorder
import com.eldora25.tayfnotes.util.FileExportHelper
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note? = null,
    folders: List<Folder> = emptyList(),
    initialSketch: Boolean = false,
    fontSize: Float = 16f,
    fontFamily: String = "Roboto",
    onBack: () -> Unit,
    onSave: (Note) -> Unit,
    onDelete: (Note) -> Unit
) {
    val context = LocalContext.current
    val noteId = remember { note?.id ?: System.currentTimeMillis().toString() }

    var currentNoteFontFamily by remember { mutableStateOf(note?.fontFamily ?: fontFamily) }
    var currentNoteFontSize by remember { mutableStateOf(note?.fontSize ?: fontSize) }
    val noteFontFamily = TayfFonts[currentNoteFontFamily] ?: androidx.compose.ui.text.font.FontFamily.Default
    
    var title by remember { mutableStateOf(note?.title ?: "") }
    var emoji by remember { mutableStateOf(note?.emoji ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var colorHex by remember { mutableStateOf(note?.colorHex ?: "#FFFFFF") }
    var reminderTimestamp by remember { mutableStateOf(note?.reminderTimestamp) }
    var folderId by remember { mutableStateOf(note?.folderId) }
    var imageUris by remember { mutableStateOf(note?.imageUris ?: emptyList()) }
    var audioPath by remember { mutableStateOf(note?.audioPath) }
    var sketchData by remember { mutableStateOf(note?.sketchData) }
    
    val initialItems = remember(note) {
        if (note?.type == NoteType.CHECKLIST && note.content.isNotEmpty()) {
            try { Json.decodeFromString<List<ChecklistItem>>(note.content) } catch(_: Exception) { emptyList() }
        } else emptyList()
    }
    var checklistItems by remember { mutableStateOf(initialItems) }

    var isPreviewMode by remember { mutableStateOf(false) }
    var isSketchMode by remember { mutableStateOf(initialSketch || (sketchData != null)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showEmojiMenu by remember { mutableStateOf(false) }
    
    val recorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.surface
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUris = imageUris + it.toString() }
    }

    val saveCurrentNote = {
        var finalContent = content
        if (checklistItems.isNotEmpty()) {
            finalContent = Json.encodeToString(checklistItems)
        }

        var finalTitle = title
        if (finalTitle.isEmpty()) {
            val textForTitle = if (checklistItems.isNotEmpty()) {
                checklistItems.firstOrNull()?.text ?: ""
            } else content
            
            if (textForTitle.isNotEmpty()) {
                finalTitle = textForTitle.trim().split("\\s+".toRegex()).take(5).joinToString(" ")
            }
        }

        if (finalTitle.isNotEmpty() || finalContent.isNotEmpty() || imageUris.isNotEmpty() || audioPath != null || checklistItems.isNotEmpty() || sketchData != null) {
            val finalNote = Note(
                id = noteId,
                title = finalTitle,
                content = finalContent,
                colorHex = colorHex,
                emoji = if (emoji.isEmpty()) null else emoji,
                type = when {
                    sketchData != null -> NoteType.SKETCH
                    checklistItems.isNotEmpty() -> NoteType.CHECKLIST
                    else -> NoteType.TEXT
                },
                reminderTimestamp = reminderTimestamp,
                folderId = folderId,
                imageUris = imageUris,
                audioPath = audioPath,
                sketchData = sketchData,
                lastModified = System.currentTimeMillis(),
                fontFamily = currentNoteFontFamily,
                fontSize = currentNoteFontSize
            )
            onSave(finalNote)
        }
    }

    // Auto-save logic
    LaunchedEffect(title, content, colorHex, reminderTimestamp, folderId, imageUris, audioPath, checklistItems, sketchData, emoji, currentNoteFontFamily, currentNoteFontSize) {
        if (title.isNotEmpty() || content.isNotEmpty() || imageUris.isNotEmpty() || audioPath != null || checklistItems.isNotEmpty() || sketchData != null || emoji.isNotEmpty()) {
            delay(2000) // Debounce save
            saveCurrentNote()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (note == null) "Yeni Ekle" else "Düzenle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        saveCurrentNote()
                        onBack()
                    }) { 
                        EditorNeonIcon {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") 
                        }
                    }
                },
                actions = {
                    if (!isSketchMode) {
                        IconButton(onClick = { isSketchMode = true }) {
                            EditorNeonIcon { Icon(Icons.Default.Gesture, contentDescription = "Sketch") }
                        }
                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            EditorNeonIcon { Icon(Icons.Default.Image, contentDescription = "Resim") }
                        }
                        IconButton(onClick = {
                            if (!isRecording) {
                                val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
                                try {
                                    recorder.startRecording(file)
                                    audioPath = file.absolutePath
                                    isRecording = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Kayıt Başlatılamadı", Toast.LENGTH_SHORT).show()
                                }
                            } else { 
                                recorder.stopRecording()
                                isRecording = false 
                            }
                        }) {
                            EditorNeonIcon {
                                Icon(
                                    if (isRecording) Icons.Default.StopCircle else Icons.Default.Mic, 
                                    contentDescription = "Ses", 
                                    tint = if (isRecording) Color.Red else Color(0xFFFFD700)
                                )
                            }
                        }
                        IconButton(onClick = { 
                            val currentNote = Note(
                                id = noteId,
                                title = title,
                                content = if (checklistItems.isNotEmpty()) Json.encodeToString(checklistItems) else content,
                                colorHex = colorHex,
                                emoji = emoji
                            )
                            FileExportHelper.exportNoteToTxt(context, currentNote)
                        }) {
                            EditorNeonIcon { Icon(Icons.Default.Share, contentDescription = "Paylaş") }
                        }

                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            val datePickerDialog = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val timePickerDialog = TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                                            reminderTimestamp = calendar.timeInMillis
                                            Toast.makeText(context, "Hatırlatıcı ayarlandı!", Toast.LENGTH_SHORT).show()
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
                                    )
                                    timePickerDialog.show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            datePickerDialog.show()
                        }) {
                            EditorNeonIcon {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = "Hatırlatıcı",
                                    tint = if (reminderTimestamp != null) MaterialTheme.colorScheme.primary else Color.Unspecified
                                )
                            }
                        }
                        
                        IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                            EditorNeonIcon { Icon(if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility, contentDescription = "Önizle") }
                        }
                    } else {
                        IconButton(onClick = { isSketchMode = false }) {
                            EditorNeonIcon { Icon(Icons.Default.TextFields, contentDescription = "Metin Modu") }
                        }
                    }
                    
                    if (note != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            EditorNeonIcon { Icon(Icons.Default.Delete, contentDescription = "Sil") }
                        }
                    }
                    
                    IconButton(onClick = {
                        saveCurrentNote()
                        onBack()
                    }) {
                        EditorNeonIcon { Icon(Icons.Default.Check, contentDescription = "Bitti") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor.copy(alpha = 0.9f))
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor.copy(alpha = 0.1f))
        ) {
            if (!isPreviewMode) {
                if (isSketchMode) {
                    // Madde 2: Header for Sketch - Ensuring visibility
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), 
                                horizontalArrangement = Arrangement.SpaceBetween, 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box {
                                        AssistChip(
                                            onClick = { showEmojiMenu = true },
                                            label = { Text(if (emoji.isEmpty()) "Emoji" else emoji) },
                                            leadingIcon = { Icon(Icons.Default.EmojiEmotions, null, modifier = Modifier.size(18.dp)) },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        if (showEmojiMenu) {
                                            androidx.compose.ui.window.Dialog(onDismissRequest = { showEmojiMenu = false }) {
                                                Surface(
                                                    shape = RoundedCornerShape(24.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shadowElevation = 8.dp,
                                                    modifier = Modifier.widthIn(max = 280.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(16.dp)) {
                                                        Text("Emoji Seç", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                                                        val emojiList = listOf("📝", "✅", "💡", "📅", "🎨", "🚀", "❤️", "⭐", "🛒", "💻", "🔥", "📌", "🌈", "⚙️", "📚", "🏠", "🍕", "🎭", "🏃", "🎧")
                                                        emojiList.chunked(5).forEach { row ->
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                                row.forEach { e ->
                                                                    Text(
                                                                        text = e,
                                                                        fontSize = 28.sp,
                                                                        modifier = Modifier
                                                                            .clickable { emoji = e; showEmojiMenu = false }
                                                                            .padding(8.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box {
                                        AssistChip(
                                            onClick = { showFolderMenu = true },
                                            label = { Text(folders.find { it.id == folderId }?.name ?: "Klasör Seç") },
                                            leadingIcon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp)) },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        DropdownMenu(expanded = showFolderMenu, onDismissRequest = { showFolderMenu = false }) {
                                            folders.forEach { folder -> DropdownMenuItem(text = { Text(folder.name) }, onClick = { folderId = folder.id; showFolderMenu = false }) }
                                        }
                                    }
                                }
                            }
                            TextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = { Text("Sketch Başlığı", style = MaterialTheme.typography.titleLarge) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                                textStyle = MaterialTheme.typography.titleLarge
                            )
                            TextField(
                                value = content,
                                onValueChange = { content = it },
                                placeholder = { Text("Açıklama...", style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    DrawingCanvas(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        initialData = sketchData,
                        onDataChanged = { sketchData = it }
                    )
                } else {
                    // Regular Note with Full Scroll
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp), 
                            horizontalArrangement = Arrangement.SpaceBetween, 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    AssistChip(
                                        onClick = { showEmojiMenu = true },
                                        label = { Text(if (emoji.isEmpty()) "Emoji" else emoji) },
                                        leadingIcon = { Icon(Icons.Default.EmojiEmotions, null, modifier = Modifier.size(18.dp)) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    if (showEmojiMenu) {
                                        androidx.compose.ui.window.Dialog(onDismissRequest = { showEmojiMenu = false }) {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                shadowElevation = 8.dp,
                                                modifier = Modifier.widthIn(max = 280.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Text("Emoji Seç", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                                                    val emojiList = listOf("📝", "✅", "💡", "📅", "🎨", "🚀", "❤️", "⭐", "🛒", "💻", "🔥", "📌", "🌈", "⚙️", "📚", "🏠", "🍕", "🎭", "🏃", "🎧")
                                                    emojiList.chunked(5).forEach { row ->
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                            row.forEach { e ->
                                                                Text(
                                                                    text = e,
                                                                    fontSize = 28.sp,
                                                                    modifier = Modifier
                                                                        .clickable { emoji = e; showEmojiMenu = false }
                                                                        .padding(8.dp)
                                                                    )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box {
                                    AssistChip(
                                        onClick = { showFolderMenu = true },
                                        label = { Text(folders.find { it.id == folderId }?.name ?: "Klasör Seç") },
                                        leadingIcon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp)) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    DropdownMenu(expanded = showFolderMenu, onDismissRequest = { showFolderMenu = false }) {
                                        folders.forEach { folder -> DropdownMenuItem(text = { Text(folder.name) }, onClick = { folderId = folder.id; showFolderMenu = false }) }
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                var showFontMenu by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(0.4f)) {
                                    IconButton(onClick = { showFontMenu = true }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FontDownload, "Font Seç", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(currentNoteFontFamily, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        }
                                    }
                                    DropdownMenu(expanded = showFontMenu, onDismissRequest = { showFontMenu = false }) {
                                        TayfFonts.keys.forEach { fontName ->
                                            DropdownMenuItem(
                                                text = { Text(fontName, fontFamily = TayfFonts[fontName]) },
                                                onClick = { currentNoteFontFamily = fontName; showFontMenu = false }
                                            )
                                        }
                                    }
                                }
                                
                                // Font Size Slider (8-50)
                                Row(modifier = Modifier.weight(0.6f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FormatSize, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Slider(
                                        value = currentNoteFontSize,
                                        onValueChange = { currentNoteFontSize = it },
                                        valueRange = 8f..50f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                    )
                                    Text("${currentNoteFontSize.toInt()}px", style = MaterialTheme.typography.labelSmall)
                                }
                                
                                ColorSelector(selectedColorHex = colorHex, onColorSelected = { colorHex = it })
                            }
                        }

                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("Başlık", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        if (checklistItems.isNotEmpty() || note?.type == NoteType.CHECKLIST) {
                            TodoEditor(items = checklistItems, onItemsChanged = { checklistItems = it })
                        } else {
                            if (imageUris.isNotEmpty()) {
                                LazyRow(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(imageUris) { uri ->
                                        Box {
                                            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.size(120.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                            IconButton(onClick = { imageUris = imageUris - uri }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            // Rich Text support could be added here using a custom TextField or BasicTextField
                            TextField(
                                value = content,
                                onValueChange = { content = it },
                                placeholder = { Text("Notunuzu yazın...", style = MaterialTheme.typography.bodyLarge.copy(fontSize = currentNoteFontSize.sp, fontFamily = noteFontFamily)) },
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = currentNoteFontSize.sp, lineHeight = (currentNoteFontSize * 1.5).sp, fontFamily = noteFontFamily)
                            )
                        }
                        Spacer(modifier = Modifier.height(200.dp)) // Extra space for keyboard/scrolling
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    val displayTitle = (if (emoji.isNotEmpty()) "$emoji " else "") + if (title.isEmpty()) "Başlıksız Not" else title
                    Text(displayTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (checklistItems.isNotEmpty()) {
                        checklistItems.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = item.isChecked, onCheckedChange = null, enabled = false)
                                Text(item.text, style = if (item.isChecked) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } else {
                        Text(content, style = MaterialTheme.typography.bodyLarge.copy(fontSize = currentNoteFontSize.sp, fontFamily = noteFontFamily))
                    }
                    imageUris.forEach { uri ->
                        AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.FillWidth)
                    }
                }
            }
        }
    }

    if (showDeleteDialog && note != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Notu Sil") },
            text = { Text("Bu notu silmek istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(onClick = { 
                    onDelete(note)
                    showDeleteDialog = false
                    onBack()
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Vazgeç") }
            }
        )
    }
}
