package com.eldora25.tayfnotes.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

enum class ClipMode {
    ARTICLE,               // 1) Makale (Metin ve görsel kaydetme)
    SIMPLIFIED_ARTICLE,    // 2) Basitleştirilmiş Makale (Düz sade metin)
    FULL_PAGE,             // 3) Tam Sayfa (Sayfanın tamamı HTML/Text)
    BOOKMARK,              // 4) Yer İşareti (Başlık, Açıklama/Özet ve Link kartı)
    SCREENSHOT             // 5) Ekran Görüntüsü (Görsel kırpma ve çizim)
}

data class ClippedContent(
    val title: String,
    val bodyText: String,
    val sourceUrl: String,
    val mainImageUrl: String? = null,
    val htmlContent: String? = null,
    val clipMode: ClipMode = ClipMode.ARTICLE,
    val screenshotPath: String? = null
)

object WebClipperHelper {

    // Sunucuya ihtiyaç duymadan cihaz üzerinde çalışan yerel DOM ayrıştırıcı
    suspend fun clipArticleLocally(url: String, mode: ClipMode = ClipMode.ARTICLE): Result<ClippedContent> = withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get()

            val title = document.title().ifBlank { url }
            val metaDescription = document.select("meta[name=description], meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
            val mainImage = document.select("meta[property=og:image], meta[name=twitter:image]").attr("content").takeIf { it.isNotBlank() }

            val rawArticleElement = document.select("article, main, .post-content, .entry-content, #content, .content").first() 
                ?: document.body()

            when (mode) {
                ClipMode.ARTICLE -> {
                    val cloned = rawArticleElement.clone()
                    cloned.select("script, style, nav, header, footer, aside, .advertisement, .ads, .comment").remove()
                    val text = cloned.text()
                    val html = cloned.html()
                    Result.success(
                        ClippedContent(
                            title = title,
                            bodyText = text.ifBlank { metaDescription ?: "" },
                            sourceUrl = url,
                            mainImageUrl = mainImage,
                            htmlContent = html,
                            clipMode = ClipMode.ARTICLE
                        )
                    )
                }
                ClipMode.SIMPLIFIED_ARTICLE -> {
                    val cloned = rawArticleElement.clone()
                    cloned.select("script, style, nav, header, footer, aside, img, svg, iframe, form, button, input").remove()
                    val plainText = cloned.wholeText().lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString("\n\n")
                    Result.success(
                        ClippedContent(
                            title = title,
                            bodyText = plainText.ifBlank { metaDescription ?: "" },
                            sourceUrl = url,
                            mainImageUrl = null,
                            clipMode = ClipMode.SIMPLIFIED_ARTICLE
                        )
                    )
                }
                ClipMode.FULL_PAGE -> {
                    val text = document.body().text()
                    Result.success(
                        ClippedContent(
                            title = title,
                            bodyText = text,
                            sourceUrl = url,
                            mainImageUrl = mainImage,
                            htmlContent = document.html(),
                            clipMode = ClipMode.FULL_PAGE
                        )
                    )
                }
                ClipMode.BOOKMARK -> {
                    val summary = metaDescription ?: rawArticleElement.text().take(250)
                    Result.success(
                        ClippedContent(
                            title = title,
                            bodyText = summary,
                            sourceUrl = url,
                            mainImageUrl = mainImage,
                            clipMode = ClipMode.BOOKMARK
                        )
                    )
                }
                ClipMode.SCREENSHOT -> {
                    Result.success(
                        ClippedContent(
                            title = title,
                            bodyText = metaDescription ?: title,
                            sourceUrl = url,
                            mainImageUrl = mainImage,
                            clipMode = ClipMode.SCREENSHOT
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
