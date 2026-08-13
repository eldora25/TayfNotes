package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.eldora25.tayfnotes.shared.model.drawing.*
import kotlin.math.*

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
        ShapeType.OCTAGON -> drawPolygon(path, centerX, centerY, width / 2, height / 2, 8)
        ShapeType.STAR -> drawStar(path, centerX, centerY, width / 2, height / 2)
        ShapeType.ARC -> path.addArc(Rect(left, top, right, bottom), 180f, 180f)
        ShapeType.LINE -> {
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)
        }
        ShapeType.ARROW_RIGHT -> {
            val arrowHeadWidth = min(width * 0.3f, 50f)
            path.moveTo(left, centerY)
            path.lineTo(right, centerY)
            path.moveTo(right - arrowHeadWidth, top + height * 0.2f)
            path.lineTo(right, centerY)
            path.lineTo(right - arrowHeadWidth, bottom - height * 0.2f)
        }
        ShapeType.ARROW_LEFT -> {
            val arrowHeadWidth = min(width * 0.3f, 50f)
            path.moveTo(right, centerY)
            path.lineTo(left, centerY)
            path.moveTo(left + arrowHeadWidth, top + height * 0.2f)
            path.lineTo(left, centerY)
            path.lineTo(left + arrowHeadWidth, bottom - height * 0.2f)
        }
        ShapeType.ARROW_UP -> {
            val arrowHeadHeight = min(height * 0.3f, 50f)
            path.moveTo(centerX, bottom)
            path.lineTo(centerX, top)
            path.moveTo(centerX - width * 0.2f, top + arrowHeadHeight)
            path.lineTo(centerX, top)
            path.lineTo(centerX + width * 0.2f, top + arrowHeadHeight)
        }
        ShapeType.ARROW_DOWN -> {
            val arrowHeadHeight = min(height * 0.3f, 50f)
            path.moveTo(centerX, top)
            path.lineTo(centerX, bottom)
            path.moveTo(centerX - width * 0.2f, bottom - arrowHeadHeight)
            path.lineTo(centerX, bottom)
            path.lineTo(centerX + width * 0.2f, bottom - arrowHeadHeight)
        }
        ShapeType.CHECKMARK -> {
            path.moveTo(left + width * 0.2f, centerY)
            path.lineTo(left + width * 0.45f, bottom - height * 0.1f)
            path.lineTo(right - width * 0.1f, top + height * 0.1f)
        }
        ShapeType.CROSS -> {
            path.moveTo(left, top)
            path.lineTo(right, bottom)
            path.moveTo(right, top)
            path.lineTo(left, bottom)
        }
        ShapeType.PLUS -> {
            path.moveTo(centerX, top)
            path.lineTo(centerX, bottom)
            path.moveTo(left, centerY)
            path.lineTo(right, centerY)
        }
        ShapeType.MINUS -> {
            path.moveTo(left, centerY)
            path.lineTo(right, centerY)
        }
        ShapeType.HEART -> {
            path.moveTo(centerX, top + height * 0.3f)
            path.cubicTo(left, top - height * 0.1f, left - width * 0.1f, centerY + height * 0.1f, centerX, bottom)
            path.cubicTo(right + width * 0.1f, centerY + height * 0.1f, right, top - height * 0.1f, centerX, top + height * 0.3f)
        }
        ShapeType.CLOUD -> {
            path.addOval(Rect(left, centerY - height * 0.2f, left + width * 0.4f, bottom))
            path.addOval(Rect(centerX - width * 0.2f, top, centerX + width * 0.2f, centerY + height * 0.3f))
            path.addOval(Rect(right - width * 0.4f, centerY - height * 0.2f, right, bottom))
            path.addRect(Rect(left + width * 0.2f, centerY, right - width * 0.2f, bottom))
        }
        ShapeType.BUBBLE -> {
            path.addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(left, top, right, bottom - height * 0.2f), androidx.compose.ui.geometry.CornerRadius(16f)))
            path.moveTo(left + width * 0.2f, bottom - height * 0.2f)
            path.lineTo(left + width * 0.1f, bottom)
            path.lineTo(left + width * 0.3f, bottom - height * 0.2f)
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
