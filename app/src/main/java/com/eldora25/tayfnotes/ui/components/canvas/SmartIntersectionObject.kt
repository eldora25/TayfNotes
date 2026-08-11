package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import com.eldora25.tayfnotes.shared.model.drawing.*

fun calculateIntersectionPath(path1: Path, path2: Path, operation: PathOperation = PathOperation.Intersect): Path {
    val resultPath = Path()
    resultPath.op(path1, path2, operation)
    return resultPath
}
