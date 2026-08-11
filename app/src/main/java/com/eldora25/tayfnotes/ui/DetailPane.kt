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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.eldora25.tayfnotes.shared.model.ChecklistItem
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import com.eldora25.tayfnotes.shared.model.drawing.*
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
                    drawObjects.forEach { drawDrawObject(it) }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDrawObject(obj: DrawObject) {
    val color = if (obj.toolType == ToolType.ERASER) Color.White else Color(android.graphics.Color.parseColor(obj.colorHex)).run {
        if (obj.toolType == ToolType.MARKER) this.copy(alpha = 0.45f) else this
    }
    val fillColor = if (obj.isFilled && obj.fillColorHex != null) Color(android.graphics.Color.parseColor(obj.fillColorHex)) else Color.Transparent
    val blendMode = if (obj.toolType == ToolType.MARKER) BlendMode.Multiply else BlendMode.SrcOver

    if (obj.toolType == ToolType.SHAPE && obj.points.size >= 2) {
        val start = Offset(obj.points[0].x, obj.points[0].y)
        val end = Offset(obj.points[1].x, obj.points[1].y)
        val left = minOf(start.x, end.x)
        val top = minOf(start.y, end.y)
        val width = Math.abs(start.x - end.x)
        val height = Math.abs(start.y - end.y)

        when (obj.shapeType) {
            ShapeType.SQUARE -> {
                val side = minOf(width, height)
                if (obj.isFilled) drawRect(fillColor, Offset(left, top), Size(side, side))
                drawRect(color, Offset(left, top), Size(side, side), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.RECTANGLE -> {
                if (obj.isFilled) drawRect(fillColor, Offset(left, top), Size(width, height))
                drawRect(color, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.CIRCLE -> {
                val radius = minOf(width, height) / 2
                if (obj.isFilled) drawCircle(fillColor, radius, Offset(left + width/2, top + height/2))
                drawCircle(color, radius, Offset(left + width/2, top + height/2), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.ELLIPSE -> {
                if (obj.isFilled) drawOval(fillColor, Offset(left, top), Size(width, height))
                drawOval(color, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.EQUILATERAL_TRIANGLE -> {
                val path = Path().apply {
                    moveTo(left + width/2, top)
                    lineTo(left, top + height)
                    lineTo(left + width, top + height)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.RIGHT_TRIANGLE -> {
                val path = Path().apply {
                    moveTo(left, top)
                    lineTo(left, top + height)
                    lineTo(left + width, top + height)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.TRAPEZOID -> {
                val path = Path().apply {
                    moveTo(left + width * 0.25f, top)
                    lineTo(left + width * 0.75f, top)
                    lineTo(left + width, top + height)
                    lineTo(left, top + height)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.PARALLELOGRAM -> {
                val path = Path().apply {
                    moveTo(left + width * 0.25f, top)
                    lineTo(left + width, top)
                    lineTo(left + width * 0.75f, top + height)
                    lineTo(left, top + height)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.DIAMOND -> {
                val path = Path().apply {
                    moveTo(left + width/2, top)
                    lineTo(left + width, top + height/2)
                    lineTo(left + width/2, top + height)
                    lineTo(left, top + height/2)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.PENTAGON -> {
                val path = drawPolygon(left, top, width, height, 5)
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.HEXAGON -> {
                val path = drawPolygon(left, top, width, height, 6)
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.STAR -> {
                val path = drawStar(left, top, width, height)
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.ARC -> {
                drawArc(color, 0f, 180f, false, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.LINE -> {
                drawLine(color, start, end, strokeWidth = obj.strokeWidth)
            }
            ShapeType.DOUBLE_ARROW -> {
                drawLine(color, start, end, strokeWidth = obj.strokeWidth)
                drawArrowHead(start, end, color, obj.strokeWidth)
                drawArrowHead(end, start, color, obj.strokeWidth)
            }
            else -> {}
        }
    } else {
        val path = Path()
        if (obj.points.isNotEmpty()) {
            path.moveTo(obj.points[0].x, obj.points[0].y)
            obj.points.forEach { path.lineTo(it.x, it.y) }
            drawPath(path = path, color = color, style = Stroke(width = obj.strokeWidth, cap = StrokeCap.Round), blendMode = blendMode)
        }
    }
}

private fun drawPolygon(left: Float, top: Float, width: Float, height: Float, sides: Int): Path {
    val path = Path()
    val centerX = left + width / 2
    val centerY = top + height / 2
    val radius = minOf(width, height) / 2
    for (i in 0 until sides) {
        val angle = 2.0 * Math.PI * i / sides - Math.PI / 2
        val x = centerX + radius * Math.cos(angle).toFloat()
        val y = centerY + radius * Math.sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun drawStar(left: Float, top: Float, width: Float, height: Float): Path {
    val path = Path()
    val centerX = left + width / 2
    val centerY = top + height / 2
    val outerRadius = minOf(width, height) / 2
    val innerRadius = outerRadius * 0.4f
    for (i in 0 until 10) {
        val angle = Math.PI * i / 5 - Math.PI / 2
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = centerX + r * Math.cos(angle).toFloat()
        val y = centerY + r * Math.sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(start: Offset, end: Offset, color: Color, strokeWidth: Float) {
    val angle = Math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val arrowLength = 20f + strokeWidth
    val arrowAngle = Math.PI / 6
    val x1 = end.x - arrowLength * Math.cos(angle - arrowAngle).toFloat()
    val y1 = end.y - arrowLength * Math.sin(angle - arrowAngle).toFloat()
    val x2 = end.x - arrowLength * Math.cos(angle + arrowAngle).toFloat()
    val y2 = end.y - arrowLength * Math.sin(angle + arrowAngle).toFloat()
    drawLine(color, end, Offset(x1, y1), strokeWidth = strokeWidth)
    drawLine(color, end, Offset(x2, y2), strokeWidth = strokeWidth)
}

