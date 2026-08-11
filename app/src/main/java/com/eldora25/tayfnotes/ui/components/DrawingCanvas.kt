package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.drawing.*
import com.eldora25.tayfnotes.ui.components.canvas.AdvancedCanvasBoard
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
    
    // Tools State
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentFillColor by remember { mutableStateOf(Color.Transparent) }
    var currentStrokeWidth by remember { mutableStateOf(10f) }
    var currentTool by remember { mutableStateOf(ToolType.PEN) }
    var currentShape by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var isFillEnabled by remember { mutableStateOf(false) }
    
    // UI State
    var showColorPicker by remember { mutableStateOf(false) }
    var showShapePicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
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
                objects = objects + newObj
                onDataChanged(Json.encodeToString(objects))
            },
            onObjectUpdated = { updatedObj ->
                objects = objects.map { if (it.id == updatedObj.id) updatedObj else it }
                onDataChanged(Json.encodeToString(objects))
            },
            onObjectDeleted = { deletedObj ->
                objects = objects.filter { it.id != deletedObj.id }
                onDataChanged(Json.encodeToString(objects))
            }
        )

        FloatingToolBar(
            currentTool = currentTool,
            onToolSelected = { 
                if (it == ToolType.SHAPE) showShapePicker = true
                else currentTool = it 
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        )
        
        ShapeSelectionBottomSheet(
            isVisible = showShapePicker,
            onDismissRequest = { showShapePicker = false },
            onShapeSelected = { 
                currentShape = it
                currentTool = ToolType.SHAPE
            }
        )

        // Color and Stroke Row (above toolbar)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp)
                .width(280.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.85f),
            tonalElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clickable { showColorPicker = true }.padding(4.dp)) {
                    EditorNeonIcon(modifier = Modifier.size(32.dp)) {
                        Box(modifier = Modifier.size(20.dp).background(currentColor, CircleShape).border(1.dp, Color.White, CircleShape))
                    }
                }
                Slider(
                    value = currentStrokeWidth,
                    onValueChange = { currentStrokeWidth = it },
                    valueRange = 1f..100f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                )
            }
        }
    }

    // Dialogs (Color, Shape)
    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Renk ve Dolgu") },
            text = {
                Column {
                    val colors = listOf(Color.Black, Color.DarkGray, Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan, Color.White)
                    Text("Ana Renk", style = MaterialTheme.typography.labelSmall)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        colors.forEach { color ->
                            Box(modifier = Modifier.padding(4.dp).size(32.dp).background(color, CircleShape).border(if (currentColor == color) 2.dp else 0.dp, Color.Gray, CircleShape).clickable { currentColor = color })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isFillEnabled, onCheckedChange = { isFillEnabled = it })
                        Text("Dolgu Rengi Aktif")
                    }
                    if (isFillEnabled) {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            colors.forEach { color ->
                                Box(modifier = Modifier.padding(4.dp).size(32.dp).background(color, CircleShape).border(if (currentFillColor == color) 2.dp else 0.dp, Color.Gray, CircleShape).clickable { currentFillColor = color })
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorPicker = false }) { Text("Tamam") } }
        )
    }
}

@Composable
private fun ToolIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = if (isSelected) Color(0xFFFFD700) else Color.White)
    }
}
