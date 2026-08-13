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
    abstract val rotation: Float
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
    override val rotation: Float = 0f,
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
    override val rotation: Float = 0f,
    override val zIndex: Int = 0,
    override val points: List<Point>,
    val shapeType: ShapeType,
    val isFilled: Boolean = false,
    val fillColorHex: String? = null,
    val pathData: String? = null
) : DrawObject()

@Serializable
data class DrawText(
    override val id: String,
    override val colorHex: String,
    override val strokeWidth: Float,
    override val toolType: ToolType = ToolType.TEXT,
    override val alpha: Float = 1f,
    override val offsetX: Float = 0f,
    override val offsetY: Float = 0f,
    override val scale: Float = 1f,
    override val rotation: Float = 0f,
    override val zIndex: Int = 0,
    override val points: List<Point> = emptyList(),
    val text: String,
    val fontFamily: String = "Default"
) : DrawObject()

@Serializable
data class DrawImage(
    override val id: String,
    override val colorHex: String = "#000000",
    override val strokeWidth: Float = 0f,
    override val toolType: ToolType = ToolType.IMAGE,
    override val alpha: Float = 1f,
    override val offsetX: Float = 0f,
    override val offsetY: Float = 0f,
    override val scale: Float = 1f,
    override val rotation: Float = 0f,
    override val zIndex: Int = 0,
    override val points: List<Point> = emptyList(),
    val imageUri: String,
    val width: Float,
    val height: Float
) : DrawObject()

enum class ToolType { 
    PEN, MARKER, PENCIL, HIGHLIGHTER, BRUSH, 
    PIXEL_ERASER, OBJECT_ERASER, STROKE_ERASER,
    SHAPE, SELECTOR, LASSO, PAINT_BUCKET, PAN, TEXT, IMAGE 
}

enum class BooleanOperation { INTERSECT, DIFFERENCE, UNION }

enum class ShapeType {
    SQUARE, RECTANGLE, CIRCLE, ELLIPSE, EQUILATERAL_TRIANGLE, RIGHT_TRIANGLE,
    TRAPEZOID, PARALLELOGRAM, DIAMOND, PENTAGON, HEXAGON, OCTAGON, STAR, ARC, LINE, 
    ARROW_RIGHT, ARROW_LEFT, ARROW_UP, ARROW_DOWN, CHECKMARK, CROSS, PLUS, MINUS,
    HEART, CLOUD, BUBBLE, DOUBLE_ARROW, INTERSECTION
}

enum class CanvasTemplate { BLANK, RULED, GRID, PDF }
enum class CanvasSize { INFINITE, A4, LETTER }

@Serializable
data class Point(val x: Float, val y: Float)
