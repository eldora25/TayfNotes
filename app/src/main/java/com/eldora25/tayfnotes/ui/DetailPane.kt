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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eldora25.tayfnotes.shared.model.ChecklistItem
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import com.eldora25.tayfnotes.shared.model.drawing.*
import com.eldora25.tayfnotes.ui.components.canvas.calculateAdvancedShapePath
import com.eldora25.tayfnotes.ui.components.canvas.calculateIntersectionPath
import com.eldora25.tayfnotes.ui.components.canvas.drawAdvancedShape
import com.eldora25.tayfnotes.ui.components.canvas.createSmoothPath
import com.eldora25.tayfnotes.util.SketchExportHelper
import kotlinx.serialization.json.Json
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DetailPane(
    note: Note?,
    modifier: Modifier = Modifier,
    fontSize: Float = 16f,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
                            style = if (item.isChecked) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough, fontSize = fontSize.sp) else MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp),
                            color = if (item.isChecked) Color.Gray else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            } else {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            if (note.sketchData?.isNotEmpty() == true) {
                val context = LocalContext.current
                val drawObjects = remember(note.sketchData) {
                    try { Json.decodeFromString<List<DrawObject>>(note.sketchData!!) } catch(_: Exception) { emptyList() }
                }
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sketch Çizimi", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    TextButton(
                        onClick = {
                            val exportBg = if (note.colorHex.isNotEmpty()) Color(android.graphics.Color.parseColor(note.colorHex)) else Color.White
                            SketchExportHelper.exportAndShareSketch(context, drawObjects, exportBg)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PNG Olarak Paylaş", style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().height(450.dp).clipToBounds(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.LightGray.copy(0.5f))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        drawObjects.forEach { drawDrawObject(it, drawObjects) }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Leave space for FAB
        }

        // Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = onDelete,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Sil")
            }
            
            FloatingActionButton(
                onClick = onEdit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Düzenle")
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
