package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.drawing.ToolType

@Composable
fun FloatingToolBar(
    currentTool: ToolType,
    onToolSelected: (ToolType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(4.dp)
            .shadow(12.dp, RoundedCornerShape(32.dp)),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // General Tools
            ToolButton(Icons.Default.PanToolAlt, "Seçici", currentTool == ToolType.SELECTOR) { onToolSelected(ToolType.SELECTOR) }
            ToolButton(Icons.Default.Gesture, "Kement", currentTool == ToolType.LASSO) { onToolSelected(ToolType.LASSO) }
            ToolButton(Icons.Default.BackHand, "El", currentTool == ToolType.PAN) { onToolSelected(ToolType.PAN) }
            
            VerticalDivider()
            
            // Writing Tools
            ToolButton(Icons.Default.Edit, "Kalem", currentTool == ToolType.PEN) { onToolSelected(ToolType.PEN) }
            ToolButton(Icons.Default.HistoryEdu, "Kurşun", currentTool == ToolType.PENCIL) { onToolSelected(ToolType.PENCIL) }
            ToolButton(Icons.Default.Brush, "Fırça", currentTool == ToolType.BRUSH) { onToolSelected(ToolType.BRUSH) }
            ToolButton(Icons.Default.BorderColor, "Marker", currentTool == ToolType.MARKER) { onToolSelected(ToolType.MARKER) }
            ToolButton(Icons.Default.Highlight, "Fosfor", currentTool == ToolType.HIGHLIGHTER) { onToolSelected(ToolType.HIGHLIGHTER) }
            
            VerticalDivider()
            
            // Creative Tools
            ToolButton(Icons.Default.Category, "Şekil", currentTool == ToolType.SHAPE) { onToolSelected(ToolType.SHAPE) }
            ToolButton(Icons.Default.FormatPaint, "Doldur", currentTool == ToolType.PAINT_BUCKET) { onToolSelected(ToolType.PAINT_BUCKET) }
            
            VerticalDivider()
            
            // Erasers
            ToolButton(Icons.Default.AutoFixNormal, "Obje Sil", currentTool == ToolType.OBJECT_ERASER) { onToolSelected(ToolType.OBJECT_ERASER) }
            ToolButton(Icons.Default.CleaningServices, "Piksel Sil", currentTool == ToolType.PIXEL_ERASER) { onToolSelected(ToolType.PIXEL_ERASER) }
            
            VerticalDivider()
            
            // Content
            ToolButton(Icons.Default.Title, "Yazı", currentTool == ToolType.TEXT) { onToolSelected(ToolType.TEXT) }
            ToolButton(Icons.Default.Image, "Resim", currentTool == ToolType.IMAGE) { onToolSelected(ToolType.IMAGE) }
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
    val tint by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun VerticalDivider() {
    Box(modifier = Modifier.height(32.dp).width(1.dp).background(Color.Gray.copy(alpha = 0.2f)).padding(horizontal = 4.dp))
}
