package com.eldora25.tayfnotes.ui

import android.annotation.SuppressLint
import android.widget.Toast
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalWebBrowserScreen(
    initialUrl: String = "https://google.com",
    onBack: () -> Unit,
    onClipContent: (String, String, String) -> Unit // title, url, extractedText
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var currentTitle by remember { mutableStateOf("Yükleniyor...") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(currentTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(currentUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (webViewRef?.canGoBack() == true) {
                            webViewRef?.goBack()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    Toast.makeText(context, "İçerik kırpılıyor...", Toast.LENGTH_SHORT).show()
                    // JavaScript Enjeksiyonu: Seçili metin varsa onu al, yoksa ana makaleyi veya body'yi al
                    val jsCode = """
                        (function() {
                            var selection = window.getSelection().toString();
                            if (selection.length > 0) {
                                AndroidClipper.processContent(document.title, window.location.href, selection);
                                return;
                            }
                            var article = document.querySelector('article, main, .post-content, .entry-content');
                            var content = article ? article.innerText : document.body.innerText;
                            AndroidClipper.processContent(document.title, window.location.href, content);
                        })();
                    """.trimIndent()

                    webViewRef?.evaluateJavascript(jsCode, null)
                },
                icon = { Icon(Icons.Default.ContentCut, contentDescription = "Kırp") },
                text = { Text("Bunu Kırp") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(50)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.databaseEnabled = true
                        
                        // WebViewClient ile sayfa içi gezinmeyi kendi içinde tutuyoruz
                        webViewClient = object : WebViewClient() {
                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                url?.let { view?.loadUrl(it) }
                                return true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                currentUrl = url ?: ""
                                currentTitle = view?.title ?: ""
                                
                                // Inject CSS for better selection visibility
                                val css = "::selection { background: #FFEB3B !important; color: black !important; }"
                                val js = "var style = document.createElement('style'); style.innerHTML = '$css'; document.head.appendChild(style);"
                                view?.evaluateJavascript(js, null)
                            }
                        }

                        // JavaScript'ten Android/Kotlin tarafına veri aktarmak için Interface köprüsü
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun processContent(title: String, url: String, text: String) {
                                // Bu fonksiyon arka plan (JS) thread'inde çalışır, UI'a veri yolluyoruz
                                onClipContent(title, url, text)
                            }
                        }, "AndroidClipper")
                    }
                },
                update = { view ->
                    webViewRef = view
                    if (view.url == null) {
                        view.loadUrl(initialUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
