package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.ui.theme.EditorNeonIcon
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DrawPath(
    val points: List<Point>,
    val colorHex: String,
    val strokeWidth: Float,
    val toolType: ToolType = ToolType.PEN,
    val shapeType: ShapeType? = null,
    val isFilled: Boolean = false,
    val fillColorHex: String? = null
)

enum class ToolType { PEN, MARKER, ERASER, SHAPE }
enum class ShapeType { RECTANGLE, CIRCLE, TRIANGLE, ELLIPSE, ARC }

@Serializable
data class Point(val x: Float, val y: Float)

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialData: String? = null,
    onDataChanged: (String) -> Unit
) {
    var paths by remember { 
        mutableStateOf(
            if (initialData != null && initialData.isNotEmpty()) {
                try { Json.decodeFromString<List<DrawPath>>(initialData) } catch(e: Exception) { emptyList() }
            } else emptyList()
        )
    }
    
    val currentPathPoints = remember { mutableStateListOf<Point>() }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentFillColor by remember { mutableStateOf(Color.Transparent) }
    var currentStrokeWidth by remember { mutableStateOf(10f) }
    var currentTool by remember { mutableStateOf(ToolType.PEN) }
    var currentShape by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var isFillEnabled by remember { mutableStateOf(false) }
    
    var showColorPicker by remember { mutableStateOf(false) }
    var showShapePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        // Toolbar - Fixed to be more distinct (Madde 2)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.9f),
            tonalElevation = 8.dp
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { currentTool = ToolType.PEN }) {
                        Icon(Icons.Default.Create, contentDescription = "Kalem", tint = if (currentTool == ToolType.PEN) Color(0xFFFFD700) else Color.White)
                    }
                    IconButton(onClick = { currentTool = ToolType.MARKER }) {
                        Icon(Icons.Default.Brush, contentDescription = "Fırça", tint = if (currentTool == ToolType.MARKER) Color(0xFFFFD700) else Color.White)
                    }
                    IconButton(onClick = { currentTool = ToolType.ERASER }) {
                        Icon(Icons.Default.AutoFixNormal, contentDescription = "Silgi", tint = if (currentTool == ToolType.ERASER) Color(0xFFFFD700) else Color.White)
                    }
                    IconButton(onClick = { showShapePicker = true }) {
                        Icon(Icons.Default.Category, contentDescription = "Şekiller", tint = if (currentTool == ToolType.SHAPE) Color(0xFFFFD700) else Color.White)
                    }
                    
                    Box(modifier = Modifier.clickable { showColorPicker = true }) {
                        EditorNeonIcon(modifier = Modifier.size(36.dp)) {
                            Box(modifier = Modifier.size(20.dp).background(if (currentColor == Color.White && currentTool != ToolType.ERASER) Color.LightGray else currentColor, CircleShape).border(1.dp, Color.White, CircleShape))
                        }
                    }

                    IconButton(onClick = { 
                        paths = emptyList()
                        currentPathPoints.clear()
                        onDataChanged("")
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Temizle", tint = Color.Red)
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LineWeight, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Slider(
                        value = currentStrokeWidth,
                        onValueChange = { currentStrokeWidth = it },
                        valueRange = 1f..100f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFFD700), activeTrackColor = Color(0xFFFFD700))
                    )
                    Text("${currentStrokeWidth.toInt()}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .pointerInput(currentTool, currentShape, currentColor, currentStrokeWidth, isFillEnabled, currentFillColor) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPathPoints.add(Point(offset.x, offset.y))
                        },
                        onDrag = { change, _ ->
                            if (currentTool != ToolType.SHAPE) {
                                currentPathPoints.add(Point(change.position.x, change.position.y))
                            } else {
                                if (currentPathPoints.size > 1) currentPathPoints.removeAt(1)
                                currentPathPoints.add(Point(change.position.x, change.position.y))
                            }
                        },
                        onDragEnd = {
                            if (currentPathPoints.isNotEmpty()) {
                                val colorString = String.format("#%06X", (0xFFFFFF and currentColor.toArgb()))
                                val fillColorString = if (isFillEnabled) String.format("#%06X", (0xFFFFFF and currentFillColor.toArgb())) else null
                                val newPath = DrawPath(
                                    points = currentPathPoints.toList(),
                                    colorHex = colorString,
                                    strokeWidth = currentStrokeWidth,
                                    toolType = currentTool,
                                    shapeType = if (currentTool == ToolType.SHAPE) currentShape else null,
                                    isFilled = isFillEnabled,
                                    fillColorHex = fillColorString
                                )
                                paths = paths + newPath
                                currentPathPoints.clear()
                                onDataChanged(Json.encodeToString(paths))
                            }
                        }
                    )
                }
        ) {
            paths.forEach { drawDataPath(it) }
            
            if (currentPathPoints.isNotEmpty()) {
                val colorString = String.format("#%06X", (0xFFFFFF and currentColor.toArgb()))
                val fillColorString = if (isFillEnabled) String.format("#%06X", (0xFFFFFF and currentFillColor.toArgb())) else null
                val previewPath = DrawPath(
                    points = currentPathPoints.toList(),
                    colorHex = colorString,
                    strokeWidth = currentStrokeWidth,
                    toolType = currentTool,
                    shapeType = if (currentTool == ToolType.SHAPE) currentShape else null,
                    isFilled = isFillEnabled,
                    fillColorHex = fillColorString
                )
                drawDataPath(previewPath)
            }
        }
    }
    
    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Renk ve Dolgu Seçimi") },
            text = {
                Column {
                    val colors = listOf(Color.Black, Color.DarkGray, Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan, Color.White)
                    Text("Ana Renk (Çizgi)", style = MaterialTheme.typography.labelSmall)
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        colors.forEach { color ->
                            Box(modifier = Modifier.padding(4.dp).size(32.dp).background(color, CircleShape).border(if (currentColor == color) 2.dp else 0.dp, Color.Gray, CircleShape).clickable { currentColor = color })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isFillEnabled, onCheckedChange = { isFillEnabled = it })
                        Text("İç Dolgu Aktif")
                    }
                    if (isFillEnabled) {
                        Text("Dolgu Rengi", style = MaterialTheme.typography.labelSmall)
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

    if (showShapePicker) {
        AlertDialog(
            onDismissRequest = { showShapePicker = false },
            title = { Text("Şekil Seç") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.RECTANGLE; showShapePicker = false }) {
                            Icon(Icons.Default.Rectangle, contentDescription = "Kare")
                        }
                        IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.CIRCLE; showShapePicker = false }) {
                            Icon(Icons.Default.Circle, contentDescription = "Daire")
                        }
                        IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.TRIANGLE; showShapePicker = false }) {
                            Icon(Icons.Default.ChangeHistory, contentDescription = "Üçgen")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.ELLIPSE; showShapePicker = false }) {
                            Icon(Icons.Default.FilterTiltShift, contentDescription = "Elips")
                        }
                        IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.ARC; showShapePicker = false }) {
                            Icon(Icons.Default.Architecture, contentDescription = "Yay")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

private fun Color.toArgb(): Int {
    return (alpha * 255.0f + 0.5f).toInt() shl 24 or
           (red * 255.0f + 0.5f).toInt() shl 16 or
           (green * 255.0f + 0.5f).toInt() shl 8 or
           (blue * 255.0f + 0.5f).toInt()
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDataPath(drawPath: DrawPath) {
    val color = if (drawPath.toolType == ToolType.ERASER) Color.White else Color(android.graphics.Color.parseColor(drawPath.colorHex)).run {
        if (drawPath.toolType == ToolType.MARKER) this.copy(alpha = 0.45f) else this
    }
    val fillColor = if (drawPath.isFilled && drawPath.fillColorHex != null) Color(android.graphics.Color.parseColor(drawPath.fillColorHex)) else Color.Transparent

    if (drawPath.toolType == ToolType.SHAPE && drawPath.points.size >= 2) {
        val start = Offset(drawPath.points[0].x, drawPath.points[0].y)
        val end = Offset(drawPath.points[1].x, drawPath.points[1].y)
        val left = minOf(start.x, end.x)
        val top = minOf(start.y, end.y)
        val width = Math.abs(start.x - end.x)
        val height = Math.abs(start.y - end.y)

        when (drawPath.shapeType) {
            ShapeType.RECTANGLE -> {
                if (drawPath.isFilled) drawRect(fillColor, Offset(left, top), Size(width, height))
                drawRect(color, Offset(left, top), Size(width, height), style = Stroke(width = drawPath.strokeWidth))
            }
            ShapeType.CIRCLE -> {
                val radius = Math.sqrt((width * width + height * height).toDouble()).toFloat() / 2
                if (drawPath.isFilled) drawCircle(fillColor, radius, Offset(left + width/2, top + height/2))
                drawCircle(color, radius, Offset(left + width/2, top + height/2), style = Stroke(width = drawPath.strokeWidth))
            }
            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(left + width/2, top)
                    lineTo(left, top + height)
                    lineTo(left + width, top + height)
                    close()
                }
                if (drawPath.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = drawPath.strokeWidth))
            }
            ShapeType.ELLIPSE -> {
                if (drawPath.isFilled) drawOval(fillColor, Offset(left, top), Size(width, height))
                drawOval(color, Offset(left, top), Size(width, height), style = Stroke(width = drawPath.strokeWidth))
            }
            ShapeType.ARC -> {
                drawArc(color, 0f, 180f, false, Offset(left, top), Size(width, height), style = Stroke(width = drawPath.strokeWidth))
            }
            else -> {}
        }
    } else {
        val path = Path()
        if (drawPath.points.isNotEmpty()) {
            path.moveTo(drawPath.points[0].x, drawPath.points[0].y)
            drawPath.points.forEach { path.lineTo(it.x, it.y) }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = drawPath.strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
