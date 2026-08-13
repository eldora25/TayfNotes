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

    /**
     * Unescapes JSON-encoded HTML string from evaluateJavascript (if still used as fallback)
     */
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
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }

    /**
     * Parses and cleans HTML content locally using Jsoup Safelist.
     */
    suspend fun parseAndCleanHtml(html: String, url: String): ScrapedContent = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.parse(html, url)
            val title = doc.title()
            
            // Find main content area
            val contentElement = doc.select("article").firstOrNull() 
                ?: doc.select("main").firstOrNull()
                ?: doc.select(".post-content, .entry-content, .article-body, #content, .content").firstOrNull()
                ?: doc.body()

            // Remove clutter before deep clean
            contentElement?.select("script, style, nav, footer, header, aside, .ads, .sidebar, .menu, .nav, .share, .cite, button, form")?.remove()

            // Deep clean to allow only safe tags and attributes, stripping inline styles that cause overlapping
            val cleanedHtml = Jsoup.clean(contentElement?.outerHtml() ?: "", url, Safelist.relaxed()
                .addAttributes("img", "src", "alt", "width", "height")
                .addTags("h1", "h2", "h3", "h4", "h5", "h6", "p", "br", "ul", "ol", "li", "b", "i", "strong", "em", "img", "blockquote", "code", "pre")
            )

            ScrapedContent(
                title = title.ifEmpty { url },
                content = cleanedHtml,
                plainText = contentElement?.text() ?: "",
                url = url
            )
        } catch (e: Exception) {
            ScrapedContent(title = "Hata", content = "İçerik işlenemedi: ${e.message}", url = url)
        }
    }

    fun wrapInReaderTheme(title: String, content: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: sans-serif; line-height: 1.6; padding: 16px; word-wrap: break-word; color: #333; background-color: #fcfcfc; }
                    h1 { border-bottom: 2px solid #eee; padding-bottom: 10px; color: #111; font-size: 1.4em; }
                    img { max-width: 100%; height: auto; border-radius: 8px; margin: 10px 0; display: block; }
                    pre { background: #f4f4f4; padding: 15px; overflow-x: auto; border-radius: 5px; font-size: 0.9em; white-space: pre-wrap; }
                    blockquote { border-left: 5px solid #ddd; padding-left: 15px; color: #666; font-style: italic; margin: 20px 0; }
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

    suspend fun saveClip(
        title: String,
        content: String,
        url: String,
        folderId: String?,
        tags: List<String>,
        description: String,
        type: NoteType = NoteType.WEB_CLIP
    ) {
        val isHtml = content.contains("<") && content.contains(">")
        val finalBody = if (isHtml) wrapInReaderTheme(title, content) else content
        val finalContent = if (description.isNotEmpty()) "$description\n\n$finalBody" else finalBody
        
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = title,
            content = finalContent,
            type = type,
            sourceUrl = url,
            folderId = folderId,
            tags = tags,
            createdAt = System.currentTimeMillis(),
            lastModified = System.currentTimeMillis()
        )
        noteRepository.insert(note)
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
