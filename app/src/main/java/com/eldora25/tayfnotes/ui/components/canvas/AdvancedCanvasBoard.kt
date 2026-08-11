package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.toArgb

@Composable
fun AdvancedCanvasBoard(
    modifier: Modifier = Modifier,
    currentColor: Color = Color.Black,
    currentStrokeWidth: Float = 5f,
    currentTool: AdvancedToolType = AdvancedToolType.PEN,
    objects: List<DrawObject>,
    onObjectAdded: (DrawObject) -> Unit
) {
    // Kullanıcının o an çizmekte olduğu aktif yol
    var currentPathPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(currentTool, currentColor, currentStrokeWidth) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Sadece çizim araçları seçiliyken yeni path başlat
                        if (currentTool == AdvancedToolType.PEN || currentTool == AdvancedToolType.HIGHLIGHTER || currentTool == AdvancedToolType.PENCIL) {
                            currentPathPoints = listOf(offset)
                        }
                    },
                    onDrag = { change, _ ->
                        if (currentPathPoints.isNotEmpty()) {
                            currentPathPoints = currentPathPoints + change.position
                        }
                    },
                    onDragEnd = {
                        if (currentPathPoints.isNotEmpty()) {
                            // Çizim bittiğinde yeni bir DrawObject oluştur ve listeye ekle
                            val hexColor = String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
                            
                            val newPath = AdvancedDrawPath(
                                colorHex = hexColor,
                                strokeWidth = currentStrokeWidth,
                                alpha = if (currentTool == AdvancedToolType.HIGHLIGHTER) 0.45f else if (currentTool == AdvancedToolType.PENCIL) 0.6f else 1f,
                                toolType = currentTool,
                                points = currentPathPoints
                            )
                            onObjectAdded(newPath)
                            currentPathPoints = emptyList() // Aktif çizimi temizle
                        }
                    },
                    onDragCancel = {
                        currentPathPoints = emptyList()
                    }
                )
            }
    ) {
        // 1. Önce kaydedilmiş (eski) objeleri çiz
        objects.forEach { drawObj ->
            if (drawObj is AdvancedDrawPath) {
                val objColor = try {
                    Color(android.graphics.Color.parseColor(drawObj.colorHex)).copy(alpha = drawObj.alpha)
                } catch (e: Exception) {
                    Color.Black.copy(alpha = drawObj.alpha)
                }
                val blendMode = if (drawObj.toolType == AdvancedToolType.HIGHLIGHTER) BlendMode.Multiply else BlendMode.SrcOver
                
                withTransform({
                    translate(left = drawObj.offsetX, top = drawObj.offsetY)
                    scale(drawObj.scale, drawObj.scale, pivot = Offset.Zero)
                }) {
                    drawPath(
                        path = drawObj.createSmoothPath(),
                        color = objColor,
                        style = Stroke(
                            width = drawObj.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = blendMode
                    )
                }
            } else if (drawObj is AdvancedDrawShape) {
                // Future implementation for shapes
            }
        }

        // 2. Kullanıcının şu an çizmekte olduğu (henüz bitmeyen) yolu çiz
        if (currentPathPoints.isNotEmpty()) {
            val tempDrawPath = AdvancedDrawPath(
                points = currentPathPoints
            )
            val activeColor = currentColor.copy(alpha = if (currentTool == AdvancedToolType.HIGHLIGHTER) 0.45f else if (currentTool == AdvancedToolType.PENCIL) 0.6f else 1f)
            val blendMode = if (currentTool == AdvancedToolType.HIGHLIGHTER) BlendMode.Multiply else BlendMode.SrcOver

            drawPath(
                path = tempDrawPath.createSmoothPath(),
                color = activeColor,
                style = Stroke(
                    width = currentStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = blendMode
            )
        }
    }
}
