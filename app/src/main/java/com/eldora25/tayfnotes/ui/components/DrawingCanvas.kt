package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.drawing.*
import com.eldora25.tayfnotes.ui.components.canvas.*
import com.eldora25.tayfnotes.ui.theme.EditorNeonIcon
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

import androidx.compose.ui.graphics.toArgb
// ...
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialData: String? = null,
    onDataChanged: (String) -> Unit
) {
    var objects by remember { 
        mutableStateOf(
            if (initialData != null && initialData.isNotEmpty()) {
                try { Json.decodeFromString<List<DrawObject>>(initialData) } catch(e: Exception) { emptyList() }
            } else emptyList()
        )
    }

    var selectedObjectIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Undo/Redo Stacks
    var undoStack by remember { mutableStateOf<List<List<DrawObject>>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<List<DrawObject>>>(emptyList()) }

    fun saveStateForUndo(currentState: List<DrawObject>) {
        undoStack = undoStack + listOf(currentState)
        redoStack = emptyList()
    }

    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val previousState = undoStack.last()
            redoStack = redoStack + listOf(objects)
            objects = previousState
            undoStack = undoStack.dropLast(1)
            onDataChanged(Json.encodeToString(objects))
        }
    }

    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.last()
            undoStack = undoStack + listOf(objects)
            objects = nextState
            redoStack = redoStack.dropLast(1)
            onDataChanged(Json.encodeToString(objects))
        }
    }
    
    // Tools State
    var toolColors by remember { 
        mutableStateOf(
            mapOf(
                ToolType.PEN to Color.Black,
                ToolType.PENCIL to Color.DarkGray,
                ToolType.MARKER to Color.Red,
                ToolType.HIGHLIGHTER to Color.Yellow,
                ToolType.SHAPE to Color.Blue,
                ToolType.BRUSH to Color.Green
            )
        ) 
    }
    
    var currentStrokeWidth by remember { mutableStateOf(10f) }
    var currentTool by remember { mutableStateOf(ToolType.PEN) }
    var currentShape by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var isFillEnabled by remember { mutableStateOf(false) }
    var currentFillColor by remember { mutableStateOf(Color.Transparent) }
    
    val currentColor = toolColors[currentTool] ?: Color.Black

    // Apply settings to selected objects
    LaunchedEffect(currentColor, currentStrokeWidth) {
        if ((currentTool == ToolType.SELECTOR || currentTool == ToolType.LASSO) && selectedObjectIds.isNotEmpty()) {
            val hex = String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
            val newObjects = objects.map { obj ->
                if (selectedObjectIds.contains(obj.id)) {
                    when (obj) {
                        is DrawPath -> obj.copy(colorHex = hex, strokeWidth = currentStrokeWidth)
                        is DrawShape -> obj.copy(colorHex = hex, strokeWidth = currentStrokeWidth)
                    }
                } else obj
            }
            if (newObjects != objects) {
                objects = newObjects
                onDataChanged(Json.encodeToString(objects))
            }
        }
    }

    // UI State
    var showColorPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showShapePicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.White).clipToBounds()) {
        AdvancedCanvasBoard(
            modifier = Modifier.fillMaxSize(),
            currentColor = currentColor,
            currentStrokeWidth = currentStrokeWidth,
            currentTool = currentTool,
            currentShape = currentShape,
            isFillEnabled = isFillEnabled,
            currentFillColor = currentFillColor,
            objects = objects,
            selectedObjectIds = selectedObjectIds,
            onSelectionChanged = { selectedObjectIds = it },
            onObjectAdded = { newObj ->
                saveStateForUndo(objects)
                objects = objects + newObj
                onDataChanged(Json.encodeToString(objects))
            },
            onObjectUpdated = { updatedObj ->
                val index = objects.indexOfFirst { it.id == updatedObj.id }
                if (index != -1) {
                    val newList = objects.toMutableList()
                    newList[index] = updatedObj
                    objects = newList
                    onDataChanged(Json.encodeToString(objects))
                }
            },
            onObjectDeleted = { deletedObj ->
                saveStateForUndo(objects)
                objects = objects.filter { it.id != deletedObj.id }
                onDataChanged(Json.encodeToString(objects))
            }
        )

        if (showColorPicker) {
            PremiumColorPicker(
                selectedColor = currentColor,
                onColorSelected = { selected ->
                    toolColors = toolColors.toMutableMap().apply { put(currentTool, selected) }
                },
                onDismiss = { showColorPicker = false }
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showSettings) {
                CanvasSettingsPopup(
                    activeColor = currentColor,
                    onColorSelected = { showColorPicker = true },
                    activeStrokeWidth = currentStrokeWidth,
                    onStrokeWidthChanged = { currentStrokeWidth = it }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { performUndo() }, enabled = undoStack.isNotEmpty()) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Geri", tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray)
                }

                FloatingToolBar(
                    currentTool = currentTool,
                    onToolSelected = { 
                        if (it == ToolType.SHAPE) showShapePicker = true
                        else if (it == currentTool) showSettings = !showSettings
                        else {
                            currentTool = it
                            showSettings = false
                        }
                    }
                )

                IconButton(onClick = { performRedo() }, enabled = redoStack.isNotEmpty()) {
                    Icon(Icons.AutoMirrored.Filled.Redo, "İleri", tint = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray)
                }
            }
        }
        
        ShapeSelectionBottomSheet(
            isVisible = showShapePicker,
            onDismissRequest = { showShapePicker = false },
            onShapeSelected = { 
                currentShape = it
                currentTool = ToolType.SHAPE
            }
        )
    }
}
