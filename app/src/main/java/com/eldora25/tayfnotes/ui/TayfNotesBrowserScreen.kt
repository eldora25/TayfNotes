package com.eldora25.tayfnotes.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import com.eldora25.tayfnotes.BuildConfig
import com.eldora25.tayfnotes.ui.components.WebClipperPanel
import com.eldora25.tayfnotes.ui.viewmodel.WebClipperViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Class defined outside to ensure @JavascriptInterface detection
class HtmlOutInterface(private val onHtml: (String) -> Unit, private val onBookmark: (String) -> Unit) {
    @JavascriptInterface
    fun processHTML(html: String) {
        onHtml(html)
    }

    @JavascriptInterface
    fun processBookmark(json: String) {
        onBookmark(json)
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TayfNotesBrowserScreen(
    viewModel: WebClipperViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    BackHandler { onBack() }

    var currentUrl by rememberSaveable { mutableStateOf("https://www.google.com") }
    var urlInput by rememberSaveable { mutableStateOf(currentUrl) }
    var isLoading by remember { mutableStateOf(false) }
    var showClipper by rememberSaveable { mutableStateOf(false) }
    
    // Active State Management
    var activeMode by rememberSaveable { mutableStateOf("FullPage") } 
    var readerModeHtml by rememberSaveable { mutableStateOf<String?>(null) }
    
    var capturedData by remember { mutableStateOf<String?>(null) }
    var bookmarkJson by remember { mutableStateOf<String?>(null) }

    val webViewState = remember { Bundle() }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // JS Bridge
    val jsInterface = remember { 
        HtmlOutInterface(
            onHtml = { capturedData = it },
            onBookmark = { bookmarkJson = it }
        ) 
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (!isLandscape) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, scrollBehavior.state.heightOffset.roundToInt()) }
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "TayfNotes Dahili Tarayıcı V.01.${BuildConfig.BUILD_NO}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                            IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Kapat")
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                readerModeHtml = null
                                urlInput = "https://www.google.com"
                                currentUrl = urlInput 
                                activeMode = "FullPage"
                            }) { Icon(Icons.Default.Home, null) }
                            
                            IconButton(onClick = { if (readerModeHtml != null) readerModeHtml = null else webViewRef?.goBack() }, enabled = readerModeHtml != null || webViewRef?.canGoBack() == true) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { webViewRef?.goForward() }, enabled = webViewRef?.canGoForward() == true) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { if (readerModeHtml != null) readerModeHtml = null; webViewRef?.reload() }) { Icon(Icons.Default.Refresh, null) }
                            
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
                                    activeMode = "FullPage"
                                    currentUrl = target
                                    focusManager.clearFocus()
                                })
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            PremiumButton("Tam Sayfa", Icons.Default.Fullscreen, isSelected = activeMode == "FullPage") {
                                activeMode = "FullPage"
                                readerModeHtml = null
                            }
                            PremiumButton("Makale", Icons.AutoMirrored.Filled.Article, isSelected = activeMode == "Article") {
                                activeMode = "Article"
                                webViewRef?.evaluateJavascript("(function() { window.HtmlViewer.processHTML(document.documentElement.outerHTML); })();", null)
                                scope.launch {
                                    isLoading = true
                                    delay(700) // Wait for bridge transfer
                                    capturedData?.let { html ->
                                        readerModeHtml = viewModel.getCleanedHtmlForPreview(html, currentUrl)
                                    }
                                    isLoading = false
                                }
                            }
                            PremiumButton("Basitleştir", Icons.Default.TextFields, isSelected = activeMode == "Simplified") {
                                activeMode = "Simplified"
                                webViewRef?.evaluateJavascript("(function() { window.HtmlViewer.processHTML(document.body.innerText || document.body.textContent); })();", null)
                                scope.launch {
                                    isLoading = true
                                    delay(500)
                                    capturedData?.let { text ->
                                        readerModeHtml = viewModel.wrapInReaderTheme("Okuma Modu", "<p>${text.replace("\n", "<br>")}</p>")
                                    }
                                    isLoading = false
                                }
                            }
                            PremiumButton("Yer İzi", Icons.Default.Bookmark, isSelected = activeMode == "Bookmark") {
                                activeMode = "Bookmark"
                                readerModeHtml = null
                            }
                            
                            PremiumButton("Kırp (Clipper)", Icons.Default.ContentCut, isMain = true) {
                                when (activeMode) {
                                    "Bookmark" -> {
                                        val js = "(function() { var data = { title: document.title, description: document.querySelector('meta[name=\"description\"]')?.content || \"\", imageUrl: document.querySelector('meta[property=\"og:image\"]')?.content || \"\", url: window.location.href }; window.HtmlViewer.processBookmark(JSON.stringify(data)); })();"
                                        webViewRef?.evaluateJavascript(js, null)
                                    }
                                    "Simplified" -> {
                                        webViewRef?.evaluateJavascript("(function() { window.HtmlViewer.processHTML(document.body.innerText || document.body.textContent); })();", null)
                                    }
                                    else -> {
                                        webViewRef?.evaluateJavascript("(function() { window.HtmlViewer.processHTML(document.documentElement.outerHTML); })();", null)
                                    }
                                }
                                scope.launch { delay(800); showClipper = true }
                            }
                        }
                        if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView(
                factory = { context ->
                    NestedScrollingWebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        
                        addJavascriptInterface(jsInterface, "HtmlViewer")

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                if (readerModeHtml == null) isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let { if (readerModeHtml == null) { urlInput = it; currentUrl = it } }
                            }
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString()
                                if (url != null && url != currentUrl) {
                                    view?.loadUrl(url)
                                    return true
                                }
                                return false
                            }
                        }
                        
                        setOnTouchListener { v, _ ->
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
                        if (view.tag != html) {
                            view.loadDataWithBaseURL(currentUrl, html, "text/html", "UTF-8", null)
                            view.tag = html
                        }
                    } else {
                        if (view.url != currentUrl) {
                            view.loadUrl(currentUrl)
                        }
                        view.tag = null
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
                    initialMode = activeMode,
                    viewModel = viewModel,
                    capturedRawData = capturedData,
                    bookmarkDataJson = bookmarkJson,
                    onDismiss = { showClipper = false }
                )
            }
        }
    }
}

