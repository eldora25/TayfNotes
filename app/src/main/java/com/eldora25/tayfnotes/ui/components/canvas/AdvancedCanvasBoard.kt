package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.shared.model.drawing.*
import java.util.UUID

import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
// ...
@Composable
fun AdvancedCanvasBoard(
    modifier: Modifier = Modifier,
    currentColor: Color = Color.Black,
    currentStrokeWidth: Float = 5f,
    currentTool: ToolType = ToolType.PEN,
    currentShape: ShapeType = ShapeType.RECTANGLE,
    isFillEnabled: Boolean = false,
    currentFillColor: Color = Color.Transparent,
    objects: List<DrawObject>,
    selectedObjectIds: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onObjectAdded: (DrawObject) -> Unit,
    onObjectUpdated: (DrawObject) -> Unit,
    onObjectDeleted: (DrawObject) -> Unit
) {
    var currentPathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var tempShape by remember { mutableStateOf<DrawShape?>(null) }
    var lassoPath by remember { mutableStateOf<Path?>(null) }
    
    // Global Canvas Transform (Madde 1: Pan/Zoom)
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    var globalScale by remember { mutableStateOf(1f) }

    val latestObjects by rememberUpdatedState(objects)
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                
                // 1. GLOBAL PAN / ZOOM
                .pointerInput(currentTool) {
                    if (currentTool == ToolType.PAN) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            globalOffset += pan
                            globalScale *= zoom
                        }
                    }
                }

                // 1.5 TEXT TOOL
                .pointerInput(currentTool, globalOffset, globalScale) {
                    if (currentTool == ToolType.TEXT) {
                        detectTapGestures { offset ->
                            val adjusted = (offset - globalOffset) / globalScale
                            // Trigger text input dialog (hoisted via onObjectAdded with a temp object or separate callback)
                            onObjectAdded(DrawText(
                                id = UUID.randomUUID().toString(),
                                colorHex = String.format("#%06X", 0xFFFFFF and currentColor.toArgb()),
                                strokeWidth = currentStrokeWidth * 5f, // Use strokeWidth as base font size
                                offsetX = adjusted.x,
                                offsetY = adjusted.y,
                                text = "Metin girin..."
                            ))
                        }
                    }
                }

                // 2. SELECTOR / LASSO
                .pointerInput(currentTool, latestObjects, globalOffset, globalScale) {
                    if (currentTool == ToolType.SELECTOR || currentTool == ToolType.LASSO) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val adjustedOffset = (startOffset - globalOffset) / globalScale
                                if (currentTool == ToolType.SELECTOR) {
                                    val hit = latestObjects.findLast { getObjectBounds(it).contains(adjustedOffset) }
                                    onSelectionChanged(hit?.let { setOf(it.id) } ?: emptySet())
                                } else {
                                    onSelectionChanged(emptySet())
                                    lassoPath = Path().apply { moveTo(adjustedOffset.x, adjustedOffset.y) }
                                }
                            },
                            onDrag = { change, _ ->
                                val adjustedPos = (change.position - globalOffset) / globalScale
                                if (currentTool == ToolType.LASSO) {
                                    lassoPath?.lineTo(adjustedPos.x, adjustedPos.y)
                                }
                            },
                            onDragEnd = {
                                if (currentTool == ToolType.LASSO) {
                                    lassoPath?.let { path ->
                                        val lassoBounds = path.getBounds()
                                        val newSelection = latestObjects.filter { obj ->
                                            lassoBounds.overlaps(getObjectBounds(obj))
                                        }.map { it.id }.toSet()
                                        onSelectionChanged(newSelection)
                                    }
                                    lassoPath = null
                                }
                            }
                        )
                    }
                }
                
                // 3. TRANSFORM SELECTED
                .pointerInput(currentTool, selectedObjectIds, globalScale) {
                    if (currentTool == ToolType.SELECTOR && selectedObjectIds.isNotEmpty()) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            selectedObjectIds.forEach { selId ->
                                latestObjects.find { it.id == selId }?.let { obj ->
                                    val updated = when (obj) {
                                        is DrawPath -> obj.copy(
                                            offsetX = obj.offsetX + pan.x / globalScale,
                                            offsetY = obj.offsetY + pan.y / globalScale,
                                            scale = obj.scale * zoom,
                                            rotation = obj.rotation + rotation
                                        )
                                        is DrawShape -> obj.copy(
                                            offsetX = obj.offsetX + pan.x / globalScale,
                                            offsetY = obj.offsetY + pan.y / globalScale,
                                            scale = obj.scale * zoom,
                                            rotation = obj.rotation + rotation
                                        )
                                        is DrawText -> obj.copy(
                                            offsetX = obj.offsetX + pan.x / globalScale,
                                            offsetY = obj.offsetY + pan.y / globalScale,
                                            scale = obj.scale * zoom,
                                            rotation = obj.rotation + rotation
                                        )
                                    }
                                    onObjectUpdated(updated)
                                }
                            }
                        }
                    }
                }
                
                // 4. DRAWING
                .pointerInput(currentTool, currentColor, currentStrokeWidth, globalOffset, globalScale) {
                    if (currentTool in listOf(ToolType.PEN, ToolType.PENCIL, ToolType.MARKER, ToolType.HIGHLIGHTER, ToolType.BRUSH)) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPathPoints = listOf((offset - globalOffset) / globalScale)
                                onSelectionChanged(emptySet())
                            },
                            onDrag = { change, _ ->
                                currentPathPoints = currentPathPoints + ((change.position - globalOffset) / globalScale)
                            },
                            onDragEnd = {
                                if (currentPathPoints.isNotEmpty()) {
                                    val colorHex = String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
                                    val newPath = DrawPath(
                                        id = UUID.randomUUID().toString(),
                                        colorHex = colorHex,
                                        strokeWidth = currentStrokeWidth,
                                        toolType = currentTool,
                                        alpha = if (currentTool == ToolType.PENCIL) 0.6f else if (currentTool == ToolType.HIGHLIGHTER) 0.4f else 1f,
                                        points = currentPathPoints.map { Point(it.x, it.y) }
                                    )
                                    onObjectAdded(newPath)
                                    currentPathPoints = emptyList()
                                }
                            }
                        )
                    }
                }

                // 5. SHAPES
                .pointerInput(currentTool, currentShape, currentColor, globalOffset, globalScale) {
                    if (currentTool == ToolType.SHAPE) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val adjusted = (offset - globalOffset) / globalScale
                                tempShape = DrawShape(
                                    id = UUID.randomUUID().toString(),
                                    colorHex = String.format("#%06X", 0xFFFFFF and currentColor.toArgb()),
                                    strokeWidth = currentStrokeWidth,
                                    points = listOf(Point(adjusted.x, adjusted.y), Point(adjusted.x, adjusted.y)),
                                    shapeType = currentShape,
                                    isFilled = isFillEnabled,
                                    fillColorHex = if (isFillEnabled) String.format("#%06X", 0xFFFFFF and currentFillColor.toArgb()) else null
                                )
                            },
                            onDrag = { change, _ ->
                                val adjusted = (change.position - globalOffset) / globalScale
                                tempShape = tempShape?.let { it.copy(points = listOf(it.points[0], Point(adjusted.x, adjusted.y))) }
                            },
                            onDragEnd = {
                                tempShape?.let { onObjectAdded(it) }
                                tempShape = null
                            }
                        )
                    }
                }
                // 6. ERASERS
                .pointerInput(currentTool, latestObjects, globalOffset, globalScale) {
                    if (currentTool == ToolType.OBJECT_ERASER) {
                        detectDragGestures { change, _ ->
                            val adjustedPos = (change.position - globalOffset) / globalScale
                            latestObjects.findLast { getObjectBounds(it).contains(adjustedPos) }?.let {
                                onObjectDeleted(it)
                            }
                        }
                    }
                }
        ) {
            // RENDERING WITH GLOBAL TRANSFORM
            withTransform({
                translate(globalOffset.x, globalOffset.y)
                scale(globalScale, globalScale, pivot = Offset.Zero)
            }) {
                objects.forEach { obj ->
                    val color = try { Color(android.graphics.Color.parseColor(obj.colorHex)).copy(alpha = obj.alpha) } catch(e: Exception) { Color.Black.copy(alpha = obj.alpha) }
                    val blendMode = if (obj.toolType == ToolType.MARKER) BlendMode.Multiply else BlendMode.SrcOver
                    
                    withTransform({
                        translate(obj.offsetX, obj.offsetY)
                        rotate(obj.rotation, pivot = getObjectBounds(obj).center)
                        scale(obj.scale, obj.scale, pivot = getObjectBounds(obj).center)
                    }) {
                        when (obj) {
                            is DrawPath -> {
                                drawPath(
                                    path = createSmoothPath(obj.points),
                                    color = color,
                                    style = Stroke(width = obj.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                                    blendMode = blendMode
                                )
                            }
                            is DrawShape -> {
                                val fillCol = try { obj.fillColorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Transparent } catch(e: Exception) { Color.Transparent }
                                drawAdvancedShape(obj, color, fillCol, objects)
                            }
                            is DrawText -> {
                                val style = androidx.compose.ui.text.TextStyle(
                                    color = color,
                                    fontSize = obj.strokeWidth.sp,
                                    fontFamily = when(obj.fontFamily) {
                                        "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                        "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                        else -> androidx.compose.ui.text.font.FontFamily.Default
                                    }
                                )
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = obj.text,
                                    style = style
                                )
                            }
                        }
                    }

                    // Selection Box
                    if (selectedObjectIds.contains(obj.id)) {
                        val bounds = getObjectBounds(obj)
                        drawRect(Color.Blue.copy(alpha = 0.1f), bounds.topLeft, bounds.size)
                        drawRect(Color.Blue, bounds.topLeft, bounds.size, style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
                    }
                }

                // Previews
                if (currentPathPoints.isNotEmpty()) {
                    val path = createSmoothPath(currentPathPoints.map { Point(it.x, it.y) })
                    drawPath(path, currentColor.copy(alpha = 0.8f), style = Stroke(currentStrokeWidth, cap = StrokeCap.Round))
                }
                
                tempShape?.let { shape ->
                    val path = calculateAdvancedShapePath(shape.points[0].x, shape.points[0].y, shape.points[1].x, shape.points[1].y, shape.shapeType)
                    drawPath(path, currentColor, style = Stroke(shape.strokeWidth))
                }

                lassoPath?.let { drawPath(it, Color.Gray, style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)))) }
            }
        }
    }
}
