package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.drawing.*

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
    onObjectAdded: (DrawObject) -> Unit,
    onObjectUpdated: (DrawObject) -> Unit,
    onObjectDeleted: (DrawObject) -> Unit
) {
    var currentPathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var tempShape by remember { mutableStateOf<DrawShape?>(null) }
    var selectedObjectIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    val latestObjects by rememberUpdatedState(objects)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                
                // 1. SELECTOR TIKLAMA: Çoklu Obje Seçimi
                .pointerInput(currentTool, latestObjects) {
                    if (currentTool == ToolType.SELECTOR) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val clickedObj = latestObjects.reversed().find { obj ->
                                    getObjectBounds(obj).contains(tapOffset)
                                }
                                
                                if (clickedObj != null) {
                                    selectedObjectIds = if (selectedObjectIds.contains(clickedObj.id)) {
                                        selectedObjectIds - clickedObj.id
                                    } else {
                                        selectedObjectIds + clickedObj.id
                                    }
                                } else {
                                    selectedObjectIds = emptySet()
                                }
                            }
                        )
                    }
                }
                
                // 2. SELECTOR SÜRÜKLEME: Taşıma ve Boyutlandırma
                .pointerInput(currentTool, selectedObjectIds) {
                    if (currentTool == ToolType.SELECTOR && selectedObjectIds.isNotEmpty()) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            selectedObjectIds.forEach { selId ->
                                val selectedObj = latestObjects.find { it.id == selId }
                                if (selectedObj != null) {
                                    val updatedObj = when (selectedObj) {
                                        is DrawPath -> selectedObj.copy(
                                            offsetX = selectedObj.offsetX + pan.x,
                                            offsetY = selectedObj.offsetY + pan.y,
                                            scale = selectedObj.scale * zoom
                                        )
                                        is DrawShape -> selectedObj.copy(
                                            offsetX = selectedObj.offsetX + pan.x,
                                            offsetY = selectedObj.offsetY + pan.y,
                                            scale = selectedObj.scale * zoom
                                        )
                                    }
                                    onObjectUpdated(updatedObj)
                                }
                            }
                        }
                    }
                }
                
                // 3. OBJE SİLGİSİ
                .pointerInput(currentTool, objects) {
                    if (currentTool == ToolType.OBJECT_ERASER) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val hit = objects.findLast { obj -> getObjectBounds(obj).contains(offset) }
                                if (hit != null) onObjectDeleted(hit)
                            },
                            onDrag = { change, _ ->
                                val hit = objects.findLast { obj -> getObjectBounds(obj).contains(change.position) }
                                if (hit != null) onObjectDeleted(hit)
                            }
                        )
                    }
                }
                
                // 4. ŞEKİL ÇİZİMİ
                .pointerInput(currentTool, currentShape, currentColor, currentStrokeWidth, isFillEnabled, currentFillColor) {
                    if (currentTool == ToolType.SHAPE) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selectedObjectIds = emptySet()
                                val hexColor = String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
                                tempShape = DrawShape(
                                    id = java.util.UUID.randomUUID().toString(),
                                    colorHex = hexColor,
                                    strokeWidth = currentStrokeWidth,
                                    toolType = ToolType.SHAPE,
                                    points = listOf(Point(offset.x, offset.y), Point(offset.x, offset.y)),
                                    shapeType = currentShape,
                                    isFilled = isFillEnabled,
                                    fillColorHex = if (isFillEnabled) String.format("#%06X", (0xFFFFFF and currentFillColor.toArgb())) else null
                                )
                            },
                            onDrag = { change, _ ->
                                tempShape = tempShape?.let { shape ->
                                    shape.copy(points = listOf(shape.points[0], Point(change.position.x, change.position.y)))
                                }
                            },
                            onDragEnd = {
                                tempShape?.let { onObjectAdded(it) }
                                tempShape = null
                            },
                            onDragCancel = { tempShape = null }
                        )
                    }
                }
                
                // 5. SERBEST ÇİZİM
                .pointerInput(currentTool, currentColor, currentStrokeWidth) {
                    if (currentTool == ToolType.PEN || currentTool == ToolType.HIGHLIGHTER || currentTool == ToolType.PENCIL || currentTool == ToolType.MARKER) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selectedObjectIds = emptySet()
                                currentPathPoints = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                currentPathPoints = currentPathPoints + change.position
                            },
                            onDragEnd = {
                                if (currentPathPoints.isNotEmpty()) {
                                    val hexColor = String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
                                    val newPath = DrawPath(
                                        id = java.util.UUID.randomUUID().toString(),
                                        colorHex = hexColor,
                                        strokeWidth = currentStrokeWidth,
                                        alpha = if (currentTool == ToolType.HIGHLIGHTER) 0.45f else if (currentTool == ToolType.PENCIL) 0.6f else if (currentTool == ToolType.MARKER) 0.45f else 1f,
                                        toolType = currentTool,
                                        points = currentPathPoints.map { Point(it.x, it.y) }
                                    )
                                    onObjectAdded(newPath)
                                    currentPathPoints = emptyList()
                                }
                            },
                            onDragCancel = { currentPathPoints = emptyList() }
                        )
                    }
                }
                
                // 6. AKILLI KOVA
                .pointerInput(currentTool, objects, currentColor) {
                    if (currentTool == ToolType.PAINT_BUCKET) {
                        detectTapGestures { offset ->
                            val hits = objects.filter { obj -> getObjectBounds(obj).contains(offset) }
                            if (hits.size >= 2) {
                                val path1 = getObjectPath(hits[hits.size - 1])
                                val path2 = getObjectPath(hits[hits.size - 2])
                                val intersection = calculateIntersectionPath(path1, path2)
                                if (!intersection.isEmpty) {
                                    val newObj = DrawShape(
                                        id = java.util.UUID.randomUUID().toString(),
                                        colorHex = String.format("#%06X", (0xFFFFFF and currentColor.toArgb())),
                                        strokeWidth = 2f,
                                        toolType = ToolType.SHAPE,
                                        shapeType = ShapeType.INTERSECTION,
                                        isFilled = true,
                                        fillColorHex = String.format("#%06X", (0xFFFFFF and currentColor.toArgb())),
                                        points = emptyList(),
                                        pathData = hits[hits.size - 1].id + "|" + hits[hits.size - 2].id
                                    )
                                    onObjectAdded(newObj)
                                }
                            } else if (hits.size == 1) {
                                val hit = hits[0]
                                if (hit is DrawShape) {
                                    onObjectUpdated(hit.copy(isFilled = true, fillColorHex = String.format("#%06X", (0xFFFFFF and currentColor.toArgb()))))
                                }
                            }
                        }
                    }
                }
        ) {
            // --- RENDER DÖNGÜSÜ ---
            objects.forEach { drawObj ->
                val objColor = try {
                    Color(android.graphics.Color.parseColor(drawObj.colorHex)).copy(alpha = drawObj.alpha)
                } catch (e: Exception) {
                    Color.Black.copy(alpha = drawObj.alpha)
                }
                val fillColor = if (drawObj is DrawShape && drawObj.isFilled && drawObj.fillColorHex != null) {
                    try { Color(android.graphics.Color.parseColor(drawObj.fillColorHex)).copy(alpha = drawObj.alpha) } catch(e: Exception) { Color.Transparent }
                } else Color.Transparent

                val blendMode = if (drawObj.toolType == ToolType.HIGHLIGHTER || drawObj.toolType == ToolType.MARKER) BlendMode.Multiply else BlendMode.SrcOver
                
                withTransform({
                    translate(left = drawObj.offsetX, top = drawObj.offsetY)
                    scale(drawObj.scale, drawObj.scale, pivot = Offset.Zero)
                }) {
                    if (drawObj is DrawPath) {
                        drawPath(
                            path = createSmoothPath(drawObj.points),
                            color = objColor,
                            style = Stroke(width = drawObj.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            blendMode = blendMode
                        )
                    } else if (drawObj is DrawShape) {
                        drawAdvancedShape(drawObj, objColor, fillColor, objects)
                    }
                }

                // Seçim Çerçevesi Çizimi
                if (selectedObjectIds.contains(drawObj.id)) {
                    val box = getObjectBounds(drawObj)
                    val padding = 16f 
                    val selectionRect = Rect(box.left - padding, box.top - padding, box.right + padding, box.bottom + padding)
                    val boxColor = if (selectedObjectIds.size == 2) Color(0xFF9C27B0) else Color(0xFF2196F3)

                    drawRect(
                        color = boxColor,
                        topLeft = selectionRect.topLeft,
                        size = selectionRect.size,
                        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
                    )
                    
                    val handleRadius = 12f
                    val handleColor = Color.White
                    
                    listOf(selectionRect.topLeft, selectionRect.topRight, selectionRect.bottomLeft, selectionRect.bottomRight).forEach { corner ->
                        drawCircle(color = handleColor, radius = handleRadius, center = corner)
                        drawCircle(color = boxColor, radius = handleRadius, center = corner, style = Stroke(width = 4f))
                    }
                }
            }

            // Preview Layers
            if (currentPathPoints.isNotEmpty()) {
                val activeColor = currentColor.copy(alpha = if (currentTool == ToolType.HIGHLIGHTER || currentTool == ToolType.MARKER) 0.45f else if (currentTool == ToolType.PENCIL) 0.6f else 1f)
                val blendMode = if (currentTool == ToolType.HIGHLIGHTER || currentTool == ToolType.MARKER) BlendMode.Multiply else BlendMode.SrcOver

                drawPath(
                    path = createSmoothPath(currentPathPoints.map { Point(it.x, it.y) }),
                    color = activeColor,
                    style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = blendMode
                )
            }
            
            tempShape?.let { shape ->
                val objColor = try { Color(android.graphics.Color.parseColor(shape.colorHex)).copy(alpha = shape.alpha) } catch (e: Exception) { Color.Black.copy(alpha = shape.alpha) }
                val fillCol = if (shape.isFilled && shape.fillColorHex != null) { try { Color(android.graphics.Color.parseColor(shape.fillColorHex)).copy(alpha = shape.alpha) } catch(e: Exception) { Color.Transparent } } else Color.Transparent

                val shapePath = calculateAdvancedShapePath(startX = shape.points[0].x, startY = shape.points[0].y, endX = shape.points[1].x, endY = shape.points[1].y, shapeType = shape.shapeType)
                if (shape.isFilled) drawPath(shapePath, fillCol)
                drawPath(path = shapePath, color = objColor, style = Stroke(width = shape.strokeWidth))
            }
        }

        // AKILLI KESİŞİM MENÜSÜ
        AnimatedVisibility(
            visible = selectedObjectIds.size == 2 && currentTool == ToolType.SELECTOR,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
        ) {
            BooleanOperationsMenu(
                onOperationClick = { operation ->
                    val ids = selectedObjectIds.toList()
                    val obj1 = objects.find { it.id == ids[0] }
                    val obj2 = objects.find { it.id == ids[1] }
                    
                    if (obj1 != null && obj2 != null) {
                        val composeOp = when(operation) {
                            BooleanOperation.INTERSECT -> PathOperation.Intersect
                            BooleanOperation.DIFFERENCE -> PathOperation.Difference
                            BooleanOperation.UNION -> PathOperation.Union
                        }
                        
                        val p1 = extractPathFromDrawObject(obj1)
                        val p2 = extractPathFromDrawObject(obj2)
                        val intersection = Path().apply { op(p1, p2, composeOp) }
                        
                        if (!intersection.isEmpty) {
                            val newObj = DrawShape(
                                id = java.util.UUID.randomUUID().toString(),
                                colorHex = String.format("#%06X", (0xFFFFFF and currentColor.toArgb())),
                                strokeWidth = 2f,
                                toolType = ToolType.SHAPE,
                                shapeType = ShapeType.INTERSECTION,
                                isFilled = true,
                                fillColorHex = String.format("#%06X", (0xFFFFFF and currentColor.toArgb())),
                                points = emptyList(),
                                pathData = obj1.id + "|" + obj2.id
                            )
                            onObjectAdded(newObj)
                            selectedObjectIds = emptySet()
                        }
                    }
                }
            )
        }
    }
}
