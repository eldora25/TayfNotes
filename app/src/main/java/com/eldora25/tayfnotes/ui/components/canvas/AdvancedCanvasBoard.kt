package com.eldora25.tayfnotes.ui.components.canvas

import android.graphics.BitmapFactory
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.shared.model.drawing.*
import com.eldora25.tayfnotes.ui.theme.TayfFonts
import java.io.File
import java.util.UUID

@OptIn(ExperimentalComposeUiApi::class)
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
    val latestOnSelectionChanged by rememberUpdatedState(onSelectionChanged)
    val latestOnObjectAdded by rememberUpdatedState(onObjectAdded)
    val latestOnObjectUpdated by rememberUpdatedState(onObjectUpdated)
    val latestOnObjectDeleted by rememberUpdatedState(onObjectDeleted)
    
    val textMeasurer = rememberTextMeasurer()

    // Optimized Caching
    val cachedBounds = remember(objects) { objects.associate { it.id to getObjectBounds(it) } }
    val cachedColors = remember(objects) {
        objects.associate { obj ->
            obj.id to try { Color(android.graphics.Color.parseColor(obj.colorHex)) } catch(e: Exception) { Color.Black }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                
                // 1. Pan & Zoom (Always active, but 1-finger drawing logic might clash)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        globalScale *= zoom
                        globalOffset += pan
                    }
                }
                
                // 2. Drawing / Selection / Eraser (1 finger) with Palm Rejection
                .pointerInput(currentTool, globalOffset, globalScale) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointerChange = event.changes.first()
                            
                            // Palm Rejection: Only allow Stylus for drawing if Stylus is present?
                            // Actually, just checking toolType for drawing logic
                            val isStylus = pointerChange.type == PointerType.Stylus
                            val isMouse = pointerChange.type == PointerType.Mouse
                            val isFinger = pointerChange.type == PointerType.Touch
                            
                            // If user is drawing with finger but we want palm rejection (stylus only),
                            // we would filter here. But common request is: Finger PANS, Stylus DRAWS.
                            
                            val adjustedPos = (pointerChange.position - globalOffset) / globalScale
                            
                            if (pointerChange.pressed) {
                                // Draw/Edit Logic
                                // If current tool is PAN, or if it's a finger and we are in stylus-only mode
                                if (currentTool == ToolType.PAN || (isFinger && currentTool != ToolType.SELECTOR)) {
                                    // Let transform gestures handle it
                                } else {
                                    // Tool Logic
                                    when (currentTool) {
                                        ToolType.PEN, ToolType.PENCIL, ToolType.MARKER, ToolType.BRUSH, ToolType.HIGHLIGHTER -> {
                                            if (pointerChange.changedToDown()) {
                                                currentPathPoints = listOf(adjustedPos)
                                                latestOnSelectionChanged(emptySet())
                                            } else {
                                                currentPathPoints = currentPathPoints + adjustedPos
                                            }
                                        }
                                        ToolType.SHAPE -> {
                                            if (pointerChange.changedToDown()) tempShapeStart = adjustedPos
                                            tempShapeEnd = adjustedPos
                                        }
                                        ToolType.SELECTOR -> {
                                            if (pointerChange.changedToDown()) {
                                                val hit = latestObjects.findLast { cachedBounds[it.id]?.contains(adjustedPos) == true }
                                                latestOnSelectionChanged(hit?.let { setOf(it.id) } ?: emptySet())
                                            } else if (selectedObjectIds.isNotEmpty()) {
                                                // Moving logic
                                                val delta = pointerChange.position - pointerChange.previousPosition
                                                selectedObjectIds.forEach { selId ->
                                                    latestObjects.find { it.id == selId }?.let { obj ->
                                                        val updated = when (obj) {
                                                            is DrawPath -> obj.copy(offsetX = obj.offsetX + delta.x / globalScale, offsetY = obj.offsetY + delta.y / globalScale)
                                                            is DrawShape -> obj.copy(offsetX = obj.offsetX + delta.x / globalScale, offsetY = obj.offsetY + delta.y / globalScale)
                                                            is DrawText -> obj.copy(offsetX = obj.offsetX + delta.x / globalScale, offsetY = obj.offsetY + delta.y / globalScale)
                                                            is DrawImage -> obj.copy(offsetX = obj.offsetX + delta.x / globalScale, offsetY = obj.offsetY + delta.y / globalScale)
                                                        }
                                                        latestOnObjectUpdated(updated)
                                                    }
                                                }
                                            }
                                        }
                                        ToolType.OBJECT_ERASER -> {
                                             latestObjects.findLast { cachedBounds[it.id]?.contains(adjustedPos) == true }?.let { latestOnObjectDeleted(it) }
                                        }
                                        else -> {}
                                    }
                                }
                                pointerChange.consume()
                            } else {
                                // Drag End
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
                                    latestOnObjectAdded(newPath)
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
                                    latestOnObjectAdded(newShape)
                                    tempShapeStart = null
                                    tempShapeEnd = null
                                }
                            }
                        }
                    }
                }
        ) {
            withTransform({
                translate(globalOffset.x, globalOffset.y)
                scale(globalScale, globalScale, pivot = Offset.Zero)
            }) {
                drawCanvasTemplate(template)

                objects.forEach { obj ->
                    val color = (cachedColors[obj.id] ?: Color.Black).copy(alpha = obj.alpha)
                    val blendMode = if (obj.toolType == ToolType.HIGHLIGHTER) BlendMode.SrcOver 
                                    else if (obj.toolType == ToolType.PIXEL_ERASER) BlendMode.Clear 
                                    else if (obj.toolType == ToolType.PENCIL) BlendMode.Multiply
                                    else BlendMode.SrcOver
                    
                    val objBounds = cachedBounds[obj.id] ?: Rect.Zero
                    withTransform({
                        translate(obj.offsetX, obj.offsetY)
                        rotate(obj.rotation, pivot = objBounds.center)
                        scale(obj.scale, obj.scale, pivot = objBounds.center)
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
                                val fillCol = if (obj.isFilled && obj.fillColorHex != null) Color(android.graphics.Color.parseColor(obj.fillColorHex)) else Color.Transparent
                                drawAdvancedShape(obj, color, fillCol, objects)
                            }
                            is DrawText -> {
                                val fontFamily = TayfFonts[obj.fontFamily] ?: FontFamily.Default
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = obj.text,
                                    style = androidx.compose.ui.text.TextStyle(
                                        color = color, 
                                        fontSize = obj.strokeWidth.sp,
                                        fontFamily = fontFamily
                                    )
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
                        val bounds = cachedBounds[obj.id] ?: Rect.Zero
                        drawRect(Color.Blue.copy(alpha = 0.1f), bounds.topLeft, bounds.size)
                        drawRect(Color.Blue, bounds.topLeft, bounds.size, style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
                        
                        // Resize handles
                        drawCircle(Color.White, 8f, bounds.bottomRight, style = Fill)
                        drawCircle(Color.Blue, 8f, bounds.bottomRight, style = Stroke(2f))
                    }
                }

                // Previews
                if (currentPathPoints.isNotEmpty()) {
                    val path = createSmoothPath(currentPathPoints.map { Point(it.x, it.y) })
                    val previewAlpha = if (currentTool == ToolType.HIGHLIGHTER) 0.3f else 0.8f
                    drawPath(path, currentColor.copy(alpha = previewAlpha), style = Stroke(currentStrokeWidth, cap = StrokeCap.Round))
                }
                
                if (tempShapeStart != null && tempShapeEnd != null) {
                    val path = calculateAdvancedShapePath(tempShapeStart!!.x, tempShapeStart!!.y, tempShapeEnd!!.x, tempShapeEnd!!.y, currentShape)
                    if (isFillEnabled) drawPath(path, currentColor.copy(alpha = 0.5f))
                    drawPath(path, currentColor, style = Stroke(currentStrokeWidth))
                }

                lassoPath?.let { drawPath(it, Color.Gray, style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f)))) }
            }
        }
    }
}

fun DrawScope.drawCanvasTemplate(template: CanvasTemplate) {
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
