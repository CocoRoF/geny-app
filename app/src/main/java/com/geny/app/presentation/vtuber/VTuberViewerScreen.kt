package com.geny.app.presentation.vtuber

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.geny.app.core.ui.components.LoadingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VTuberViewerScreen(
    viewModel: VTuberViewerViewModel,
    onBack: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Detect keyboard open state
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val isKeyboardOpen = imeVisible

    // Push avatar state to WebView when it changes
    LaunchedEffect(uiState.avatarState) {
        val state = uiState.avatarState ?: return@LaunchedEffect
        val wv = webView ?: return@LaunchedEffect
        val js = "applyAvatarState(${state.expressionIndex ?: 0}, " +
                "'${state.motionGroup ?: "Idle"}', ${state.motionIndex ?: 0}, " +
                "'${state.trigger ?: "system"}')"
        wv.post { wv.evaluateJavascript(js, null) }
    }

    // Push thinking state to WebView
    LaunchedEffect(uiState.isThinking) {
        val wv = webView ?: return@LaunchedEffect
        if (uiState.isThinking) {
            wv.post { wv.evaluateJavascript("applyAvatarState(0, 'Thinking', 0, 'thinking')", null) }
        }
    }

    // Init model when WebView is ready and model info is available
    LaunchedEffect(uiState.webViewReady, uiState.currentModel) {
        if (!uiState.webViewReady) return@LaunchedEffect
        val model = uiState.currentModel ?: return@LaunchedEffect
        val wv = webView ?: return@LaunchedEffect
        val serverUrl = viewModel.serverUrl
        val js = "initLive2D('$serverUrl', '${model.url}', ${model.kScale}, '${model.idleMotionGroup}')"
        wv.post { wv.evaluateJavascript(js, null) }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (uiState.isLoading) {
            LoadingOverlay()
            return
        }

        // Layer 1: Live2D WebView (full-screen)
        AndroidView(
            factory = { context ->
                val frame = FrameLayout(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                @SuppressLint("SetJavaScriptEnabled")
                val wv = WebView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    @Suppress("DEPRECATION")
                    settings.allowUniversalAccessFromFileURLs = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    setBackgroundColor(android.graphics.Color.parseColor("#1a1a2e"))
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                            cm?.let {
                                if (it.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                    viewModel.onWebViewError(it.message())
                                }
                            }
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            viewModel.onWebViewReady()
                        }
                    }
                    addJavascriptInterface(
                        VTuberJsBridge(viewModel),
                        "AndroidBridge"
                    )
                    WebView.setWebContentsDebuggingEnabled(true)
                    loadUrl("file:///android_asset/live2d_viewer.html")
                }
                webView = wv
                frame.addView(wv)
                frame
            },
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Top bar (semi-transparent)
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(uiState.agentName ?: "VTuber")
                    if (uiState.isConnected) {
                        Icon(
                            Icons.Filled.Circle, null,
                            modifier = Modifier.padding(start = 8.dp).size(8.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer ?: onBack) {
                    Icon(Icons.Filled.Menu, "Menu")
                }
            },
            actions = {
                // Chat overlay toggle
                IconButton(onClick = viewModel::toggleChatOverlay) {
                    Icon(
                        if (uiState.chatOverlayVisible) Icons.Filled.ChatBubble
                        else Icons.Filled.ChatBubbleOutline,
                        if (uiState.chatOverlayVisible) "Hide Chat" else "Show Chat",
                        tint = if (uiState.chatOverlayVisible)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                // TTS toggle
                IconButton(onClick = viewModel::toggleTts) {
                    Icon(
                        if (uiState.ttsEnabled) Icons.AutoMirrored.Filled.VolumeUp
                        else Icons.AutoMirrored.Filled.VolumeOff,
                        if (uiState.ttsEnabled) "TTS On" else "TTS Off",
                        tint = if (uiState.ttsEnabled) Color(0xFF4CAF50)
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
        )

        // Layer 3: Chat overlay (above input, NOT affected by keyboard)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 64.dp) // space for the input row
        ) {
            // Chat messages overlay (collapsible)
            AnimatedVisibility(
                visible = uiState.chatOverlayVisible && uiState.messages.isNotEmpty() && !isKeyboardOpen,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                ChatOverlay(
                    messages = uiState.messages,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Thinking indicator
            AnimatedVisibility(
                visible = uiState.isThinking && !isKeyboardOpen,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                ThinkingIndicator(
                    agentName = uiState.agentName,
                    thinkingPreview = uiState.thinkingPreview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Layer 4: Chat input (moves with keyboard)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = uiState.chatMessage,
                onValueChange = viewModel::onChatMessageChanged,
                placeholder = { Text("Send a message...") },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                enabled = !uiState.isSending,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                )
            )

            IconButton(
                onClick = viewModel::sendChat,
                enabled = uiState.chatMessage.isNotBlank() && !uiState.isSending,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (uiState.chatMessage.isNotBlank()) 1f else 0.4f
                        )
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/** JavaScript interface for WebView -> Android communication */
class VTuberJsBridge(private val viewModel: VTuberViewerViewModel) {
    @JavascriptInterface
    fun onTap(hitArea: String, x: Float, y: Float) {
        viewModel.interact(hitArea, x, y)
    }

    @JavascriptInterface
    fun onModelLoaded() {
        viewModel.onModelLoaded()
    }

    @JavascriptInterface
    fun onError(message: String) {
        viewModel.onWebViewError(message)
    }
}
