package com.eldora25.tayfnotes.ui.components.canvas

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.shared.model.drawing.*
import java.io.File
import java.util.UUID

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
    template: CanvasTemplate = CanvasTemplate.BLANK,
    pdfPages: List<String> = emptyList(), 
    onSelectionChanged: (Set<String>) -> Unit,
    onObjectAdded: (DrawObject) -> Unit,
    onObjectUpdated: (DrawObject) -> Unit,
    onObjectDeleted: (DrawObject) -> Unit
) {
    var currentPathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var tempShapeEnd by remember { mutableStateOf<Offset?>(null) }
    var tempShapeStart by remember { mutableStateOf<Offset?>(null) }
    var lassoPath by remember { mutableStateOf<Path?>(null) }
    
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    var globalScale by remember { mutableStateOf(1f) }

    val latestObjects by rememberUpdatedState(objects)
    val textMeasurer = rememberTextMeasurer()

    val pageBitmaps = remember(pdfPages) {
        pdfPages.mapNotNull { path ->
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                
                // Consolidated Gesture Handling
                .pointerInput(currentTool, currentColor, currentStrokeWidth, globalOffset, globalScale, latestObjects, selectedObjectIds, currentShape, isFillEnabled, currentFillColor) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointers = event.changes
                            
                            if (pointers.size >= 2) {
                                // 2+ Fingers: Pan & Zoom
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                globalScale *= zoom
                                globalOffset += pan
                                pointers.forEach { it.consume() }
                            } else if (pointers.size == 1) {
                                val change = pointers.first()
                                val adjustedPos = (change.position - globalOffset) / globalScale
                                
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        if (currentTool in listOf(ToolType.PEN, ToolType.PENCIL, ToolType.MARKER, ToolType.BRUSH, ToolType.HIGHLIGHTER, ToolType.PIXEL_ERASER)) {
                                            currentPathPoints = listOf(adjustedPos)
                                            onSelectionChanged(emptySet())
                                        } else if (currentTool == ToolType.SHAPE) {
                                            tempShapeStart = adjustedPos
                                            tempShapeEnd = adjustedPos
                                        } else if (currentTool == ToolType.SELECTOR) {
                                            val hit = latestObjects.findLast { getObjectBounds(it).contains(adjustedPos) }
                                            onSelectionChanged(hit?.let { setOf(it.id) } ?: emptySet())
                                        } else if (currentTool == ToolType.LASSO) {
                                            lassoPath = Path().apply { moveTo(adjustedPos.x, adjustedPos.y) }
                                        }
                                        change.consume()
                                    }
                                    PointerEventType.Move -> {
                                        if (change.pressed) {
                                            if (currentPathPoints.isNotEmpty()) {
                                                currentPathPoints = currentPathPoints + adjustedPos
                                            } else if (tempShapeStart != null) {
                                                tempShapeEnd = adjustedPos
                                            } else if (lassoPath != null) {
                                                lassoPath?.lineTo(adjustedPos.x, adjustedPos.y)
                                            } else if (currentTool == ToolType.SELECTOR && selectedObjectIds.isNotEmpty()) {
                                                val dragAmount = change.position - change.previousPosition
                                                selectedObjectIds.forEach { selId ->
                                                    latestObjects.find { it.id == selId }?.let { obj ->
                                                        val updated = when (obj) {
                                                            is DrawPath -> obj.copy(offsetX = obj.offsetX + dragAmount.x / globalScale, offsetY = obj.offsetY + dragAmount.y / globalScale)
                                                            is DrawShape -> obj.copy(offsetX = obj.offsetX + dragAmount.x / globalScale, offsetY = obj.offsetY + dragAmount.y / globalScale)
                                                            is DrawText -> obj.copy(offsetX = obj.offsetX + dragAmount.x / globalScale, offsetY = obj.offsetY + dragAmount.y / globalScale)
                                                            is DrawImage -> obj.copy(offsetX = obj.offsetX + dragAmount.x / globalScale, offsetY = obj.offsetY + dragAmount.y / globalScale)
                                                        }
                                                        onObjectUpdated(updated)
                                                    }
                                                }
                                            } else if (currentTool == ToolType.PAN) {
                                                globalOffset += (change.position - change.previousPosition)
                                            } else if (currentTool == ToolType.OBJECT_ERASER || currentTool == ToolType.STROKE_ERASER) {
                                                latestObjects.findLast { getObjectBounds(it).contains(adjustedPos) }?.let { onObjectDeleted(it) }
                                            }
                                            change.consume()
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        if (currentPathPoints.isNotEmpty()) {
                                            val colorHex = String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
                                            val newPath = DrawPath(
                                                id = UUID.randomUUID().toString(),
                                                colorHex = colorHex,
                                                strokeWidth = currentStrokeWidth,
                                                toolType = currentTool,
                                                alpha = when(currentTool) {
                                                    ToolType.PENCIL -> 0.35f
                                                    ToolType.MARKER -> 0.5f
                                                    ToolType.HIGHLIGHTER -> 0.3f
                                                    else -> 1f
                                                },
                                                points = currentPathPoints.map { Point(it.x, it.y) }
                                            )
                                            onObjectAdded(newPath)
                                            currentPathPoints = emptyList()
                                        } else if (tempShapeStart != null && tempShapeEnd != null) {
                                            val colorHex = String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
                                            val fillHex = if (isFillEnabled) String.format("#%06X", 0xFFFFFF and currentFillColor.toArgb()) else null
                                            val newShape = DrawShape(
                                                id = UUID.randomUUID().toString(),
                                                colorHex = colorHex,
                                                strokeWidth = currentStrokeWidth,
                                                points = listOf(Point(tempShapeStart!!.x, tempShapeStart!!.y), Point(tempShapeEnd!!.x, tempShapeEnd!!.y)),
                                                shapeType = currentShape,
                                                isFilled = isFillEnabled,
                                                fillColorHex = fillHex
                                            )
                                            onObjectAdded(newShape)
                                            tempShapeStart = null
                                            tempShapeEnd = null
                                        } else if (lassoPath != null) {
                                            val lassoBounds = lassoPath!!.getBounds()
                                            val newSelection = latestObjects.filter { obj ->
                                                lassoBounds.overlaps(getObjectBounds(obj))
                                            }.map { it.id }.toSet()
                                            onSelectionChanged(newSelection)
                                            lassoPath = null
                                        }
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        globalOffset = Offset.Zero
                        globalScale = 1f
                    })
                }
        ) {
            withTransform({
                translate(globalOffset.x, globalOffset.y)
                scale(globalScale, globalScale, pivot = Offset.Zero)
            }) {
                drawCanvasTemplate(template)

                pageBitmaps.forEachIndexed { index, bitmap ->
                    drawImage(bitmap, topLeft = Offset(0f, index * bitmap.height.toFloat()))
                }

                objects.forEach { obj ->
                    val color = try { Color(android.graphics.Color.parseColor(obj.colorHex)).copy(alpha = obj.alpha) } catch(e: Exception) { Color.Black.copy(alpha = obj.alpha) }
                    val blendMode = if (obj.toolType == ToolType.HIGHLIGHTER) BlendMode.SrcOver 
                                    else if (obj.toolType == ToolType.PIXEL_ERASER) BlendMode.Clear 
                                    else if (obj.toolType == ToolType.PENCIL) BlendMode.Multiply
                                    else BlendMode.SrcOver
                    
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
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = obj.text,
                                    style = androidx.compose.ui.text.TextStyle(color = color, fontSize = obj.strokeWidth.sp)
                                )
                            }
                            is DrawImage -> {
                                val bitmap = BitmapFactory.decodeFile(obj.imageUri)?.asImageBitmap()
                                if (bitmap != null) {
                                    drawImage(bitmap, dstSize = androidx.compose.ui.unit.IntSize(obj.width.toInt(), obj.height.toInt()))
                                } else {
                                    drawRect(color.copy(alpha = 0.2f), size = Size(obj.width, obj.height))
                                }
                            }
                        }
                    }

                    if (selectedObjectIds.contains(obj.id)) {
                        val bounds = getObjectBounds(obj)
                        drawRect(Color.Blue.copy(alpha = 0.1f), bounds.topLeft, bounds.size)
                        drawRect(Color.Blue, bounds.topLeft, bounds.size, style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
                    }
                }

                // Previews
                if (currentPathPoints.isNotEmpty()) {
                    val path = createSmoothPath(currentPathPoints.map { Point(it.x, it.y) })
                    val previewAlpha = when(currentTool) {
                        ToolType.PENCIL -> 0.35f
                        ToolType.MARKER -> 0.5f
                        ToolType.HIGHLIGHTER -> 0.3f
                        else -> 0.8f
                    }
                    drawPath(path, currentColor.copy(alpha = previewAlpha), style = Stroke(currentStrokeWidth, cap = StrokeCap.Round))
                }
                
                if (tempShapeStart != null && tempShapeEnd != null) {
                    val path = calculateAdvancedShapePath(tempShapeStart!!.x, tempShapeStart!!.y, tempShapeEnd!!.x, tempShapeEnd!!.y, currentShape)
                    if (isFillEnabled) drawPath(path, currentFillColor.copy(alpha = 0.5f))
                    drawPath(path, currentColor, style = Stroke(currentStrokeWidth))
                }

                lassoPath?.let { drawPath(it, Color.Gray, style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)))) }
            }
        }
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCanvasTemplate(template: CanvasTemplate) {
    val width = size.width
    val height = size.height
    val color = Color.LightGray.copy(alpha = 0.5f)

    when (template) {
        CanvasTemplate.RULED -> {
            val step = 40.dp.toPx()
            for (y in step.toInt() until height.toInt() step step.toInt()) {
                drawLine(color, Offset(0f, y.toFloat()), Offset(width, y.toFloat()), strokeWidth = 1f)
            }
        }
        CanvasTemplate.GRID -> {
            val step = 40.dp.toPx()
            for (x in step.toInt() until width.toInt() step step.toInt()) {
                drawLine(color, Offset(x.toFloat(), 0f), Offset(x.toFloat(), height), strokeWidth = 1f)
            }
            for (y in step.toInt() until height.toInt() step step.toInt()) {
                drawLine(color, Offset(0f, y.toFloat()), Offset(width, y.toFloat()), strokeWidth = 1f)
            }
        }
        else -> {}
    }
}
