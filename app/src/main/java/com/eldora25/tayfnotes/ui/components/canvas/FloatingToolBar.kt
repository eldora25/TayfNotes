package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
            .padding(8.dp)
            .shadow(
                elevation = 16.dp, 
                shape = RoundedCornerShape(32.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                spotColor = MaterialTheme.colorScheme.primary
            ),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(
                icon = Icons.Default.PanToolAlt,
                isSelected = currentTool == ToolType.SELECTOR,
                onClick = { onToolSelected(ToolType.SELECTOR) }
            )
            ToolButton(
                icon = Icons.Default.Gesture,
                isSelected = currentTool == ToolType.LASSO,
                onClick = { onToolSelected(ToolType.LASSO) }
            )
            
            VerticalDivider()

            ToolButton(
                icon = Icons.Default.Edit,
                isSelected = currentTool == ToolType.PEN,
                onClick = { onToolSelected(ToolType.PEN) }
            )
            ToolButton(
                icon = Icons.Default.HistoryEdu,
                isSelected = currentTool == ToolType.PENCIL,
                onClick = { onToolSelected(ToolType.PENCIL) }
            )
            ToolButton(
                icon = Icons.Default.Brush,
                isSelected = currentTool == ToolType.BRUSH,
                onClick = { onToolSelected(ToolType.BRUSH) }
            )
            ToolButton(
                icon = Icons.Default.BorderColor,
                isSelected = currentTool == ToolType.MARKER,
                onClick = { onToolSelected(ToolType.MARKER) }
            )
            ToolButton(
                icon = Icons.Default.Highlight,
                isSelected = currentTool == ToolType.HIGHLIGHTER,
                onClick = { onToolSelected(ToolType.HIGHLIGHTER) }
            )
            
            VerticalDivider()
            
            ToolButton(
                icon = Icons.Default.Category,
                isSelected = currentTool == ToolType.SHAPE,
                onClick = { onToolSelected(ToolType.SHAPE) }
            )
            ToolButton(
                icon = Icons.Default.FormatPaint,
                isSelected = currentTool == ToolType.PAINT_BUCKET,
                onClick = { onToolSelected(ToolType.PAINT_BUCKET) }
            )
            
            VerticalDivider()

            ToolButton(
                icon = Icons.Default.AutoFixNormal,
                isSelected = currentTool == ToolType.OBJECT_ERASER,
                onClick = { onToolSelected(ToolType.OBJECT_ERASER) }
            )
            ToolButton(
                icon = Icons.Default.CleaningServices,
                isSelected = currentTool == ToolType.PIXEL_ERASER,
                onClick = { onToolSelected(ToolType.PIXEL_ERASER) }
            )
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "ToolBgColor"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "ToolIconColor"
    )
    
    val size by animateDpAsState(
        targetValue = if (isSelected) 48.dp else 40.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ToolSize"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}
