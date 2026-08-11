package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.eldora25.tayfnotes.shared.model.drawing.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun calculateAdvancedShapePath(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    shapeType: ShapeType
): Path {
    val path = Path()
    val left = min(startX, endX)
    val top = min(startY, endY)
    val right = maxOf(startX, endX)
    val bottom = maxOf(startY, endY)
    val width = right - left
    val height = bottom - top
    val centerX = left + width / 2
    val centerY = top + height / 2

    if (width <= 0 || height <= 0) return path

    when (shapeType) {
        ShapeType.RECTANGLE -> path.addRect(Rect(left, top, right, bottom))
        ShapeType.SQUARE -> {
            val side = min(width, height)
            path.addRect(Rect(left, top, left + side, top + side))
        }
        ShapeType.ELLIPSE -> path.addOval(Rect(left, top, right, bottom))
        ShapeType.CIRCLE -> {
            val radius = min(width, height) / 2
            path.addOval(Rect(centerX - radius, centerY - radius, centerX + radius, centerY + radius))
        }
        ShapeType.EQUILATERAL_TRIANGLE -> {
            path.moveTo(centerX, top)
            path.lineTo(right, bottom)
            path.lineTo(left, bottom)
            path.close()
        }
        ShapeType.RIGHT_TRIANGLE -> {
            path.moveTo(left, top)
            path.lineTo(left, bottom)
            path.lineTo(right, bottom)
            path.close()
        }
        ShapeType.DIAMOND -> {
            path.moveTo(centerX, top)
            path.lineTo(right, centerY)
            path.lineTo(centerX, bottom)
            path.lineTo(left, centerY)
            path.close()
        }
        ShapeType.PARALLELOGRAM -> {
            val offset = width * 0.2f
            path.moveTo(left + offset, top)
            path.lineTo(right, top)
            path.lineTo(right - offset, bottom)
            path.lineTo(left, bottom)
            path.close()
        }
        ShapeType.TRAPEZOID -> {
            val offset = width * 0.2f
            path.moveTo(left + offset, top)
            path.lineTo(right - offset, top)
            path.lineTo(right, bottom)
            path.lineTo(left, bottom)
            path.close()
        }
        ShapeType.PENTAGON -> drawPolygon(path, centerX, centerY, width / 2, height / 2, 5)
        ShapeType.HEXAGON -> drawPolygon(path, centerX, centerY, width / 2, height / 2, 6)
        ShapeType.STAR -> drawStar(path, centerX, centerY, width / 2, height / 2)
        ShapeType.ARC -> path.addArc(Rect(left, top, right, bottom), 180f, 180f)
        ShapeType.LINE -> {
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)
        }
        ShapeType.DOUBLE_ARROW -> {
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)
            path.addOval(Rect(startX - 5f, startY - 5f, startX + 5f, startY + 5f))
            path.addOval(Rect(endX - 5f, endY - 5f, endX + 5f, endY + 5f))
        }
        else -> {}
    }
    return path
}

private fun drawPolygon(path: Path, cx: Float, cy: Float, rx: Float, ry: Float, sides: Int) {
    val angle = 2.0 * PI / sides
    for (i in 0 until sides) {
        val currentAngle = i * angle - PI / 2
        val x = (cx + rx * cos(currentAngle)).toFloat()
        val y = (cy + ry * sin(currentAngle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
}

private fun drawStar(path: Path, cx: Float, cy: Float, rx: Float, ry: Float) {
    val points = 5
    val angle = PI / points
    for (i in 0 until points * 2) {
        val radiusX = if (i % 2 == 0) rx else rx / 2
        val radiusY = if (i % 2 == 0) ry else ry / 2
        val currentAngle = i * angle - PI / 2
        val x = (cx + radiusX * cos(currentAngle)).toFloat()
        val y = (cy + radiusY * sin(currentAngle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
}
