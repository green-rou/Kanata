package com.greenrou.kanata.features.webplayer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.R
import com.greenrou.kanata.features.webplayer.content.StreamDetectedBanner
import com.greenrou.kanata.features.webplayer.content.WebPlayerGuide
import com.greenrou.kanata.features.webplayer.model.WebPlayerEvent
import org.koin.androidx.compose.koinViewModel

private val VIDEO_URL_REGEX = Regex("""\.(m3u8|mp4|mkv|webm|ts)(\?.*)?$""", RegexOption.IGNORE_CASE)

private val JS_INJECTION = """
(function() {
  if (window.__kanataInjected) return;
  window.__kanataInjected = true;
  var notify = function(url) {
    try {
      if (/\.(m3u8|mp4|mkv|webm|ts)(\?.*)?$/i.test(url)) {
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
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (streamUrl: String, referer: String) -> Unit,
    viewModel: WebPlayerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WebPlayerEvent.NavigateToPlayer -> onNavigateToPlayer(event.streamUrl, event.referer)
                WebPlayerEvent.NavigateBack -> onNavigateBack()
                else -> Unit
            }
        }
    }

    val onStreamDetected: (String, String) -> Unit = remember(viewModel) {
        { url, referer ->
            Handler(Looper.getMainLooper()).post {
                viewModel.handleEvent(WebPlayerEvent.StreamDetected(url, referer))
            }
        }
    }

    val onPageNavigated: (String) -> Unit = remember(viewModel) {
        { url -> viewModel.handleEvent(WebPlayerEvent.PageNavigated(url)) }
    }

    val onLoadingChanged: (Boolean) -> Unit = remember(viewModel) {
        { loading -> viewModel.handleEvent(WebPlayerEvent.LoadingChanged(loading)) }
    }

    val webView = remember(context) {
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
                    if (VIDEO_URL_REGEX.containsMatchIn(url)) {
                        Handler(Looper.getMainLooper()).post {
                            onStreamDetected(url, request.requestHeaders["Referer"].orEmpty())
                        }
                    }
                    return null
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    onLoadingChanged(true)
                    onPageNavigated(url)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    onLoadingChanged(false)
                    view.evaluateJavascript(JS_INJECTION, null)
                }

                override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                    onPageNavigated(url)
                }
            }
        }
    }

    DisposableEffect(webView) {
        onDispose { webView.destroy() }
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
                            if (webView.canGoBack()) webView.goBack()
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
                WebPlayerGuide()
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
}

private class StreamJsBridge(private val onStream: (url: String, referer: String) -> Unit) {
    @JavascriptInterface
    fun onStream(url: String, referer: String) {
        Handler(Looper.getMainLooper()).post { onStream(url, referer) }
    }
}
