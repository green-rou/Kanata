package com.greenrou.kanata.features.webplayer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.R
import com.greenrou.kanata.features.webplayer.content.StreamDetectedBanner
import com.greenrou.kanata.features.webplayer.content.WebPlayerGuide
import com.greenrou.kanata.features.webplayer.model.WebPlayerEvent
import org.koin.androidx.compose.koinViewModel


private val VIDEO_URL_REGEX = Regex("""\.(m3u8|mp4|mkv|webm)(\?.*)?$""", RegexOption.IGNORE_CASE)

private val JS_INJECTION = """
(function() {
  if (window.__kanataInjected) return;
  window.__kanataInjected = true;
  var reported = new Set();
  var notify = function(url) {
    try {
      if (/\.(m3u8|mp4|mkv|webm)(\?.*)?$/i.test(url) && !reported.has(url)) {
        reported.add(url);
        StreamBridge.onStream(url, location.href);
      }
    } catch(e) {}
  };
  var xhrOpen = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function(method, url) {
    try { notify(String(url)); } catch(e) {}
    return xhrOpen.apply(this, arguments);
  };
  var origFetch = window.fetch;
  if (origFetch) {
    window.fetch = function(input, init) {
      try {
        var url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
        notify(url);
      } catch(e) {}
      return origFetch.apply(this, arguments);
    };
  }
  new MutationObserver(function() {
    document.querySelectorAll('video[src]').forEach(function(v) {
      if (v.src && !v.src.startsWith('blob:')) notify(v.src);
    });
  }).observe(document.documentElement, {childList:true, subtree:true, attributes:true, attributeFilter:['src']});
})();
""".trimIndent()

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPlayerScreen(
    initialUrl: String = "",
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (streamUrl: String, referer: String) -> Unit,
    viewModel: WebPlayerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (initialUrl.isNotBlank()) {
            viewModel.handleEvent(WebPlayerEvent.UrlSubmitted(initialUrl))
        }
        viewModel.events.collect { event ->
            when (event) {
                is WebPlayerEvent.NavigateToPlayer -> {
                    onNavigateToPlayer(event.streamUrl, event.referer)
                }
                WebPlayerEvent.NavigateBack -> onNavigateBack()
                else -> Unit
            }
        }
    }

    val adBlockerEnabled = remember { java.util.concurrent.atomic.AtomicBoolean(true) }
    SideEffect { adBlockerEnabled.set(state.adBlockerEnabled) }

    val onStreamDetected: (String, String) -> Unit = remember(viewModel) {
        { url, referer ->
            val isAd = adBlockerEnabled.get() && AdBlocker.isAdStream(url)
            if (!isAd) {
                Handler(Looper.getMainLooper()).post {
                    viewModel.handleEvent(WebPlayerEvent.StreamDetected(url, referer))
                }
            }
        }
    }

    val onPageNavigated: (String) -> Unit = remember(viewModel) {
        { url -> viewModel.handleEvent(WebPlayerEvent.PageNavigated(url)) }
    }

    val onLoadingChanged: (Boolean) -> Unit = remember(viewModel) {
        { loading -> viewModel.handleEvent(WebPlayerEvent.LoadingChanged(loading)) }
    }

    val canGoBackState = remember { mutableStateOf(false) }
    val canGoBack by canGoBackState

    val webView = remember(context) {
        val reportedNetworkUrls = mutableSetOf<String>()

        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }

            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            addJavascriptInterface(
                StreamJsBridge(onStreamDetected),
                "StreamBridge",
            )

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    val url = request.url.toString()
                    if (adBlockerEnabled.get() && AdBlocker.shouldBlock(url)) {
                        return AdBlocker.emptyResponse()
                    }
                    if (VIDEO_URL_REGEX.containsMatchIn(url) && reportedNetworkUrls.add(url)) {
                        val referer = request.requestHeaders["Referer"].orEmpty()
                        Handler(Looper.getMainLooper()).post {
                            onStreamDetected(url, referer)
                        }
                    }
                    return null
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    reportedNetworkUrls.clear()
                    onLoadingChanged(true)
                    onPageNavigated(url)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    canGoBackState.value = view.canGoBack()
                    onLoadingChanged(false)
                    view.evaluateJavascript(JS_INJECTION, null)
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                    canGoBackState.value = view.canGoBack()
                    onPageNavigated(url)
                }

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    val url = view.url?.takeIf { it.isNotBlank() }
                    reportedNetworkUrls.clear()
                    Handler(Looper.getMainLooper()).post {
                        if (url != null) view.loadUrl(url) else view.reload()
                    }
                    return true
                }
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            val bundle = Bundle()
            webView.saveState(bundle)
            if (!bundle.isEmpty) viewModel.saveWebViewState(bundle)
            webView.destroy()
        }
    }

    LaunchedEffect(webView) {
        val saved = viewModel.consumeWebViewState()
        if (saved != null) {
            webView.restoreState(saved)
            canGoBackState.value = webView.canGoBack()
        } else if (state.hasNavigated && state.addressBarText.isNotBlank()) {
            webView.loadUrl(state.addressBarText)
        }
    }

    LaunchedEffect(state.urlToLoad) {
        val url = state.urlToLoad ?: return@LaunchedEffect
        webView.loadUrl(url)
        viewModel.handleEvent(WebPlayerEvent.UrlLoadDispatched)
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack()
        else viewModel.handleEvent(WebPlayerEvent.NavigateBack)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            if (state.webBackNavTopBar && webView.canGoBack()) webView.goBack()
                            else viewModel.handleEvent(WebPlayerEvent.NavigateBack)
                        }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                    title = {
                        TextField(
                            value = state.addressBarText,
                            onValueChange = { viewModel.handleEvent(WebPlayerEvent.AddressBarChanged(it)) },
                            placeholder = { androidx.compose.material3.Text(stringResource(R.string.webplayer_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go,
                            ),
                            keyboardActions = KeyboardActions(onGo = {
                                keyboard?.hide()
                                viewModel.handleEvent(WebPlayerEvent.UrlSubmitted(state.addressBarText))
                            }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    actions = {
                        if (state.hasNavigated) {
                            IconButton(onClick = {
                                viewModel.handleEvent(WebPlayerEvent.ShowSaveDialog)
                            }) {
                                Icon(
                                    Icons.Rounded.BookmarkAdd,
                                    contentDescription = stringResource(R.string.webplayer_save_page),
                                )
                            }
                        }
                        IconButton(onClick = { webView.reload() }) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.webplayer_cd_reload),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(),
                )
                if (state.isPageLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        strokeCap = StrokeCap.Round,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize(),
            )

            AnimatedVisibility(
                visible = !state.hasNavigated,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = slideOutVertically { it / 4 } + fadeOut(),
            ) {
                WebPlayerGuide(
                    adBlockerEnabled = state.adBlockerEnabled,
                    onDisableAdBlocker = { viewModel.handleEvent(WebPlayerEvent.DisableAdBlocker) },
                )
            }

            AnimatedVisibility(
                visible = !state.webBackNavTopBar && canGoBack,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = 16.dp),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                SmallFloatingActionButton(
                    onClick = { webView.goBack() },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            }

            AnimatedVisibility(
                visible = state.detectedStreamUrl != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                state.detectedStreamUrl?.let { url ->
                    StreamDetectedBanner(
                        streamUrl = url,
                        onOpenInPlayer = { viewModel.handleEvent(WebPlayerEvent.OpenInPlayer) },
                        onDismiss = { viewModel.handleEvent(WebPlayerEvent.DismissStream) },
                    )
                }
            }
        }
    }

    if (state.showSaveDialog) {
        var nameText by rememberSaveable { mutableStateOf(state.addressBarText.toSuggestedName()) }
        AlertDialog(
            onDismissRequest = { viewModel.handleEvent(WebPlayerEvent.DismissSaveDialog) },
            title = { Text(stringResource(R.string.webplayer_save_page_title)) },
            text = {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text(stringResource(R.string.webplayer_save_page_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = nameText.trim().ifBlank { state.addressBarText.toSuggestedName() }
                        viewModel.handleEvent(WebPlayerEvent.SavePage(name, state.addressBarText))
                    },
                    enabled = nameText.isNotBlank(),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleEvent(WebPlayerEvent.DismissSaveDialog) }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

private fun String.toSuggestedName(): String = runCatching {
    java.net.URI(this).host?.removePrefix("www.") ?: this
}.getOrDefault(this)

private class StreamJsBridge(private val onStream: (url: String, referer: String) -> Unit) {
    @JavascriptInterface
    fun onStream(url: String, referer: String) {
        Handler(Looper.getMainLooper()).post { onStream(url, referer) }
    }
}
