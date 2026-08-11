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
import androidx.compose.ui.text.font.FontWeight
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
    var dragHandle by remember { mutableStateOf<Int?>(null) } // 0: TopLeft, 3: BottomRight, 4: Move
    
    // UI State
    var showColorPicker by remember { mutableStateOf(false) }
    var showShapePicker by remember { mutableStateOf(false) }
    var toolbarOffset by remember { mutableStateOf(Offset(50f, 50f)) }

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentTool, currentShape, currentColor, currentStrokeWidth, isFillEnabled, currentFillColor, selectedObjectId) {
                    if (currentTool == ToolType.SELECT) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val selObj = objects.find { it.id == selectedObjectId }
                                if (selObj != null) {
                                    val bounds = getObjectBounds(selObj)
                                    if ((Offset(bounds.left, bounds.top) - offset).getDistance() < 30f) dragHandle = 0
                                    else if ((Offset(bounds.right, bounds.bottom) - offset).getDistance() < 30f) dragHandle = 3
                                    else if (bounds.contains(offset)) dragHandle = 4
                                    else dragHandle = null
                                }
                                
                                if (dragHandle == null) {
                                    val hit = objects.findLast { obj -> getObjectBounds(obj).contains(offset) }
                                    selectedObjectId = hit?.id
                                    if (selectedObjectId != null) dragHandle = 4
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val selId = selectedObjectId ?: return@detectDragGestures
                                objects = objects.map { obj ->
                                    if (obj.id == selId) {
                                        when (dragHandle) {
                                            4 -> obj.copy(points = obj.points.map { Point(it.x + dragAmount.x, it.y + dragAmount.y) })
                                            0 -> {
                                                val newPoints = obj.points.toMutableList()
                                                if (newPoints.isNotEmpty()) newPoints[0] = Point(newPoints[0].x + change.position.x - (newPoints[0].x), newPoints[0].y + change.position.y - (newPoints[0].y))
                                                obj.copy(points = newPoints)
                                            }
                                            3 -> {
                                                val newPoints = obj.points.toMutableList()
                                                val idx = if (obj.toolType == ToolType.SHAPE) 1 else newPoints.size - 1
                                                if (idx >= 0 && idx < newPoints.size) {
                                                    newPoints[idx] = Point(change.position.x, change.position.y)
                                                }
                                                obj.copy(points = newPoints)
                                            }
                                            else -> obj
                                        }
                                    } else obj
                                }
                            },
                            onDragEnd = {
                                dragHandle = null
                                onDataChanged(Json.encodeToString(objects))
                            }
                        )
                    } else if (currentTool == ToolType.OBJECT_ERASER) {
                        detectTapGestures { offset ->
                            val hit = objects.findLast { obj -> getObjectBounds(obj).contains(offset) }
                            if (hit != null) {
                                objects = objects.filter { it.id != hit.id }
                                selectedObjectId = null
                                onDataChanged(Json.encodeToString(objects))
                            }
                        }
                    } else if (currentTool == ToolType.PAINT_BUCKET) {
                        detectTapGestures { offset ->
                            val hits = objects.filter { obj -> 
                                val bounds = getObjectBounds(obj)
                                bounds.contains(offset)
                            }
                            if (hits.size >= 2) {
                                // Find intersection of the first two for simplicity
                                val path1 = getObjectPath(hits[hits.size - 1])
                                val path2 = getObjectPath(hits[hits.size - 2])
                                val intersection = Path().apply {
                                    op(path1, path2, PathOperation.Intersect)
                                }
                                if (!intersection.isEmpty) {
                                    val newObj = DrawObject(
                                        id = java.util.UUID.randomUUID().toString(),
                                        points = emptyList(),
                                        colorHex = String.format("#%06X", (0xFFFFFF and currentColor.toArgb())),
                                        strokeWidth = 2f,
                                        toolType = ToolType.SHAPE,
                                        shapeType = ShapeType.INTERSECTION,
                                        isFilled = true,
                                        fillColorHex = String.format("#%06X", (0xFFFFFF and currentColor.toArgb())),
                                        zIndex = objects.size,
                                        pathData = hits[hits.size - 1].id + "|" + hits[hits.size - 2].id
                                    )
                                    objects = objects + newObj
                                    onDataChanged(Json.encodeToString(objects))
                                }
                            } else if (hits.size == 1) {
                                objects = objects.map { obj ->
                                    if (obj.id == hits[0].id) obj.copy(isFilled = true, fillColorHex = String.format("#%06X", (0xFFFFFF and currentColor.toArgb())))
                                    else obj
                                }
                                onDataChanged(Json.encodeToString(objects))
                            }
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
                drawDrawObject(obj, objects, isSelected = obj.id == selectedObjectId)
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
                drawDrawObject(preview, objects, isSelected = false)
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
                    ToolIcon(Icons.Default.FormatPaint, "Kova", currentTool == ToolType.PAINT_BUCKET) { currentTool = ToolType.PAINT_BUCKET }
                    ToolIcon(Icons.Default.AutoFixNormal, "Obje Silgi", currentTool == ToolType.OBJECT_ERASER) { currentTool = ToolType.OBJECT_ERASER }
                    ToolIcon(Icons.Default.CleaningServices, "Piksel Silgi", currentTool == ToolType.PIXEL_ERASER) { currentTool = ToolType.PIXEL_ERASER }
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
            title = { Text("Şekil Seçin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                val shapes = listOf(
                    ShapeType.SQUARE to Icons.Default.Square,
                    ShapeType.RECTANGLE to Icons.Default.Rectangle,
                    ShapeType.CIRCLE to Icons.Default.Circle,
                    ShapeType.ELLIPSE to Icons.Default.FilterTiltShift,
                    ShapeType.EQUILATERAL_TRIANGLE to Icons.Default.ChangeHistory,
                    ShapeType.RIGHT_TRIANGLE to Icons.Default.Details,
                    ShapeType.TRAPEZOID to Icons.Default.Category,
                    ShapeType.PARALLELOGRAM to Icons.Default.Layers,
                    ShapeType.DIAMOND to Icons.Default.Diamond,
                    ShapeType.PENTAGON to Icons.Default.Pentagon,
                    ShapeType.HEXAGON to Icons.Default.Hexagon,
                    ShapeType.STAR to Icons.Default.Star,
                    ShapeType.ARC to Icons.Default.Architecture,
                    ShapeType.LINE to Icons.Default.HorizontalRule,
                    ShapeType.DOUBLE_ARROW to Icons.Default.SwapHoriz
                )
                
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    modifier = Modifier.height(300.dp).fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(shapes.size) { index ->
                        val (shape, icon) = shapes[index]
                        Surface(
                            onClick = {
                                currentTool = ToolType.SHAPE
                                currentShape = shape
                                showShapePicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentShape == shape) Color(0xFFFFD700).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (currentShape == shape) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD700)) else null,
                            modifier = Modifier.aspectRatio(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = shape.name, modifier = Modifier.size(28.dp), tint = if (currentShape == shape) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showShapePicker = false }) { Text("Kapat") } }
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDrawObject(obj: DrawObject, allObjects: List<DrawObject>, isSelected: Boolean) {
    val color = if (obj.toolType == ToolType.PIXEL_ERASER) Color.White else Color(android.graphics.Color.parseColor(obj.colorHex)).run {
        if (obj.toolType == ToolType.MARKER) this.copy(alpha = 0.45f) else this
    }
    val fillColor = if (obj.isFilled && obj.fillColorHex != null) Color(android.graphics.Color.parseColor(obj.fillColorHex)) else Color.Transparent
    val blendMode = if (obj.toolType == ToolType.MARKER) BlendMode.Multiply else BlendMode.SrcOver
    val pathData = obj.pathData

    if (obj.shapeType == ShapeType.INTERSECTION && pathData != null) {
        val ids = pathData.split("|")
        if (ids.size >= 2) {
            val src1 = allObjects.find { it.id == ids[0] }
            val src2 = allObjects.find { it.id == ids[1] }
            if (src1 != null && src2 != null) {
                val p1 = getObjectPath(src1)
                val p2 = getObjectPath(src2)
                val intersection = Path().apply { op(p1, p2, PathOperation.Intersect) }
                drawPath(intersection, fillColor)
                drawPath(intersection, color, style = Stroke(width = obj.strokeWidth))
            }
        }
    } else if (obj.toolType == ToolType.SHAPE && obj.points.size >= 2) {
        val start = Offset(obj.points[0].x, obj.points[0].y)
        val end = Offset(obj.points[1].x, obj.points[1].y)
        val left = minOf(start.x, end.x)
        val top = minOf(start.y, end.y)
        val width = Math.abs(start.x - end.x)
        val height = Math.abs(start.y - end.y)

        when (obj.shapeType) {
            ShapeType.SQUARE -> {
                val side = minOf(width, height)
                if (obj.isFilled) drawRect(fillColor, Offset(left, top), Size(side, side))
                drawRect(color, Offset(left, top), Size(side, side), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.RECTANGLE -> {
                if (obj.isFilled) drawRect(fillColor, Offset(left, top), Size(width, height))
                drawRect(color, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.CIRCLE -> {
                val radius = minOf(width, height) / 2
                if (obj.isFilled) drawCircle(fillColor, radius, Offset(left + width/2, top + height/2))
                drawCircle(color, radius, Offset(left + width/2, top + height/2), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.ELLIPSE -> {
                if (obj.isFilled) drawOval(fillColor, Offset(left, top), Size(width, height))
                drawOval(color, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.EQUILATERAL_TRIANGLE -> {
                val path = Path().apply {
                    moveTo(left + width/2, top)
                    lineTo(left, top + height)
                    lineTo(left + width, top + height)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.RIGHT_TRIANGLE -> {
                val path = Path().apply {
                    moveTo(left, top)
                    lineTo(left, top + height)
                    lineTo(left + width, top + height)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.TRAPEZOID -> {
                val path = Path().apply {
                    moveTo(left + width * 0.25f, top)
                    lineTo(left + width * 0.75f, top)
                    lineTo(left + width, top + height)
                    lineTo(left, top + height)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.PARALLELOGRAM -> {
                val path = Path().apply {
                    moveTo(left + width * 0.25f, top)
                    lineTo(left + width, top)
                    lineTo(left + width * 0.75f, top + height)
                    lineTo(left, top + height)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.DIAMOND -> {
                val path = Path().apply {
                    moveTo(left + width/2, top)
                    lineTo(left + width, top + height/2)
                    lineTo(left + width/2, top + height)
                    lineTo(left, top + height/2)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.PENTAGON -> {
                val path = drawPolygon(left, top, width, height, 5)
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.HEXAGON -> {
                val path = drawPolygon(left, top, width, height, 6)
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.STAR -> {
                val path = drawStar(left, top, width, height)
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.ARC -> {
                drawArc(color, 0f, 180f, false, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.LINE -> {
                drawLine(color, start, end, strokeWidth = obj.strokeWidth)
            }
            ShapeType.DOUBLE_ARROW -> {
                drawLine(color, start, end, strokeWidth = obj.strokeWidth)
                drawArrowHead(start, end, color, obj.strokeWidth)
                drawArrowHead(end, start, color, obj.strokeWidth)
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
        
        // Drag Handles
        drawCircle(Color.White, radius = 12f, center = bounds.topLeft, style = Stroke(width = 3f))
        drawCircle(Color.Blue, radius = 10f, center = bounds.topLeft)
        
        drawCircle(Color.White, radius = 12f, center = bounds.bottomRight, style = Stroke(width = 3f))
        drawCircle(Color.Blue, radius = 10f, center = bounds.bottomRight)
    }
}

private fun getObjectPath(obj: DrawObject): Path {
    val path = Path()
    if (obj.toolType == ToolType.SHAPE && obj.points.size >= 2) {
        val start = Offset(obj.points[0].x, obj.points[0].y)
        val end = Offset(obj.points[1].x, obj.points[1].y)
        val left = minOf(start.x, end.x)
        val top = minOf(start.y, end.y)
        val width = Math.abs(start.x - end.x)
        val height = Math.abs(start.y - end.y)
        
        when (obj.shapeType) {
            ShapeType.SQUARE -> {
                val side = minOf(width, height)
                path.addRect(Rect(Offset(left, top), Size(side, side)))
            }
            ShapeType.RECTANGLE -> path.addRect(Rect(Offset(left, top), Size(width, height)))
            ShapeType.CIRCLE -> {
                val radius = minOf(width, height) / 2
                path.addOval(Rect(Offset(left + width/2 - radius, top + height/2 - radius), Size(radius * 2, radius * 2)))
            }
            ShapeType.ELLIPSE -> path.addOval(Rect(Offset(left, top), Size(width, height)))
            ShapeType.EQUILATERAL_TRIANGLE -> {
                path.moveTo(left + width/2, top)
                path.lineTo(left, top + height)
                path.lineTo(left + width, top + height)
                path.close()
            }
            ShapeType.RIGHT_TRIANGLE -> {
                path.moveTo(left, top)
                path.lineTo(left, top + height)
                path.lineTo(left + width, top + height)
                path.close()
            }
            ShapeType.DIAMOND -> {
                path.moveTo(left + width/2, top)
                path.lineTo(left + width, top + height/2)
                path.lineTo(left + width/2, top + height)
                path.lineTo(left, top + height/2)
                path.close()
            }
            // Add other shapes as needed for intersection
            else -> path.addRect(Rect(Offset(left, top), Size(width, height)))
        }
    } else if (obj.points.isNotEmpty()) {
        path.moveTo(obj.points[0].x, obj.points[0].y)
        obj.points.forEach { path.lineTo(it.x, it.y) }
    }
    return path
}

private fun Color.toArgb(): Int {
    return (alpha * 255.0f + 0.5f).toInt() shl 24 or (red * 255.0f + 0.5f).toInt() shl 16 or (green * 255.0f + 0.5f).toInt() shl 8 or (blue * 255.0f + 0.5f).toInt()
}

private fun drawPolygon(left: Float, top: Float, width: Float, height: Float, sides: Int): Path {
    val path = Path()
    val centerX = left + width / 2
    val centerY = top + height / 2
    val radius = minOf(width, height) / 2
    for (i in 0 until sides) {
        val angle = 2.0 * Math.PI * i / sides - Math.PI / 2
        val x = centerX + radius * Math.cos(angle).toFloat()
        val y = centerY + radius * Math.sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun drawStar(left: Float, top: Float, width: Float, height: Float): Path {
    val path = Path()
    val centerX = left + width / 2
    val centerY = top + height / 2
    val outerRadius = minOf(width, height) / 2
    val innerRadius = outerRadius * 0.4f
    for (i in 0 until 10) {
        val angle = Math.PI * i / 5 - Math.PI / 2
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = centerX + r * Math.cos(angle).toFloat()
        val y = centerY + r * Math.sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(start: Offset, end: Offset, color: Color, strokeWidth: Float) {
    val angle = Math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val arrowLength = 20f + strokeWidth
    val arrowAngle = Math.PI / 6
    val x1 = end.x - arrowLength * Math.cos(angle - arrowAngle).toFloat()
    val y1 = end.y - arrowLength * Math.sin(angle - arrowAngle).toFloat()
    val x2 = end.x - arrowLength * Math.cos(angle + arrowAngle).toFloat()
    val y2 = end.y - arrowLength * Math.sin(angle + arrowAngle).toFloat()
    drawLine(color, end, Offset(x1, y1), strokeWidth = strokeWidth)
    drawLine(color, end, Offset(x2, y2), strokeWidth = strokeWidth)
}
