package com.eldora25.tayfnotes.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.eldora25.tayfnotes.BuildConfig
import com.eldora25.tayfnotes.ui.components.WebClipperPanel
import com.eldora25.tayfnotes.ui.viewmodel.WebClipperViewModel
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TayfNotesBrowserScreen(
    viewModel: WebClipperViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    // UI State saved across configuration changes
    var currentUrl by rememberSaveable { mutableStateOf("https://www.google.com") }
    var urlInput by rememberSaveable { mutableStateOf(currentUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var showClipper by rememberSaveable { mutableStateOf(false) }
    var readerModeHtml by rememberSaveable { mutableStateOf<String?>(null) }
    
    val webViewState = remember { Bundle() }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TayfNotes Dahili Tarayıcı V.01.${BuildConfig.BUILD_NO}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                    IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                // Browser Controls
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { 
                        readerModeHtml = null
                        urlInput = "https://www.google.com"
                        currentUrl = urlInput 
                    }) { Icon(Icons.Default.Home, contentDescription = "Home") }
                    
                    IconButton(onClick = { 
                        if (readerModeHtml != null) readerModeHtml = null else webViewRef?.goBack() 
                    }, enabled = readerModeHtml != null || webViewRef?.canGoBack() == true) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", modifier = Modifier.size(20.dp))
                    }
                    
                    IconButton(onClick = { webViewRef?.goForward() }, enabled = webViewRef?.canGoForward() == true) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "İleri", modifier = Modifier.size(20.dp))
                    }
                    
                    IconButton(onClick = { 
                        if (readerModeHtml != null) readerModeHtml = null
                        webViewRef?.reload() 
                    }) { Icon(Icons.Default.Refresh, contentDescription = "Yenile") }
                    
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            var target = urlInput
                            if (!target.startsWith("http")) target = "https://$target"
                            readerModeHtml = null
                            currentUrl = target
                            focusManager.clearFocus()
                        })
                    )
                }

                // Premium Action Buttons
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PremiumButton("Tam Sayfa", Icons.Default.Fullscreen, isSelected = readerModeHtml == null) {
                        readerModeHtml = null
                    }
                    PremiumButton("Makale", Icons.AutoMirrored.Filled.Article, isSelected = readerModeHtml != null) {
                        webViewRef?.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { html ->
                            scope.launch {
                                isLoading = true
                                val unescapedHtml = html.removePrefix("\"").removeSuffix("\"").replace("\\u003C", "<").replace("\\\"", "\"").replace("\\\\", "\\")
                                val result = viewModel.parseHtmlContent(unescapedHtml, currentUrl)
                                readerModeHtml = viewModel.wrapInReaderTheme(result.title, result.content)
                                isLoading = false
                            }
                        }
                    }
                    PremiumButton("Basitleştir", Icons.Default.TextFields) {
                        webViewRef?.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { html ->
                            scope.launch {
                                isLoading = true
                                val unescapedHtml = html.removePrefix("\"").removeSuffix("\"").replace("\\u003C", "<").replace("\\\"", "\"")
                                val result = viewModel.parseHtmlContent(unescapedHtml, currentUrl)
                                val plainHtml = "<p>${result.plainText.replace("\n", "<br>")}</p>"
                                readerModeHtml = viewModel.wrapInReaderTheme(result.title, plainHtml)
                                isLoading = false
                            }
                        }
                    }
                    PremiumButton("Kırp (Clipper)", Icons.Default.ContentCut, isMain = true) {
                        showClipper = true
                    }
                }
                if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
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
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                if (readerModeHtml == null) isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let { if (readerModeHtml == null) { urlInput = it; currentUrl = it } }
                            }
                        }
                        
                        // Prevent Navigation Drawer from intercepting touch events
                        setOnTouchListener { v, event ->
                            v.parent.requestDisallowInterceptTouchEvent(true)
                            false
                        }

                        if (!webViewState.isEmpty) restoreState(webViewState)
                    }
                },
                update = { view ->
                    webViewRef = view
                    val html = readerModeHtml
                    if (html != null) {
                        view.loadDataWithBaseURL(currentUrl, html, "text/html", "UTF-8", null)
                    } else if (view.url != currentUrl) {
                        view.loadUrl(currentUrl)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onRelease = { it.saveState(webViewState) }
            )
        }

        if (showClipper) {
            ModalBottomSheet(
                onDismissRequest = { showClipper = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                WebClipperPanel(
                    url = currentUrl,
                    initialMode = "Article",
                    viewModel = viewModel,
                    onDismiss = { showClipper = false }
                )
            }
        }
    }
}

@Composable
fun PremiumButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isMain: Boolean = false, isSelected: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(if (isMain) 56.dp else 48.dp),
            colors = if (isSelected || isMain) IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) else IconButtonDefaults.filledTonalIconButtonColors()
        ) { Icon(icon, contentDescription = text, modifier = Modifier.size(if (isMain) 28.dp else 24.dp)) }
        Text(text, fontSize = 10.sp, fontWeight = if (isMain || isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified)
    }
}
