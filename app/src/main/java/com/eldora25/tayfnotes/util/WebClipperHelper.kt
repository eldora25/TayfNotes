package com.eldora25.tayfnotes.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class ClippedContent(
    val title: String,
    val bodyText: String,
    val sourceUrl: String,
    val mainImageUrl: String? = null
)

object WebClipperHelper {

    // Sunucuya ihtiyaç duymadan cihaz üzerinde çalışan yerel DOM ayrıştırıcı
    suspend fun clipArticleLocally(url: String): Result<ClippedContent> = withContext(Dispatchers.IO) {
        try {
            // Jsoup ile sayfayı indir ve ayrıştır
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(5000)
                .get()

            val title = document.title()
            
            // Makalenin ana içeriğini bulmak için genel HTML etiketlerini filtrele
            val articleElement = document.select("article, main, .post-content, .entry-content").first() 
                ?: document.body()

            // Header, footer, nav ve script etiketlerini temizle
            articleElement.select("script, style, nav, header, footer, aside").remove()

            val bodyText = articleElement.text()

            // Varsa meta etiketlerinden ana resmi (og:image) çek
            val mainImage = document.select("meta[property=og:image]").attr("content").takeIf { it.isNotEmpty() }

            Result.success(
                ClippedContent(
                    title = title,
                    bodyText = bodyText,
                    sourceUrl = url,
                    mainImageUrl = mainImage
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
