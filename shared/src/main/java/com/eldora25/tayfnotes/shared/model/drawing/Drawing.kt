package com.eldora25.tayfnotes.shared.model.drawing

import kotlinx.serialization.Serializable

@Serializable
sealed class DrawObject {
    abstract val id: String
    abstract val colorHex: String
    abstract val strokeWidth: Float
    abstract val toolType: ToolType
    abstract val alpha: Float
    abstract val offsetX: Float
    abstract val offsetY: Float
    abstract val scale: Float
    abstract val zIndex: Int
    abstract val points: List<Point>
}

@Serializable
data class DrawPath(
    override val id: String,
    override val colorHex: String,
    override val strokeWidth: Float,
    override val toolType: ToolType = ToolType.PEN,
    override val alpha: Float = 1f,
    override val offsetX: Float = 0f,
    override val offsetY: Float = 0f,
    override val scale: Float = 1f,
    override val zIndex: Int = 0,
    override val points: List<Point>
) : DrawObject()

@Serializable
data class DrawShape(
    override val id: String,
    override val colorHex: String,
    override val strokeWidth: Float,
    override val toolType: ToolType = ToolType.SHAPE,
    override val alpha: Float = 1f,
    override val offsetX: Float = 0f,
    override val offsetY: Float = 0f,
    override val scale: Float = 1f,
    override val zIndex: Int = 0,
    override val points: List<Point>,
    val shapeType: ShapeType,
    val isFilled: Boolean = false,
    val fillColorHex: String? = null,
    val pathData: String? = null
) : DrawObject()

enum class ToolType { PEN, MARKER, PENCIL, HIGHLIGHTER, PIXEL_ERASER, OBJECT_ERASER, SHAPE, SELECTOR, PAINT_BUCKET }
enum class BooleanOperation { INTERSECT, DIFFERENCE, UNION }
enum class ShapeType {
    SQUARE, RECTANGLE, CIRCLE, ELLIPSE, EQUILATERAL_TRIANGLE, RIGHT_TRIANGLE,
    TRAPEZOID, PARALLELOGRAM, DIAMOND, PENTAGON, HEXAGON, STAR, ARC, LINE, DOUBLE_ARROW,
    INTERSECTION
}

@Serializable
data class Point(val x: Float, val y: Float)
