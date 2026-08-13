package com.eldora25.tayfnotes.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.drawing.*
import com.eldora25.tayfnotes.ui.components.canvas.*
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

    var toolColors by remember { 
        mutableStateOf(mapOf(
            ToolType.PEN to Color.Black,
            ToolType.MARKER to Color.Red,
            ToolType.PENCIL to Color.DarkGray,
            ToolType.HIGHLIGHTER to Color(0xFFFFFF00).copy(alpha = 0.3f),
            ToolType.SHAPE to Color.Blue,
            ToolType.BRUSH to Color.Green
        )) 
    }
    
    var currentStrokeWidth by remember { mutableStateOf(10f) }
    var currentTool by remember { mutableStateOf(ToolType.PEN) }
    var currentShape by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var isFillEnabled by remember { mutableStateOf(false) }
    var currentTemplate by remember { mutableStateOf(CanvasTemplate.BLANK) }
    var pdfPagePaths by remember { mutableStateOf<List<String>>(emptyList()) }

    var textInput by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }
    var textObjectToEdit by remember { mutableStateOf<DrawText?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showFullColorPicker by remember { mutableStateOf(false) }

    val currentColor = toolColors[currentTool] ?: Color.Black

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
            modifier = Modifier.fillMaxSize(),
            currentColor = currentColor,
            currentStrokeWidth = currentStrokeWidth,
            currentTool = currentTool,
            currentShape = currentShape,
            isFillEnabled = isFillEnabled,
            currentFillColor = currentColor, // Using current color for fill too
            objects = objects,
            selectedObjectIds = selectedObjectIds,
            template = currentTemplate,
            pdfPages = pdfPagePaths,
            onSelectionChanged = { selectedObjectIds = it },
            onObjectAdded = { newObj ->
                if (newObj is DrawText) { textObjectToEdit = newObj; showTextInput = true }
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

        // UI Controls (Toolbar & Popups)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showSettings) {
                CanvasSettingsPopup(
                    activeColor = currentColor,
                    onColorSelected = { color ->
                        toolColors = toolColors.toMutableMap().apply { put(currentTool, color) }
                        if (color == Color.Transparent) showFullColorPicker = true
                    },
                    activeStrokeWidth = currentStrokeWidth,
                    onStrokeWidthChanged = { currentStrokeWidth = it },
                    activeShape = currentShape,
                    onShapeSelected = { currentShape = it },
                    isFillEnabled = isFillEnabled,
                    onToggleFill = { isFillEnabled = it }
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

        if (showFullColorPicker) {
            PremiumColorPicker(
                selectedColor = currentColor,
                onColorSelected = { color ->
                    toolColors = toolColors.toMutableMap().apply { put(currentTool, color) }
                    showFullColorPicker = false
                },
                onDismiss = { showFullColorPicker = false }
            )
        }
        
        // Add PDF Import Button
        IconButton(
            onClick = { pdfLauncher.launch("application/pdf") },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) { Icon(Icons.Default.PictureAsPdf, null) }
    }
}
