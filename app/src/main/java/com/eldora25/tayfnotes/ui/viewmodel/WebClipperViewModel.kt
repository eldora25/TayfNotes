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

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    private val referrer = "http://www.google.com"

    init {
        viewModelScope.launch {
            folderRepository.allFolders.collect {
                _folders.value = it
            }
        }
    }

    suspend fun scrapeArticle(url: String): ScrapedContent = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(referrer)
                .timeout(10000)
                .get()
            
            val title = doc.title()
            
            // Modern Readability approach: Extract main content
            val articleElement = doc.select("article").firstOrNull() 
                ?: doc.select("main").firstOrNull()
                ?: doc.select(".post-content, .entry-content, .article-body, #content, .content").firstOrNull()
                ?: doc.body()

            // Clone to avoid modifying the original if needed, but here we just want cleaned output
            val cleanElement = articleElement?.clone()
            cleanElement?.select("script, style, nav, footer, header, aside, .ads, .sidebar, .menu, .nav")?.remove()

            val htmlContent = cleanElement?.outerHtml() ?: ""
            val textContent = cleanElement?.text() ?: ""

            ScrapedContent(
                title = title,
                content = htmlContent,
                plainText = textContent,
                url = url
            )
        } catch (e: Exception) {
            ScrapedContent(title = "Hata", content = "İçerik alınamadı: ${e.message}", url = url)
        }
    }

    fun wrapInReaderTheme(title: String, content: String): String {
        return """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { 
                        font-family: -apple-system, sans-serif; 
                        line-height: 1.6; 
                        padding: 20px; 
                        color: #333; 
                        background-color: #f9f9f9;
                    }
                    h1 { border-bottom: 2px solid #eee; padding-bottom: 10px; color: #1a1a1a; }
                    img { max-width: 100%; height: auto; border-radius: 8px; }
                    pre { background: #eee; padding: 10px; overflow-x: auto; }
                    blockquote { border-left: 4px solid #ccc; padding-left: 15px; margin-left: 0; font-style: italic; }
                </style>
            </head>
            <body>
                <h1>$title</h1>
                $content
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
        val finalContent = if (description.isNotEmpty()) "$description\n\n$content" else content
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
