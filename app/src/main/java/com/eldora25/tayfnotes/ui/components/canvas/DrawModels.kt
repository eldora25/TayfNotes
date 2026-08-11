package com.eldora25.tayfnotes.ui.components.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.eldora25.tayfnotes.shared.model.drawing.ShapeType
import java.util.UUID

// Çizim araçlarımızın gelişmiş tipleri
enum class AdvancedToolType {
    PEN,        // Net ve keskin dolma kalem
    PENCIL,     // Yarı saydam, dokulu kurşun kalem
    HIGHLIGHTER,// Fosforlu kalem (Multiply blend mode için)
    SHAPE,      // Geometrik şekil
    ERASER_PIXEL, // Klasik silgi
    ERASER_OBJECT, // Dokunulan objeyi tamamen silen silgi
    SELECT,
    PAINT_BUCKET
}

// Seçim, taşıma ve boyutlandırma için her objenin ortak özellikleri
sealed class DrawObject {
    abstract val id: String
    abstract var colorHex: String
    abstract var strokeWidth: Float
    abstract var alpha: Float
    abstract var toolType: AdvancedToolType
    
    // Objenin ekrandaki taşıma (translation) ve ölçekleme (scale) durumu
    abstract var offsetX: Float
    abstract var offsetY: Float
    abstract var scale: Float
    
    // Seçim motoru için objenin kapladığı dikdörtgen alan
    abstract fun getBoundingBox(): Rect
}

// Serbest çizim yolları (Fırça motorunun çıktısı)
data class AdvancedDrawPath(
    override val id: String = UUID.randomUUID().toString(),
    override var colorHex: String = "#FFFFFF",
    override var strokeWidth: Float = 5f,
    override var alpha: Float = 1f,
    override var toolType: AdvancedToolType = AdvancedToolType.PEN,
    override var offsetX: Float = 0f,
    override var offsetY: Float = 0f,
    override var scale: Float = 1f,
    val points: List<Offset> = emptyList()
) : DrawObject() {
    
    // Compose çizim motoru için pürüzsüz (Bezier) Path üretir
    fun createSmoothPath(): Path {
        val path = Path()
        if (points.isEmpty()) return path

        path.moveTo(points.first().x, points.first().y)
        var currentX = points.first().x
        var currentY = points.first().y

        for (i in 1 until points.size) {
            val nextPoint = points[i]
            // İki nokta arasındaki orta noktayı (kontrol noktası) hesapla
            val midPointX = (currentX + nextPoint.x) / 2
            val midPointY = (currentY + nextPoint.y) / 2
            
            // Quadratic Bezier Curve ile yumuşak geçiş yap
            if (i == 1) {
                path.lineTo(midPointX, midPointY)
            } else {
                path.quadraticBezierTo(currentX, currentY, midPointX, midPointY)
            }
            currentX = nextPoint.x
            currentY = nextPoint.y
        }
        // Son noktayı bağla
        path.lineTo(currentX, currentY)
        return path
    }

    override fun getBoundingBox(): Rect {
        if (points.isEmpty()) return Rect.Zero
        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val maxX = points.maxOf { it.x }
        val maxY = points.maxOf { it.y }
        
        // Offset (Taşıma) ve Ölçekleme (Scale) değerlerini hesaba katarak çerçeveyi döndür
        val width = (maxX - minX) * scale
        val height = (maxY - minY) * scale
        val centerX = (minX + maxX) / 2 + offsetX
        val centerY = (minY + maxY) / 2 + offsetY
        
        return Rect(
            left = centerX - width / 2,
            top = centerY - height / 2,
            right = centerX + width / 2,
            bottom = centerY + height / 2
        )
    }
}

data class AdvancedDrawShape(
    override val id: String = UUID.randomUUID().toString(),
    override var colorHex: String = "#FFFFFF",
    override var strokeWidth: Float = 5f,
    override var alpha: Float = 1f,
    override var toolType: AdvancedToolType = AdvancedToolType.SHAPE,
    override var offsetX: Float = 0f,
    override var offsetY: Float = 0f,
    override var scale: Float = 1f,
    val shapeType: ShapeType,
    val points: List<Offset>, // Usually 2 points: start and end
    var isFilled: Boolean = false,
    var fillColorHex: String? = null,
    val pathData: String? = null
) : DrawObject() {
    override fun getBoundingBox(): Rect {
        if (points.size < 2) return Rect.Zero
        val start = points[0]
        val end = points[1]
        
        val left = minOf(start.x, end.x)
        val top = minOf(start.y, end.y)
        val right = maxOf(start.x, end.x)
        val bottom = maxOf(start.y, end.y)
        
        val width = (right - left) * scale
        val height = (bottom - top) * scale
        val centerX = (left + right) / 2 + offsetX
        val centerY = (top + bottom) / 2 + offsetY
        
        return Rect(
            left = centerX - width / 2,
            top = centerY - height / 2,
            right = centerX + width / 2,
            bottom = centerY + height / 2
        )
    }
}
