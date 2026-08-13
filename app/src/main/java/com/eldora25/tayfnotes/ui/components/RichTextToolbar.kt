package com.eldora25.tayfnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eldora25.tayfnotes.ui.theme.TayfFonts

@Composable
fun RichTextToolbar(
    currentFontFamily: String,
    onFontFamilyChange: (String) -> Unit,
    currentFontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    currentTextColor: Color,
    onTextColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFontDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Font Family
            ToolbarButton(
                icon = Icons.Default.FontDownload,
                label = currentFontFamily,
                onClick = { showFontDialog = true }
            )

            // Font Size
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FormatSize, null, modifier = Modifier.size(18.dp))
                Slider(
                    value = currentFontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 8f..50f,
                    modifier = Modifier.width(120.dp)
                )
                Text("${currentFontSize.toInt()}", style = MaterialTheme.typography.labelSmall)
            }

            // Text Color
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(currentTextColor)
                    .clickable { showColorDialog = true }
                    .background(if (currentTextColor == Color.Transparent) Color.LightGray else currentTextColor)
            )
        }
    }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Yazı Tipi Seç") },
            text = {
                Box(modifier = Modifier.height(300.dp)) {
                    LazyColumn {
                        items(TayfFonts.keys.toList()) { fontName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        onFontFamilyChange(fontName)
                                        showFontDialog = false 
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = fontName == currentFontFamily, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Text(fontName, fontFamily = TayfFonts[fontName], fontSize = 18.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showFontDialog = false }) { Text("Kapat") } }
        )
    }

    if (showColorDialog) {
        val colors = listOf(
            Color.Black, Color.White, Color.Red, Color.Blue, Color.Green, 
            Color.Yellow, Color.Magenta, Color.Cyan, Color.Gray, Color.DarkGray
        )
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Metin Rengi") },
            text = {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { 
                                    onTextColorChange(color)
                                    showColorDialog = false 
                                }
                                .background(if (color == Color.White) Color.LightGray else color)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showColorDialog = false }) { Text("İptal") } }
        )
    }
}

@Composable
private fun ToolbarButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
