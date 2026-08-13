package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eldora25.tayfnotes.shared.model.drawing.ShapeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapeSelectionBottomSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    onShapeSelected: (ShapeType) -> Unit
) {
    if (isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Geometrik Şekiller",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                val shapes = listOf(
                    ShapeType.SQUARE to Icons.Default.Square,
                    ShapeType.RECTANGLE to Icons.Default.Rectangle,
                    ShapeType.CIRCLE to Icons.Default.Circle,
                    ShapeType.ELLIPSE to Icons.Default.FilterTiltShift,
                    ShapeType.EQUILATERAL_TRIANGLE to Icons.Default.ChangeHistory,
                    ShapeType.RIGHT_TRIANGLE to Icons.Default.Details,
                    ShapeType.TRAPEZOID to Icons.Default.Category,
                    ShapeType.PARALLELOGRAM to Icons.Default.Layers,
                    ShapeType.DIAMOND to Icons.Default.Diamond,
                    ShapeType.PENTAGON to Icons.Default.Pentagon,
                    ShapeType.HEXAGON to Icons.Default.Hexagon,
                    ShapeType.OCTAGON to Icons.Default.Polyline,
                    ShapeType.STAR to Icons.Default.Star,
                    ShapeType.HEART to Icons.Default.Favorite,
                    ShapeType.CLOUD to Icons.Default.Cloud,
                    ShapeType.BUBBLE to Icons.Default.ChatBubble,
                    ShapeType.LINE to Icons.Default.HorizontalRule,
                    ShapeType.ARROW_RIGHT to Icons.AutoMirrored.Filled.ArrowForward,
                    ShapeType.ARROW_LEFT to Icons.AutoMirrored.Filled.ArrowBack,
                    ShapeType.ARROW_UP to Icons.Default.ArrowUpward,
                    ShapeType.ARROW_DOWN to Icons.Default.ArrowDownward,
                    ShapeType.CHECKMARK to Icons.Default.Check,
                    ShapeType.CROSS to Icons.Default.Close,
                    ShapeType.PLUS to Icons.Default.Add,
                    ShapeType.MINUS to Icons.Default.Remove
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    items(shapes) { (shapeType, icon) ->
                        ShapeGridItem(
                            shapeType = shapeType,
                            icon = icon,
                            onClick = {
                                onShapeSelected(shapeType)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShapeGridItem(
    shapeType: ShapeType,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val displayName = shapeType.name
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
