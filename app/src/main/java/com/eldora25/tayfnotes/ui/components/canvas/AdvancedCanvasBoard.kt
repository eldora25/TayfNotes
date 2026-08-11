package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toArgb
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
    var selectedObjectId by remember { mutableStateOf<String?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(currentTool, objects) {
                if (currentTool == ToolType.SELECTOR) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val clickedObj = objects.reversed().find { obj ->
                                getObjectBounds(obj).contains(tapOffset)
                            }
                            selectedObjectId = clickedObj?.id
                        }
                    )
                }
            }
            .pointerInput(currentTool, selectedObjectId) {
                if (currentTool == ToolType.SELECTOR && selectedObjectId != null) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val selectedObj = objects.find { it.id == selectedObjectId }
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
            .pointerInput(currentTool, currentColor, currentStrokeWidth, currentShape, isFillEnabled, currentFillColor) {
                if (currentTool == ToolType.PEN || currentTool == ToolType.HIGHLIGHTER || currentTool == ToolType.PENCIL || currentTool == ToolType.MARKER) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedObjectId = null
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
                } else if (currentTool == ToolType.SHAPE) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedObjectId = null
                            currentPathPoints = listOf(offset, offset)
                        },
                        onDrag = { change, _ ->
                            if (currentPathPoints.size >= 2) {
                                currentPathPoints = listOf(currentPathPoints[0], change.position)
                            }
                        },
                        onDragEnd = {
                            if (currentPathPoints.size >= 2) {
                                val hexColor = String.format("#%06X", (0xFFFFFF and currentColor.toArgb()))
                                val newShape = DrawShape(
                                    id = java.util.UUID.randomUUID().toString(),
                                    colorHex = hexColor,
                                    strokeWidth = currentStrokeWidth,
                                    alpha = 1f,
                                    toolType = ToolType.SHAPE,
                                    points = currentPathPoints.map { Point(it.x, it.y) },
                                    shapeType = currentShape,
                                    isFilled = isFillEnabled,
                                    fillColorHex = if (isFillEnabled) String.format("#%06X", (0xFFFFFF and currentFillColor.toArgb())) else null
                                )
                                onObjectAdded(newShape)
                                currentPathPoints = emptyList()
                            }
                        },
                        onDragCancel = { currentPathPoints = emptyList() }
                    )
                } else if (currentTool == ToolType.OBJECT_ERASER) {
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
                } else if (currentTool == ToolType.PAINT_BUCKET) {
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

            if (drawObj.id == selectedObjectId) {
                val box = getObjectBounds(drawObj)
                val padding = 16f 
                val selectionRect = Rect(
                    left = box.left - padding,
                    top = box.top - padding,
                    right = box.right + padding,
                    bottom = box.bottom + padding
                )

                drawRect(
                    color = Color(0xFF2196F3),
                    topLeft = selectionRect.topLeft,
                    size = selectionRect.size,
                    style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
                )
                
                val handleRadius = 12f
                val handleColor = Color.White
                val handleStroke = Color(0xFF2196F3)
                
                listOf(selectionRect.topLeft, selectionRect.topRight, selectionRect.bottomLeft, selectionRect.bottomRight).forEach { corner ->
                    drawCircle(color = handleColor, radius = handleRadius, center = corner)
                    drawCircle(color = handleStroke, radius = handleRadius, center = corner, style = Stroke(width = 4f))
                }
            }
        }

        if (currentPathPoints.isNotEmpty()) {
            val activeColor = currentColor.copy(alpha = if (currentTool == ToolType.HIGHLIGHTER || currentTool == ToolType.MARKER) 0.45f else if (currentTool == ToolType.PENCIL) 0.6f else 1f)
            val blendMode = if (currentTool == ToolType.HIGHLIGHTER || currentTool == ToolType.MARKER) BlendMode.Multiply else BlendMode.SrcOver

            if (currentTool == ToolType.SHAPE) {
                val shapePath = calculateAdvancedShapePath(
                    startX = currentPathPoints[0].x,
                    startY = currentPathPoints[0].y,
                    endX = currentPathPoints[1].x,
                    endY = currentPathPoints[1].y,
                    shapeType = currentShape
                )
                drawPath(path = shapePath, color = activeColor, style = Stroke(width = currentStrokeWidth))
            } else {
                drawPath(
                    path = createSmoothPath(currentPathPoints.map { Point(it.x, it.y) }),
                    color = activeColor,
                    style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = blendMode
                )
            }
        }
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
    
    val width = (maxX - minX) * obj.scale
    val height = (maxY - minY) * obj.scale
    val centerX = (minX + maxX) / 2 + obj.offsetX
    val centerY = (minY + maxY) / 2 + obj.offsetY
    
    return Rect(centerX - width/2, centerY - height/2, centerX + width/2, centerY + height/2)
}

private fun createSmoothPath(points: List<Point>): Path {
    val path = Path()
    if (points.isEmpty()) return path

    path.moveTo(points.first().x, points.first().y)
    var currentX = points.first().x
    var currentY = points.first().y

    for (i in 1 until points.size) {
        val nextPoint = points[i]
        val midPointX = (currentX + nextPoint.x) / 2
        val midPointY = (currentY + nextPoint.y) / 2
        
        if (i == 1) {
            path.lineTo(midPointX, midPointY)
        } else {
            path.quadraticTo(currentX, currentY, midPointX, midPointY)
        }
        currentX = nextPoint.x
        currentY = nextPoint.y
    }
    path.lineTo(currentX, currentY)
    return path
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAdvancedShape(
    obj: DrawShape,
    color: Color,
    fillColor: Color,
    allObjects: List<DrawObject>
) {
    val pathData = obj.pathData
    when (obj.shapeType) {
        ShapeType.INTERSECTION -> {
            if (pathData != null) {
                val ids = pathData.split("|")
                if (ids.size >= 2) {
                    val src1 = allObjects.find { it.id == ids[0] }
                    val src2 = allObjects.find { it.id == ids[1] }
                    if (src1 != null && src2 != null) {
                        val p1 = getObjectPath(src1)
                        val p2 = getObjectPath(src2)
                        val intersection = calculateIntersectionPath(p1, p2)
                        if (obj.isFilled) drawPath(intersection, fillColor)
                        drawPath(intersection, color, style = Stroke(width = obj.strokeWidth))
                    }
                }
            }
        }
        else -> {
            val shapePath = calculateAdvancedShapePath(
                startX = if (obj.points.size >= 2) obj.points[0].x else 0f,
                startY = if (obj.points.size >= 2) obj.points[0].y else 0f,
                endX = if (obj.points.size >= 2) obj.points[1].x else 0f,
                endY = if (obj.points.size >= 2) obj.points[1].y else 0f,
                shapeType = obj.shapeType
            )
            if (obj.isFilled && obj.fillColorHex != null) {
                drawPath(shapePath, fillColor)
            }
            drawPath(shapePath, color, style = Stroke(width = obj.strokeWidth))
        }
    }
}

private fun getObjectPath(obj: DrawObject): Path {
    val path = Path()
    if (obj is DrawShape && obj.points.size >= 2) {
        val start = obj.points[0]
        val end = obj.points[1]
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
            else -> path.addRect(Rect(Offset(left, top), Size(width, height)))
        }
    } else if (obj is DrawPath && obj.points.isNotEmpty()) {
        path.moveTo(obj.points[0].x, obj.points[0].y)
        obj.points.forEach { path.lineTo(it.x, it.y) }
    }
    return path
}
