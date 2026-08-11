package com.eldora25.tayfnotes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eldora25.tayfnotes.shared.model.ChecklistItem
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import com.eldora25.tayfnotes.shared.model.drawing.*
import com.eldora25.tayfnotes.ui.components.canvas.calculateAdvancedShapePath
import com.eldora25.tayfnotes.ui.components.canvas.calculateIntersectionPath
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DetailPane(
    note: Note?,
    modifier: Modifier = Modifier
) {
    if (note == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Görüntülemek için bir not seçin", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.surface
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor.copy(alpha = 0.08f))
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = note.title.ifEmpty { "Başlıksız Not" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr")).format(Date(note.lastModified)),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        if (note.imageUris.isNotEmpty()) {
            LazyRow(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(note.imageUris) { uri ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 4.dp,
                        modifier = Modifier.height(250.dp).width(350.dp)
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }

        if (note.type == NoteType.CHECKLIST) {
            val items = try { Json.decodeFromString<List<ChecklistItem>>(note.content) } catch(_: Exception) { emptyList() }
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                    Checkbox(checked = item.isChecked, onCheckedChange = null, enabled = false)
                    Text(
                        text = item.text,
                        style = if (item.isChecked) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge,
                        color = if (item.isChecked) Color.Gray else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        } else {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        if (note.sketchData?.isNotEmpty() == true) {
            val drawObjects = remember(note.sketchData) {
                try { Json.decodeFromString<List<DrawObject>>(note.sketchData!!) } catch(_: Exception) { emptyList() }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("Sketch Çizimi", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(450.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.LightGray.copy(0.5f))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    drawObjects.forEach { drawDrawObject(it, drawObjects) }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDrawObject(obj: DrawObject, allObjects: List<DrawObject>) {
    val color = if (obj.toolType == ToolType.PIXEL_ERASER) Color.White else Color(android.graphics.Color.parseColor(obj.colorHex)).run {
        if (obj.toolType == ToolType.MARKER || obj.toolType == ToolType.HIGHLIGHTER) this.copy(alpha = 0.45f * obj.alpha) else this.copy(alpha = obj.alpha)
    }
    val fillColor = if (obj is DrawShape && obj.isFilled && obj.fillColorHex != null) Color(android.graphics.Color.parseColor(obj.fillColorHex)).copy(alpha = obj.alpha) else Color.Transparent
    val blendMode = if (obj.toolType == ToolType.MARKER || obj.toolType == ToolType.HIGHLIGHTER) BlendMode.Multiply else BlendMode.SrcOver

    withTransform({
        translate(left = obj.offsetX, top = obj.offsetY)
        scale(obj.scale, obj.scale, pivot = Offset.Zero)
    }) {
        if (obj is DrawPath) {
            if (obj.points.isNotEmpty()) {
                val path = createSmoothPath(obj.points)
                drawPath(path = path, color = color, style = Stroke(width = obj.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blendMode)
            }
        } else if (obj is DrawShape) {
            drawAdvancedShape(obj, color, fillColor, allObjects)
        }
    }
}

private fun createSmoothPath(points: List<Point>): Path {
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAdvancedShape(
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

private fun getObjectPath(obj: DrawObject): Path {
    val path = Path()
    if (obj is DrawShape && obj.points.size >= 2) {
        val start = obj.points[0]
        val end = obj.points[1]
        val left = minOf(start.x, end.x)
        val top = minOf(start.y, end.y)
        val width = Math.abs(start.x - end.x)
        val height = Math.abs(start.y - end.y)
        
        when (obj.shapeType) {
            ShapeType.SQUARE -> {
                val side = minOf(width, height)
                path.addRect(Rect(Offset(left, top), Size(side, side)))
            }
            ShapeType.RECTANGLE -> path.addRect(Rect(Offset(left, top), Size(width, height)))
            ShapeType.CIRCLE -> {
                val radius = minOf(width, height) / 2
                path.addOval(Rect(Offset(left + width/2 - radius, top + height/2 - radius), Size(radius * 2, radius * 2)))
            }
            ShapeType.ELLIPSE -> path.addOval(Rect(Offset(left, top), Size(width, height)))
            ShapeType.EQUILATERAL_TRIANGLE -> {
                path.moveTo(left + width/2, top)
                path.lineTo(left, top + height)
                path.lineTo(left + width, top + height)
                path.close()
            }
            ShapeType.RIGHT_TRIANGLE -> {
                path.moveTo(left, top)
                path.lineTo(left, top + height)
                path.lineTo(left + width, top + height)
                path.close()
            }
            ShapeType.DIAMOND -> {
                path.moveTo(left + width/2, top)
                path.lineTo(left + width, top + height/2)
                path.lineTo(left + width/2, top + height)
                path.lineTo(left, top + height/2)
                path.close()
            }
            else -> path.addRect(Rect(Offset(left, top), Size(width, height)))
        }
    } else if (obj is DrawPath && obj.points.isNotEmpty()) {
        path.moveTo(obj.points[0].x, obj.points[0].y)
        obj.points.forEach { path.lineTo(it.x, it.y) }
    }
    return path
}
