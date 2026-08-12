package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import com.eldora25.tayfnotes.shared.model.drawing.*

fun getObjectBounds(obj: DrawObject): Rect {
    if (obj is DrawText) {
        // Approximate bounds for text
        val size = obj.strokeWidth * obj.scale
        return Rect(obj.offsetX, obj.offsetY, obj.offsetX + size * obj.text.length * 0.6f, obj.offsetY + size)
    }
    
    if (obj is DrawImage) {
        return Rect(obj.offsetX, obj.offsetY, obj.offsetX + obj.width * obj.scale, obj.offsetY + obj.height * obj.scale)
    }
    
    if (obj.points.isEmpty() && obj is DrawShape && obj.shapeType != ShapeType.INTERSECTION) return Rect.Zero
    
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    
    if (obj.points.isNotEmpty()) {
        obj.points.forEach {
            minX = minOf(minX, it.x)
            minY = minOf(minY, it.y)
            maxX = maxOf(maxX, it.x)
            maxY = maxOf(maxY, it.y)
        }
    } else {
        return Rect.Zero
    }
    
    val width = (maxX - minX) * obj.scale
    val height = (maxY - minY) * obj.scale
    val centerX = (minX + maxX) / 2 + obj.offsetX
    val centerY = (minY + maxY) / 2 + obj.offsetY
    
    return Rect(centerX - width/2, centerY - height/2, centerX + width/2, centerY + height/2)
}

fun createSmoothPath(points: List<Point>): Path {
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

fun extractPathFromDrawObject(obj: DrawObject): Path {
    val base = when (obj) {
        is DrawShape -> {
            if (obj.shapeType == ShapeType.INTERSECTION && obj.pathData != null) {
                Path() 
            } else {
                calculateAdvancedShapePath(obj.points[0].x, obj.points[0].y, obj.points[1].x, obj.points[1].y, obj.shapeType)
            }
        }
        is DrawPath -> createSmoothPath(obj.points)
        is DrawText -> Path() 
        is DrawImage -> Path()
    }
    
    val matrix = Matrix()
    matrix.translate(obj.offsetX, obj.offsetY)
    matrix.scale(obj.scale, obj.scale)
    base.transform(matrix)
    
    return base
}

fun getObjectPath(obj: DrawObject): Path {
    return extractPathFromDrawObject(obj)
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAdvancedShape(
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
