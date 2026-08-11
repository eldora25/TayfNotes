package com.eldora25.tayfnotes.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.eldora25.tayfnotes.shared.model.drawing.*
import com.eldora25.tayfnotes.ui.components.canvas.*
import java.io.File
import java.io.FileOutputStream

object SketchExportHelper {

    fun exportAndShareSketch(context: Context, objects: List<DrawObject>, backgroundColor: Color = Color.White) {
        if (objects.isEmpty()) return

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        objects.forEach { obj ->
            val bounds = getObjectBounds(obj)
            if (bounds.left < minX) minX = bounds.left
            if (bounds.top < minY) minY = bounds.top
            if (bounds.right > maxX) maxX = bounds.right
            if (bounds.bottom > maxY) maxY = bounds.bottom
        }

        val padding = 100f
        val width = (maxX - minX + padding * 2).toInt().coerceAtLeast(1)
        val height = (maxY - minY + padding * 2).toInt().coerceAtLeast(1)

        val imageBitmap = ImageBitmap(width, height)
        val canvas = Canvas(imageBitmap)
        val drawScope = CanvasDrawScope()

        drawScope.draw(
            density = Density(context),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat())
        ) {
            drawRect(color = backgroundColor, size = size)

            withTransform({
                translate(left = -minX + padding, top = -minY + padding)
            }) {
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
                        scale(drawObj.scale, drawObj.scale, pivot = androidx.compose.ui.geometry.Offset.Zero)
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
                }
            }
        }

        val androidBitmap = imageBitmap.asAndroidBitmap()
        val cachePath = File(context.cacheDir, "shared_sketches")
        cachePath.mkdirs()
        val file = File(cachePath, "sketch_export_${System.currentTimeMillis()}.png")
        
        val stream = FileOutputStream(file)
        androidBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        val uri = FileProvider.getUriForFile(context, "com.eldora25.tayfnotes.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Sketch'i Paylaş (PNG)"))
    }
}
