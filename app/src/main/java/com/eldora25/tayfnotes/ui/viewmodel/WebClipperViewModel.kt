package com.eldora25.tayfnotes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eldora25.tayfnotes.data.repository.FolderRepository
import com.eldora25.tayfnotes.data.repository.NoteRepository
import com.eldora25.tayfnotes.shared.model.Folder
import com.eldora25.tayfnotes.shared.model.Note
import com.eldora25.tayfnotes.shared.model.NoteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BookmarkData(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val url: String
)

class WebClipperViewModel(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    init {
        viewModelScope.launch {
            folderRepository.allFolders.collect {
                _folders.value = it
            }
        }
    }

    fun unescapeHtml(html: String): String {
        if (html == "null") return ""
        return html.removePrefix("\"").removeSuffix("\"")
            .replace("\\u003C", "<")
            .replace("\\u003E", ">")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
    }

    /**
     * Helper to parse and clean HTML for LIVE PREVIEW (Reader Mode)
     */
    suspend fun parseAndCleanHtml(html: String, url: String): ScrapedContent = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.parse(html, url)
            val contentElement = doc.select("article, main, .post-content, .entry-content, .article-body, #content").firstOrNull() ?: doc.body()
            
            contentElement.select("script, style, nav, footer, header, aside, .ads, .sidebar, .menu, .nav, .share, .cite, button, form").remove()

            val cleanedHtml = Jsoup.clean(contentElement.outerHtml(), url, Safelist.relaxed()
                .addAttributes("img", "src", "alt", "width", "height")
                .addTags("h1", "h2", "h3", "h4", "h5", "h6", "p", "br", "ul", "ol", "li", "b", "i", "strong", "em", "img", "blockquote", "code", "pre")
            )

            ScrapedContent(
                title = doc.title().ifEmpty { "Web İçeriği" },
                content = cleanedHtml,
                plainText = contentElement.text(),
                url = url
            )
        } catch (e: Exception) {
            ScrapedContent(title = "Hata", content = "İçerik işlenemedi", url = url)
        }
    }

    /**
     * Internal processor for different clip modes to ensure NO overlap in logic.
     */
    suspend fun processAndSave(
        mode: String,
        rawData: String,
        url: String,
        folderId: String?,
        tags: List<String>,
        userDescription: String,
        providedTitle: String? = null,
        bookmarkJson: String? = null
    ) = withContext(Dispatchers.IO) {
        val doc = if (mode == "Article" || mode == "FullPage") Jsoup.parse(rawData, url) else null
        val pageTitle = providedTitle ?: doc?.title() ?: url

        val (finalContent, finalType) = when (mode) {
            "Bookmark" -> {
                Pair(bookmarkJson ?: "", NoteType.WEB_CLIP)
            }
            "FullPage" -> {
                Pair(rawData, NoteType.WEB_CLIP)
            }
            "Article" -> {
                val scraped = parseAndCleanHtml(rawData, url)
                Pair(wrapInReaderTheme(pageTitle, scraped.content), NoteType.WEB_CLIP)
            }
            "Simplified" -> {
                Pair(rawData, NoteType.TEXT) 
            }
            else -> Pair(rawData, NoteType.TEXT)
        }

        val noteContent = if (userDescription.isNotEmpty() && mode != "Bookmark") "$userDescription\n\n$finalContent" else finalContent
        
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = if (mode == "Simplified") "Basitleştirilmiş: $pageTitle" else pageTitle,
            content = noteContent,
            type = finalType,
            sourceUrl = url,
            folderId = folderId,
            tags = tags,
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
        noteRepository.insert(note)
    }

    fun wrapInReaderTheme(title: String, content: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: sans-serif; line-height: 1.6; padding: 20px; color: #2c3e50; background-color: #fcfcfc; }
                    h1 { border-bottom: 2px solid #ecf0f1; padding-bottom: 12px; color: #2c3e50; font-size: 1.5em; }
                    img { max-width: 100%; height: auto; border-radius: 10px; margin: 15px 0; display: block; }
                    pre { background: #f8f9fa; padding: 15px; overflow-x: auto; border-radius: 6px; font-size: 0.9em; border: 1px solid #e9ecef; white-space: pre-wrap; }
                    blockquote { border-left: 6px solid #3498db; padding-left: 20px; color: #7f8c8d; font-style: italic; margin: 25px 0; }
                    * { max-width: 100%; box-sizing: border-box; }
                </style>
            </head>
            <body>
                <h1>$title</h1>
                <div class="content">$content</div>
            </body>
            </html>
        """.trimIndent()
    }
}

class WebClipperViewModelFactory(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WebClipperViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WebClipperViewModel(noteRepository, folderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class ScrapedContent(
    val title: String,
    val content: String,
    val plainText: String = "",
    val url: String
)
