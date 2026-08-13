package com.eldora25.tayfnotes.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfHelper {
    /**
     * Render PDF pages to Bitmaps for canvas background
     */
    fun renderPdfToBitmaps(context: Context, uri: Uri): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val contentResolver = context.contentResolver
        val fileDescriptor: ParcelFileDescriptor? = contentResolver.openFileDescriptor(uri, "r")
        
        fileDescriptor?.let {
            val renderer = PdfRenderer(it)
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            renderer.close()
        }
        return bitmaps
    }

    /**
     * Save rendered bitmaps to local storage and return URIs
     */
    fun saveBitmapsToCache(context: Context, bitmaps: List<Bitmap>): List<String> {
        return bitmaps.mapIndexed { index, bitmap ->
            val file = File(context.cacheDir, "pdf_page_${System.currentTimeMillis()}_$index.png")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            file.absolutePath
        }
    }
}
