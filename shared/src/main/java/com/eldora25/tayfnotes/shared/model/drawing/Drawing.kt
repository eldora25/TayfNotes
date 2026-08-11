package com.eldora25.tayfnotes.shared.model.drawing

import kotlinx.serialization.Serializable

@Serializable
data class DrawObject(
    val id: String,
    val points: List<Point>,
    val colorHex: String,
    val strokeWidth: Float,
    val toolType: ToolType = ToolType.PEN,
    val shapeType: ShapeType? = null,
    val isFilled: Boolean = false,
    val fillColorHex: String? = null,
    val zIndex: Int = 0,
    val pathData: String? = null
)

enum class ToolType { PEN, MARKER, PIXEL_ERASER, OBJECT_ERASER, SHAPE, SELECT, PAINT_BUCKET }
enum class ShapeType {
    SQUARE, RECTANGLE, CIRCLE, ELLIPSE, EQUILATERAL_TRIANGLE, RIGHT_TRIANGLE,
    TRAPEZOID, PARALLELOGRAM, DIAMOND, PENTAGON, HEXAGON, STAR, ARC, LINE, DOUBLE_ARROW,
    INTERSECTION
}

@Serializable
data class Point(val x: Float, val y: Float)
