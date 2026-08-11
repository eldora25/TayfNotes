package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.drawing.*
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
    
    var selectedObjectId by remember { mutableStateOf<String?>(null) }
    val currentPathPoints = remember { mutableStateListOf<Point>() }
    
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
    var toolbarOffset by remember { mutableStateOf(Offset(50f, 50f)) }

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentTool, currentShape, currentColor, currentStrokeWidth, isFillEnabled, currentFillColor) {
                    if (currentTool == ToolType.SELECT) {
                        detectTapGestures { offset ->
                            val hit = objects.findLast { obj -> getObjectBounds(obj).contains(offset) }
                            selectedObjectId = hit?.id
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selectedObjectId = null
                                currentPathPoints.clear()
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
                                    val newObj = DrawObject(
                                        id = java.util.UUID.randomUUID().toString(),
                                        points = currentPathPoints.toList(),
                                        colorHex = String.format("#%06X", (0xFFFFFF and currentColor.toArgb())),
                                        strokeWidth = currentStrokeWidth,
                                        toolType = currentTool,
                                        shapeType = if (currentTool == ToolType.SHAPE) currentShape else null,
                                        isFilled = isFillEnabled,
                                        fillColorHex = if (isFillEnabled) String.format("#%06X", (0xFFFFFF and currentFillColor.toArgb())) else null,
                                        zIndex = objects.size
                                    )
                                    objects = objects + newObj
                                    currentPathPoints.clear()
                                    onDataChanged(Json.encodeToString(objects))
                                }
                            }
                        )
                    }
                }
        ) {
            objects.forEach { obj ->
                drawDrawObject(obj, isSelected = obj.id == selectedObjectId)
            }
            
            if (currentPathPoints.isNotEmpty()) {
                val preview = DrawObject(
                    id = "preview",
                    points = currentPathPoints.toList(),
                    colorHex = String.format("#%06X", (0xFFFFFF and currentColor.toArgb())),
                    strokeWidth = currentStrokeWidth,
                    toolType = currentTool,
                    shapeType = if (currentTool == ToolType.SHAPE) currentShape else null,
                    isFilled = isFillEnabled,
                    fillColorHex = if (isFillEnabled) String.format("#%06X", (0xFFFFFF and currentFillColor.toArgb())) else null
                )
                drawDrawObject(preview, isSelected = false)
            }
        }

        // Floating Toolbar
        Surface(
            modifier = Modifier
                .offset { IntOffset(toolbarOffset.x.roundToInt(), toolbarOffset.y.roundToInt()) }
                .width(280.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        toolbarOffset += dragAmount
                    }
                },
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.85f),
            tonalElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    ToolIcon(Icons.Default.PanToolAlt, "Seç", currentTool == ToolType.SELECT) { currentTool = ToolType.SELECT }
                    ToolIcon(Icons.Default.Create, "Kalem", currentTool == ToolType.PEN) { currentTool = ToolType.PEN }
                    ToolIcon(Icons.Default.Brush, "Fırça", currentTool == ToolType.MARKER) { currentTool = ToolType.MARKER }
                    ToolIcon(Icons.Default.Category, "Şekil", currentTool == ToolType.SHAPE) { showShapePicker = true }
                    ToolIcon(Icons.Default.AutoFixNormal, "Silgi", currentTool == ToolType.ERASER) { currentTool = ToolType.ERASER }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
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

        // Contextual Menu
        if (selectedObjectId != null) {
            val selObj = objects.find { it.id == selectedObjectId }
            if (selObj != null) {
                val bounds = getObjectBounds(selObj)
                Surface(
                    modifier = Modifier.offset { IntOffset(bounds.left.roundToInt(), (bounds.top - 70f).roundToInt()) }.shadow(8.dp, RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        IconButton(onClick = { objects = objects.filter { it.id != selectedObjectId }; selectedObjectId = null; onDataChanged(Json.encodeToString(objects)) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red)
                        }
                        IconButton(onClick = { showColorPicker = true }) { Icon(Icons.Default.Palette, null) }
                    }
                }
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

    if (showShapePicker) {
        AlertDialog(
            onDismissRequest = { showShapePicker = false },
            title = { Text("Şekil Seç") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.RECTANGLE; showShapePicker = false }) { Icon(Icons.Default.Rectangle, null) }
                        IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.CIRCLE; showShapePicker = false }) { Icon(Icons.Default.Circle, null) }
                        IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.TRIANGLE; showShapePicker = false }) { Icon(Icons.Default.ChangeHistory, null) }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.ELLIPSE; showShapePicker = false }) { Icon(Icons.Default.FilterTiltShift, null) }
                        IconButton(onClick = { currentTool = ToolType.SHAPE; currentShape = ShapeType.ARC; showShapePicker = false }) { Icon(Icons.Default.Architecture, null) }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun ToolIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = if (isSelected) Color(0xFFFFD700) else Color.White)
    }
}