@Composable
fun PremiumButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isMain: Boolean = false, isSelected: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(2.dp)) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(if (isMain) 44.dp else 36.dp),
            colors = if (isSelected || isMain) IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
            ) else IconButtonDefaults.filledTonalIconButtonColors()
        ) { Icon(icon, contentDescription = text, modifier = Modifier.size(if (isMain) 22.dp else 18.dp)) }
        Text(
            text = text, 
            fontSize = 8.sp, 
            fontWeight = if (isMain || isSelected) FontWeight.Bold else FontWeight.Normal, 
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified
        )
    }
}

class NestedScrollingWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) : WebView(context, attrs, defStyleAttr), NestedScrollingChild3 {

    private val childHelper = NestedScrollingChildHelper(this)
    private var lastY = 0

    init {
        isNestedScrollingEnabled = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val y = event.y.toInt()
        val result = super.onTouchEvent(event)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = y
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = lastY - y
                dispatchNestedPreScroll(0, dy, null, null, ViewCompat.TYPE_TOUCH)
                dispatchNestedScroll(0, 0, 0, dy, null, ViewCompat.TYPE_TOUCH)
                lastY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopNestedScroll(ViewCompat.TYPE_TOUCH)
            }
        }
        return result
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean = childHelper.startNestedScroll(axes, type)
    override fun stopNestedScroll(type: Int) = childHelper.stopNestedScroll(type)
    override fun hasNestedScrollingParent(type: Int): Boolean = childHelper.hasNestedScrollingParent(type)
    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?, type: Int, consumed: IntArray) =
        childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed)
    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?, type: Int): Boolean =
        childHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type)
    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?, type: Int): Boolean =
        childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
}
