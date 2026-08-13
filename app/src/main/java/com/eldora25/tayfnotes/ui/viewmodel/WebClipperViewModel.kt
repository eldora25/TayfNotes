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
     * Unescapes JSON-encoded HTML string from evaluateJavascript
     */
    fun unescapeHtml(html: String): String {
        return html.removePrefix("\"").removeSuffix("\"")
            .replace("\\u003C", "<")
            .replace("\\u003E", ">")
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }

    /**
     * Parses HTML content locally using Jsoup.
     */
    suspend fun parseHtmlContent(html: String, url: String): ScrapedContent = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.parse(html, url)
            val title = doc.title()
            
            val articleElement = doc.select("article").firstOrNull() 
                ?: doc.select("main").firstOrNull()
                ?: doc.select(".post-content, .entry-content, .article-body, #content, .content").firstOrNull()
                ?: doc.body()

            val cleanElement = articleElement?.clone()
            cleanElement?.select("script, style, nav, footer, header, aside, .ads, .sidebar, .menu, .nav")?.remove()

            ScrapedContent(
                title = title.ifEmpty { url },
                content = cleanElement?.outerHtml() ?: "",
                plainText = cleanElement?.text() ?: "",
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
                    body { font-family: -apple-system, system-ui, sans-serif; line-height: 1.6; padding: 20px; color: #333; background-color: #fcfcfc; }
                    h1 { border-bottom: 2px solid #eee; padding-bottom: 10px; color: #111; font-size: 1.5em; }
                    img { max-width: 100%; height: auto; border-radius: 8px; margin: 10px 0; }
                    pre { background: #f4f4f4; padding: 15px; overflow-x: auto; border-radius: 5px; font-size: 0.9em; }
                    blockquote { border-left: 5px solid #ddd; padding-left: 15px; color: #666; font-style: italic; margin: 20px 0; }
                    a { color: #007AFF; text-decoration: none; }
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
        // Wrap content in theme if it's HTML to ensure good display later
        val finalHtml = if (content.contains("<") && content.contains(">")) {
            wrapInReaderTheme(title, content)
        } else content

        val finalContent = if (description.isNotEmpty()) "$description\n\n$finalHtml" else finalHtml
        
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
