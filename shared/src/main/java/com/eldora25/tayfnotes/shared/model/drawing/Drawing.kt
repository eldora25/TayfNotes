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
    val zIndex: Int = 0
)

enum class ToolType { PEN, MARKER, ERASER, SHAPE, SELECT }
enum class ShapeType { RECTANGLE, CIRCLE, TRIANGLE, ELLIPSE, ARC }

@Serializable
data class Point(val x: Float, val y: Float)
