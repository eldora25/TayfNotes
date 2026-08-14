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

    /**
     * Internal processor for different clip modes.
     * Ensures large data from Bridge is handled and cleaned correctly.
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
                // Return full HTML as received from Bridge
                Pair(rawData, NoteType.WEB_CLIP)
            }
            "Article" -> {
                val contentElement = doc?.select("article, main, .post-content, .entry-content, .article-body, #content")?.firstOrNull() ?: doc?.body()
                
                // Deep cleaning using Jsoup Safelist to remove all clutter and script overlapping
                val cleanedHtml = Jsoup.clean(contentElement?.outerHtml() ?: "", url, Safelist.relaxed()
                    .addAttributes("img", "src", "alt", "width", "height")
                    .addTags("h1", "h2", "h3", "h4", "h5", "h6", "p", "br", "ul", "ol", "li", "b", "i", "strong", "em", "img", "blockquote", "code", "pre")
                )
                Pair(wrapInReaderTheme(pageTitle, cleanedHtml), NoteType.WEB_CLIP)
            }
            "Simplified" -> {
                // Pure plain text mode, no HTML processing
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
                    body { font-family: -apple-system, sans-serif; line-height: 1.6; padding: 20px; color: #333; background-color: #fdfdfd; }
                    h1 { border-bottom: 2px solid #eee; padding-bottom: 10px; color: #111; font-size: 1.4em; }
                    img { max-width: 100%; height: auto; border-radius: 8px; margin: 15px 0; display: block; }
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

    // Helper for Live Reader mode in Browser
    suspend fun getCleanedHtmlForPreview(html: String, url: String): String {
        val doc = Jsoup.parse(html, url)
        val content = doc.select("article, main, .content").firstOrNull() ?: doc.body()
        val cleaned = Jsoup.clean(content.outerHtml(), url, Safelist.relaxed())
        return wrapInReaderTheme(doc.title(), cleaned)
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
