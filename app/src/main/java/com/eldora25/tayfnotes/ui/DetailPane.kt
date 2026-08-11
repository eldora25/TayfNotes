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
            ShapeType.RECTANGLE -> {
                if (obj.isFilled) drawRect(fillColor, Offset(left, top), Size(width, height))
                drawRect(color, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.CIRCLE -> {
                val radius = Math.sqrt((width * width + height * height).toDouble()).toFloat() / 2
                if (obj.isFilled) drawCircle(fillColor, radius, Offset(left + width/2, top + height/2))
                drawCircle(color, radius, Offset(left + width/2, top + height/2), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(left + width/2, top)
                    lineTo(left, top + height)
                    lineTo(left + width, top + height)
                    close()
                }
                if (obj.isFilled) drawPath(path, fillColor)
                drawPath(path, color, style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.ELLIPSE -> {
                if (obj.isFilled) drawOval(fillColor, Offset(left, top), Size(width, height))
                drawOval(color, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
            }
            ShapeType.ARC -> {
                drawArc(color, 0f, 180f, false, Offset(left, top), Size(width, height), style = Stroke(width = obj.strokeWidth))
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
