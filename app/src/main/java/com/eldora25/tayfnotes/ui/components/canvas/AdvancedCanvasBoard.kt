package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import com.eldora25.tayfnotes.shared.model.drawing.*
import java.util.UUID

import androidx.compose.ui.graphics.toArgb
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
    
    val latestObjects by rememberUpdatedState(objects)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                
                // 1. SELECTOR / LASSO
                .pointerInput(currentTool, latestObjects) {
                    if (currentTool == ToolType.SELECTOR || currentTool == ToolType.LASSO) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                if (currentTool == ToolType.SELECTOR) {
                                    val hit = latestObjects.findLast { getObjectBounds(it).contains(startOffset) }
                                    onSelectionChanged(hit?.let { setOf(it.id) } ?: emptySet())
                                } else {
                                    onSelectionChanged(emptySet())
                                    lassoPath = Path().apply { moveTo(startOffset.x, startOffset.y) }
                                }
                            },
                            onDrag = { change, _ ->
                                if (currentTool == ToolType.LASSO) {
                                    lassoPath?.lineTo(change.position.x, change.position.y)
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
                
                // 2. TRANSFORM
                .pointerInput(currentTool, selectedObjectIds) {
                    if (currentTool == ToolType.SELECTOR && selectedObjectIds.isNotEmpty()) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            selectedObjectIds.forEach { selId ->
                                latestObjects.find { it.id == selId }?.let { obj ->
                                    val updated = when (obj) {
                                        is DrawPath -> obj.copy(
                                            offsetX = obj.offsetX + pan.x,
                                            offsetY = obj.offsetY + pan.y,
                                            scale = obj.scale * zoom,
                                            rotation = obj.rotation + rotation
                                        )
                                        is DrawShape -> obj.copy(
                                            offsetX = obj.offsetX + pan.x,
                                            offsetY = obj.offsetY + pan.y,
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
                
                // 3. DRAWING
                .pointerInput(currentTool, currentColor, currentStrokeWidth) {
                    if (currentTool in listOf(ToolType.PEN, ToolType.PENCIL, ToolType.MARKER, ToolType.HIGHLIGHTER, ToolType.BRUSH)) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPathPoints = listOf(offset)
                                onSelectionChanged(emptySet())
                            },
                            onDrag = { change, _ ->
                                currentPathPoints = currentPathPoints + change.position
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

                // 4. SHAPES
                .pointerInput(currentTool, currentShape, currentColor) {
                    if (currentTool == ToolType.SHAPE) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                tempShape = DrawShape(
                                    id = UUID.randomUUID().toString(),
                                    colorHex = String.format("#%06X", 0xFFFFFF and currentColor.toArgb()),
                                    strokeWidth = currentStrokeWidth,
                                    points = listOf(Point(offset.x, offset.y), Point(offset.x, offset.y)),
                                    shapeType = currentShape,
                                    isFilled = isFillEnabled,
                                    fillColorHex = if (isFillEnabled) String.format("#%06X", 0xFFFFFF and currentFillColor.toArgb()) else null
                                )
                            },
                            onDrag = { change, _ ->
                                tempShape = tempShape?.let { it.copy(points = listOf(it.points[0], Point(change.position.x, change.position.y))) }
                            },
                            onDragEnd = {
                                tempShape?.let { onObjectAdded(it) }
                                tempShape = null
                            }
                        )
                    }
                }

                // 5. ERASERS
                .pointerInput(currentTool, latestObjects) {
                    if (currentTool == ToolType.OBJECT_ERASER) {
                        detectDragGestures { change, _ ->
                            latestObjects.findLast { getObjectBounds(it).contains(change.position) }?.let {
                                onObjectDeleted(it)
                            }
                        }
                    }
                }
        ) {
            // RENDERING
            objects.forEach { obj ->
                val color = try { Color(android.graphics.Color.parseColor(obj.colorHex)).copy(alpha = obj.alpha) } catch(e: Exception) { Color.Black.copy(alpha = obj.alpha) }
                val blendMode = if (obj.toolType == ToolType.MARKER) BlendMode.Multiply else BlendMode.SrcOver
                
                withTransform({
                    translate(obj.offsetX, obj.offsetY)
                    rotate(obj.rotation, pivot = getObjectBounds(obj).center)
                    scale(obj.scale, obj.scale, pivot = getObjectBounds(obj).center)
                }) {
                    if (obj is DrawPath) {
                        drawPath(
                            path = createSmoothPath(obj.points),
                            color = color,
                            style = Stroke(width = obj.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            blendMode = blendMode
                        )
                    } else if (obj is DrawShape) {
                        val fillCol = try { obj.fillColorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Transparent } catch(e: Exception) { Color.Transparent }
                        drawAdvancedShape(obj, color, fillCol, objects)
                    }
                }

                // Selection Box & Handles
                if (selectedObjectIds.contains(obj.id)) {
                    val bounds = getObjectBounds(obj)
                    drawRect(Color.Blue.copy(alpha = 0.1f), bounds.topLeft, bounds.size)
                    drawRect(Color.Blue, bounds.topLeft, bounds.size, style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
                    
                    val handleSize = 8.dp.toPx()
                    val corners = listOf(bounds.topLeft, bounds.topRight, bounds.bottomLeft, bounds.bottomRight)
                    corners.forEach { 
                        drawCircle(Color.White, handleSize/2, it)
                        drawCircle(Color.Blue, handleSize/2, it, style = Stroke(1.5f)) 
                    }
                }
            }

            // Real-time previews
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
