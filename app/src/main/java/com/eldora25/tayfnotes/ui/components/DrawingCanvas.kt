package com.eldora25.tayfnotes.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.shared.model.drawing.*
import com.eldora25.tayfnotes.ui.components.canvas.*
import com.eldora25.tayfnotes.ui.theme.TayfFonts
import com.eldora25.tayfnotes.util.PdfHelper
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialData: String? = null,
    onDataChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var objects by remember { 
        mutableStateOf(
            if (initialData != null && initialData.isNotEmpty()) {
                try { Json.decodeFromString<List<DrawObject>>(initialData) } catch(e: Exception) { emptyList() }
            } else emptyList()
        )
    }

    var selectedObjectIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var undoStack by remember { mutableStateOf<List<List<DrawObject>>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<List<DrawObject>>>(emptyList()) }

    // Madde 2: Araç Hafızası (Tool State Memory)
    var toolSettingsMap by remember {
        mutableStateOf(
            ToolType.entries.associateWith { tool ->
                when (tool) {
                    ToolType.PEN -> ToolSettings(colorHex = "#000000", strokeWidth = 8f)
                    ToolType.MARKER -> ToolSettings(colorHex = "#FF0000", strokeWidth = 15f, alpha = 0.6f)
                    ToolType.PENCIL -> ToolSettings(colorHex = "#444444", strokeWidth = 4f, alpha = 0.4f)
                    ToolType.HIGHLIGHTER -> ToolSettings(colorHex = "#FFFF00", strokeWidth = 30f, alpha = 0.3f)
                    ToolType.BRUSH -> ToolSettings(colorHex = "#00FF00", strokeWidth = 20f)
                    ToolType.TEXT -> ToolSettings(colorHex = "#000000", strokeWidth = 24f)
                    else -> ToolSettings()
                }
            }
        )
    }
    
    var currentTool by remember { mutableStateOf(ToolType.PEN) }
    val currentSettings = toolSettingsMap[currentTool] ?: ToolSettings()
    
    var currentShape by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var isFillEnabled by remember { mutableStateOf(false) }
    var currentTemplate by remember { mutableStateOf(CanvasTemplate.BLANK) }
    var pdfPagePaths by remember { mutableStateOf<List<String>>(emptyList()) }

    var showTextInputDialog by remember { mutableStateOf(false) }
    var tempTextValue by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showFullColorPicker by remember { mutableStateOf(false) }
    var showShapeSelection by remember { mutableStateOf(false) }
    var showResolutionDialog by remember { mutableStateOf(false) }
    
    var canvasWidth by remember { mutableStateOf(1080) }
    var canvasHeight by remember { mutableStateOf(1920) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val id = UUID.randomUUID().toString()
            val newImage = DrawImage(id, imageUri = it.toString(), width = 500f, height = 500f)
            objects = objects + newImage
            onDataChanged(Json.encodeToString(objects))
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmaps = PdfHelper.renderPdfToBitmaps(context, it)
            pdfPagePaths = PdfHelper.saveBitmapsToCache(context, bitmaps)
            currentTemplate = CanvasTemplate.PDF
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.White).clipToBounds()) {
        AdvancedCanvasBoard(
            modifier = Modifier.size(canvasWidth.dp, canvasHeight.dp).align(Alignment.Center),
            currentColor = Color(android.graphics.Color.parseColor(currentSettings.colorHex)),
            currentStrokeWidth = currentSettings.strokeWidth,
            currentTool = currentTool,
            currentShape = currentShape,
            isFillEnabled = isFillEnabled,
            currentFillColor = Color(android.graphics.Color.parseColor(currentSettings.colorHex)),
            objects = objects,
            selectedObjectIds = selectedObjectIds,
            template = currentTemplate,
            pdfPages = pdfPagePaths,
            onSelectionChanged = { selectedObjectIds = it },
            onObjectAdded = { newObj ->
                undoStack = undoStack + listOf(objects)
                objects = objects + newObj
                onDataChanged(Json.encodeToString(objects))
            },
            onObjectUpdated = { updated ->
                val index = objects.indexOfFirst { it.id == updated.id }
                if (index != -1) {
                    val newList = objects.toMutableList()
                    newList[index] = updated
                    objects = newList
                    onDataChanged(Json.encodeToString(objects))
                }
            },
            onObjectDeleted = { deleted ->
                undoStack = undoStack + listOf(objects)
                objects = objects.filter { it.id != deleted.id }
                onDataChanged(Json.encodeToString(objects))
            }
        )

        // Toolbar
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showSettings) {
                CanvasSettingsPopup(
                    activeColor = Color(android.graphics.Color.parseColor(currentSettings.colorHex)),
                    onColorSelected = { color ->
                        val hex = String.format("#%06X", 0xFFFFFF and color.toArgb())
                        toolSettingsMap = toolSettingsMap.toMutableMap().apply {
                            put(currentTool, currentSettings.copy(colorHex = hex))
                        }
                        if (color == Color.Transparent) showFullColorPicker = true
                    },
                    activeStrokeWidth = currentSettings.strokeWidth,
                    onStrokeWidthChanged = { width ->
                        toolSettingsMap = toolSettingsMap.toMutableMap().apply {
                            put(currentTool, currentSettings.copy(strokeWidth = width))
                        }
                    },
                    activeShape = currentShape,
                    onOpenShapeSelection = { showShapeSelection = true },
                    isFillEnabled = isFillEnabled,
                    onToggleFill = { isFillEnabled = it },
                    onOpenResolutionDialog = { showResolutionDialog = true }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { 
                        if (undoStack.isNotEmpty()) {
                            redoStack = redoStack + listOf(objects)
                            objects = undoStack.last()
                            undoStack = undoStack.dropLast(1)
                            onDataChanged(Json.encodeToString(objects))
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                ) { Icon(Icons.AutoMirrored.Filled.Undo, null) }

                Spacer(modifier = Modifier.width(8.dp))

                FloatingToolBar(
                    currentTool = currentTool,
                    onToolSelected = { 
                        if (it == ToolType.IMAGE) galleryLauncher.launch("image/*")
                        else if (it == ToolType.TEXT) { showTextInputDialog = true; currentTool = it }
                        else currentTool = it 
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showSettings = !showSettings },
                    modifier = Modifier.background(
                        if (showSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        CircleShape
                    )
                ) { 
                    Icon(
                        Icons.Default.Settings, 
                        contentDescription = "Ayarlar",
                        tint = if (showSettings) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ) 
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (redoStack.isNotEmpty()) {
                            undoStack = undoStack + listOf(objects)
                            objects = redoStack.last()
                            redoStack = redoStack.dropLast(1)
                            onDataChanged(Json.encodeToString(objects))
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                ) { Icon(Icons.AutoMirrored.Filled.Redo, null) }
            }
        }

        // Dialogs
        if (showTextInputDialog) {
            var selectedFont by remember { mutableStateOf(currentSettings.fontFamily) }
            var selectedSize by remember { mutableStateOf(currentSettings.strokeWidth * 2) } 
            val fontNames = TayfFonts.keys.toList()

            AlertDialog(
                onDismissRequest = { showTextInputDialog = false },
                title = { Text("Metin Ekle") },
                text = {
                    Column {
                        TextField(
                            value = tempTextValue,
                            onValueChange = { tempTextValue = it },
                            placeholder = { Text("Buraya yazın...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        // Font Preview
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = if (tempTextValue.isEmpty()) "Metin Önizleme" else tempTextValue,
                                    fontFamily = TayfFonts[selectedFont],
                                    fontSize = selectedSize.sp,
                                    color = Color(android.graphics.Color.parseColor(currentSettings.colorHex)),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        Text("Boyut: ${selectedSize.toInt()} sp", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Slider(
                            value = selectedSize,
                            onValueChange = { selectedSize = it },
                            valueRange = 8f..150f, // Even wider for Canvas
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        Text("Yazı Tipi Seçimi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.height(200.dp)) {
                             androidx.compose.foundation.lazy.LazyColumn {
                                 items(fontNames.size) { index ->
                                     val font = fontNames[index]
                                     Row(
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .clickable { selectedFont = font }
                                             .background(if (selectedFont == font) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                             .padding(12.dp),
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         RadioButton(selected = selectedFont == font, onClick = { selectedFont = font })
                                         Spacer(Modifier.width(8.dp))
                                         Text(
                                             text = font, 
                                             fontFamily = TayfFonts[font],
                                             fontSize = 18.sp
                                         )
                                     }
                                 }
                             }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (tempTextValue.isNotEmpty()) {
                            val newText = DrawText(
                                id = UUID.randomUUID().toString(),
                                colorHex = currentSettings.colorHex,
                                strokeWidth = selectedSize,
                                text = tempTextValue,
                                fontFamily = selectedFont,
                                offsetX = 100f, offsetY = 100f
                            )
                            objects = objects + newText
                            onDataChanged(Json.encodeToString(objects))
                            
                            // Update tool settings (keep it consistent)
                            toolSettingsMap = toolSettingsMap.toMutableMap().apply {
                                put(ToolType.TEXT, currentSettings.copy(fontFamily = selectedFont, strokeWidth = selectedSize / 2))
                            }
                            
                            tempTextValue = ""
                        }
                        showTextInputDialog = false
                    }) { Text("Kanvasa Ekle") }
                },
                dismissButton = { TextButton(onClick = { showTextInputDialog = false }) { Text("İptal") } }
            )
        }

        if (showResolutionDialog) {
            var w by remember { mutableStateOf(canvasWidth.toString()) }
            var h by remember { mutableStateOf(canvasHeight.toString()) }
            AlertDialog(
                onDismissRequest = { showResolutionDialog = false },
                title = { Text("Kanvas Çözünürlüğü") },
                text = {
                    Column {
                        TextField(value = w, onValueChange = { w = it }, label = { Text("Genişlik (px)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Spacer(Modifier.height(8.dp))
                        TextField(value = h, onValueChange = { h = it }, label = { Text("Yükseklik (px)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        canvasWidth = w.toIntOrNull() ?: canvasWidth
                        canvasHeight = h.toIntOrNull() ?: canvasHeight
                        showResolutionDialog = false
                    }) { Text("Uygula") }
                }
            )
        }

        ShapeSelectionBottomSheet(
            isVisible = showShapeSelection,
            onDismissRequest = { showShapeSelection = false },
            onShapeSelected = { currentShape = it; currentTool = ToolType.SHAPE }
        )

        if (showFullColorPicker) {
            PremiumColorPicker(
                selectedColor = Color(android.graphics.Color.parseColor(currentSettings.colorHex)),
                onColorSelected = { color ->
                    val hex = String.format("#%06X", 0xFFFFFF and color.toArgb())
                    toolSettingsMap = toolSettingsMap.toMutableMap().apply {
                        put(currentTool, currentSettings.copy(colorHex = hex))
                    }
                    showFullColorPicker = false
                },
                onDismiss = { showFullColorPicker = false }
            )
        }
        
        IconButton(
            onClick = { pdfLauncher.launch("application/pdf") },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) { Icon(Icons.Default.PictureAsPdf, null) }
    }
}
