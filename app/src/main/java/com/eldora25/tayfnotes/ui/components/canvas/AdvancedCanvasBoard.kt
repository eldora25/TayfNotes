package com.eldora25.tayfnotes.ui.components.canvas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.pointer.PointerType
import com.eldora25.tayfnotes.shared.model.drawing.*
import java.util.UUID
import androidx.compose.ui.graphics.toArgb
import java.io.File

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
    pdfPages: List<String> = emptyList(), // Local paths to rendered PDF bitmaps
    onSelectionChanged: (Set<String>) -> Unit,
    onObjectAdded: (DrawObject) -> Unit,
    onObjectUpdated: (DrawObject) -> Unit,
    onObjectDeleted: (DrawObject) -> Unit
) {
    var currentPathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var tempShape by remember { mutableStateOf<DrawShape?>(null) }
    var lassoPath by remember { mutableStateOf<Path?>(null) }
    
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    var globalScale by remember { mutableStateOf(1f) }

    val latestObjects by rememberUpdatedState(objects)
    val textMeasurer = rememberTextMeasurer()

    // Load PDF bitmaps if template is PDF
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
                
                // 1. GLOBAL PAN / ZOOM & DOUBLE TAP
                .pointerInput(currentTool) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (currentTool == ToolType.PAN) {
                            globalOffset += pan
                            globalScale *= zoom
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        globalOffset = Offset.Zero
                        globalScale = 1f
                    })
                }

                // 2. DRAWING & SMART STYLUS
                .pointerInput(currentTool, currentColor, currentStrokeWidth, globalOffset, globalScale) {
                    if (currentTool in listOf(ToolType.PEN, ToolType.PENCIL, ToolType.MARKER, ToolType.BRUSH, ToolType.HIGHLIGHTER, ToolType.PIXEL_ERASER)) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val adjusted = (offset - globalOffset) / globalScale
                                currentPathPoints = listOf(adjusted)
                                onSelectionChanged(emptySet())
                            },
                            onDrag = { change, _ ->
                                val isStylus = change.type == PointerType.Stylus
                                // Madde 1: Stylus only drawing if preferred (optional feature)
                                if (isStylus || currentTool != ToolType.PAN) {
                                    currentPathPoints = currentPathPoints + ((change.position - globalOffset) / globalScale)
                                }
                            },
                            onDragEnd = {
                                if (currentPathPoints.isNotEmpty()) {
                                    val colorHex = String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
                                    val newPath = DrawPath(
                                        id = UUID.randomUUID().toString(),
                                        colorHex = colorHex,
                                        strokeWidth = currentStrokeWidth,
                                        toolType = currentTool,
                                        alpha = if (currentTool == ToolType.HIGHLIGHTER) 0.3f else 1f,
                                        points = currentPathPoints.map { Point(it.x, it.y) }
                                    )
                                    onObjectAdded(newPath)
                                    currentPathPoints = emptyList()
                                }
                            }
                        )
                    }
                }

                // 3. SELECTION & MANIPULATION
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
                                        is DrawImage -> obj.copy(
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

                // 4. ERASERS (Pixel / Stroke)
                .pointerInput(currentTool, latestObjects, globalOffset, globalScale) {
                    if (currentTool == ToolType.OBJECT_ERASER || currentTool == ToolType.STROKE_ERASER) {
                        detectDragGestures { change, _ ->
                            val adjustedPos = (change.position - globalOffset) / globalScale
                            latestObjects.findLast { getObjectBounds(it).contains(adjustedPos) }?.let {
                                onObjectDeleted(it)
                            }
                        }
                    }
                }
        ) {
            // RENDERING
            withTransform({
                translate(globalOffset.x, globalOffset.y)
                scale(globalScale, globalScale, pivot = Offset.Zero)
            }) {
                // Draw Templates (Ruled, Grid)
                drawCanvasTemplate(template)

                // Draw PDF Background
                pageBitmaps.forEachIndexed { index, bitmap ->
                    drawImage(bitmap, topLeft = Offset(0f, index * bitmap.height.toFloat()))
                }

                objects.forEach { obj ->
                    val color = try { Color(android.graphics.Color.parseColor(obj.colorHex)).copy(alpha = obj.alpha) } catch(e: Exception) { Color.Black.copy(alpha = obj.alpha) }
                    val blendMode = if (obj.toolType == ToolType.HIGHLIGHTER) BlendMode.SrcOver 
                                    else if (obj.toolType == ToolType.PIXEL_ERASER) BlendMode.Clear 
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
                    drawPath(path, currentColor.copy(alpha = if (currentTool == ToolType.HIGHLIGHTER) 0.3f else 0.8f), style = Stroke(currentStrokeWidth, cap = StrokeCap.Round))
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

fun Size.toSize() = androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt())
