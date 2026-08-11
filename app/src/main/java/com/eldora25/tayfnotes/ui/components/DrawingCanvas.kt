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
import com.eldora25.tayfnotes.ui.components.canvas.AdvancedCanvasBoard
import com.eldora25.tayfnotes.ui.components.canvas.CanvasSettingsPopup
import com.eldora25.tayfnotes.ui.components.canvas.FloatingToolBar
import com.eldora25.tayfnotes.ui.components.canvas.ShapeSelectionBottomSheet
import com.eldora25.tayfnotes.ui.theme.EditorNeonIcon
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

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
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentFillColor by remember { mutableStateOf(Color.Transparent) }
    var currentStrokeWidth by remember { mutableStateOf(10f) }
    var currentTool by remember { mutableStateOf(ToolType.PEN) }
    var currentShape by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var isFillEnabled by remember { mutableStateOf(false) }
    
    // UI State
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
            onObjectAdded = { newObj ->
                saveStateForUndo(objects)
                objects = objects + newObj
                onDataChanged(Json.encodeToString(objects))
            },
            onObjectUpdated = { updatedObj ->
                // Don't save state for every small update (like drag)
                // objects = objects.map { if (it.id == updatedObj.id) updatedObj else it }
                
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

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showSettings) {
                CanvasSettingsPopup(
                    activeColor = currentColor,
                    onColorSelected = { currentColor = it },
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

@Composable
private fun ToolIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = if (isSelected) Color(0xFFFFD700) else Color.White)
    }
}
