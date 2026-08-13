package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.drawing.ShapeType

@Composable
fun CanvasSettingsPopup(
    activeColor: Color,
    onColorSelected: (Color) -> Unit,
    activeStrokeWidth: Float,
    onStrokeWidthChanged: (Float) -> Unit,
    activeShape: ShapeType,
    onShapeSelected: (ShapeType) -> Unit,
    isFillEnabled: Boolean,
    onToggleFill: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorPalette = listOf(
        Color(0xFFFFFFFF), Color(0xFF1A1C1E), Color(0xFFFF5252), Color(0xFFFFAB40), 
        Color(0xFFFFD740), Color(0xFF69F0AE), Color(0xFF40C4FF), Color(0xFFB388FF),
        Color.Transparent // Triggers full color picker
    )

    Surface(
        modifier = modifier.padding(bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stroke Width
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LineWeight, null, modifier = Modifier.size(16.dp))
                Slider(
                    value = activeStrokeWidth,
                    onValueChange = onStrokeWidthChanged,
                    valueRange = 2f..100f,
                    modifier = Modifier.width(180.dp).padding(horizontal = 8.dp)
                )
                Text("${activeStrokeWidth.toInt()}px", style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Color Palette
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colorPalette.forEach { color ->
                    val isSelected = color == activeColor
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (color == Color.Transparent) Color.LightGray else color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == Color.Transparent) Icon(Icons.Default.Palette, null, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shape & Fill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Fill Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFillEnabled, onCheckedChange = onToggleFill)
                    Text("Doldur", style = MaterialTheme.typography.labelMedium)
                }

                // Shape Select (simplified for popup)
                IconButton(onClick = { onShapeSelected(ShapeType.RECTANGLE) }) {
                    Icon(Icons.Default.Rectangle, null, tint = if (activeShape == ShapeType.RECTANGLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { onShapeSelected(ShapeType.CIRCLE) }) {
                    Icon(Icons.Default.Circle, null, tint = if (activeShape == ShapeType.CIRCLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