private fun getObjectBounds(obj: DrawObject): Rect {
    if (obj.points.isEmpty()) return Rect.Zero
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    obj.points.forEach {
        minX = minOf(minX, it.x)
        minY = minOf(minY, it.y)
        maxX = maxOf(maxX, it.x)
        maxY = maxOf(maxY, it.y)
    }
    return Rect(minX, minY, maxX, maxY)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDrawObject(obj: DrawObject, isSelected: Boolean) {
    val color = if (obj.toolType == ToolType.ERASER) Color.White else Color(android.graphics.Color.parseColor(obj.colorHex)).run {
        if (obj.toolType == ToolType.MARKER) this.copy(alpha = 0.45f) else this
    }
    val fillColor = if (obj.isFilled && obj.fillColorHex != null) Color(android.graphics.Color.parseColor(obj.fillColorHex)) else Color.Transparent
    val blendMode = if (obj.toolType == ToolType.MARKER) BlendMode.Multiply else BlendMode.SrcOver

    if (obj.toolType == ToolType.SHAPE && obj.points.size >= 2) {
        val start = Offset(obj.points[0].x, obj.points[0].y)
        val end = Offset(obj.points[1].x, obj.points[1].y)
        val left = minOf(start.x, end.x)
        val top = minOf(start.y, end.y)
        val width = Math.abs(start.x - end.x)
        val height = Math.abs(start.y - end.y)

        when (obj.shapeType) {
            ShapeType.RECTANGLE -> {
                if (obj.isFilled) drawRect(fillColor, Offset(left, top), Size(width, height))
                drawRect(color, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.CIRCLE -> {
                val radius = Math.sqrt((width * width + height * height).toDouble()).toFloat() / 2
                if (obj.isFilled) drawCircle(fillColor, radius, Offset(left + width/2, top + height/2))
                drawCircle(color, radius, Offset(left + width/2, top + height/2), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(left + width/2, top)
                    lineTo(left, top + height)
                    lineTo(left + width, top + height)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.ELLIPSE -> {
                if (obj.isFilled) drawOval(fillColor, Offset(left, top), Size(width, height))
                drawOval(color, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.ARC -> {
                drawArc(color, 0f, 180f, false, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
            }
            else -> {}
        }
    } else {
        val path = Path()
        if (obj.points.isNotEmpty()) {
            path.moveTo(obj.points[0].x, obj.points[0].y)
            obj.points.forEach { path.lineTo(it.x, it.y) }
            drawPath(path = path, color = color, style = Stroke(width = obj.strokeWidth, cap = StrokeCap.Round), blendMode = blendMode)
        }
    }
    
    if (isSelected) {
        val bounds = getObjectBounds(obj)
        drawRect(Color.Blue.copy(alpha = 0.3f), bounds.topLeft, bounds.size, style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
    }
}

private fun Color.toArgb(): Int {
    return (alpha * 255.0f + 0.5f).toInt() shl 24 or (red * 255.0f + 0.5f).toInt() shl 16 or (green * 255.0f + 0.5f).toInt() shl 8 or (blue * 255.0f + 0.5f).toInt()
}
