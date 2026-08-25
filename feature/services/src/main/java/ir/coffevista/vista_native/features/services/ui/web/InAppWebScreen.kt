package ir.coffevista.vista_native.features.services.ui.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import ir.coffevista.vista_native.core.designsystem.theme.VistaFontFamily
import ir.coffevista.vista_native.core.designsystem.tokens.VistaBrandColors

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppWebScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
    restrictHost: String? = null,
    appBarColor: Color? = null,
    appBarForegroundColor: Color? = null,
    useBackButton: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val barBg = appBarColor ?: MaterialTheme.colorScheme.background
    val fgColor = appBarForegroundColor ?: MaterialTheme.colorScheme.onBackground

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onBack()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = barBg,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            if (title.isNotBlank()) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = VistaFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = fgColor,
                                    ),
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    if (webViewInstance?.canGoBack() == true) {
                                        webViewInstance?.goBack()
                                    } else {
                                        onBack()
                                    }
                                },
                            ) {
                                val imageVector = if (useBackButton) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Close
                                Icon(
                                    imageVector = imageVector,
                                    contentDescription = "بازگشت",
                                    tint = fgColor,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = barBg,
                        ),
                    )
                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { loadingProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = if (appBarColor != null) Color.White.copy(alpha = 0.5f) else VistaBrandColors.Indigo,
                            trackColor = Color.Transparent,
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (loadError == null) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadingProgress = newProgress / 100f
                                    isLoading = newProgress < 100
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val reqUrl = request?.url?.toString() ?: return false
                                    if (restrictHost != null) {
                                        val host = request.url.host.orEmpty()
                                        if (!host.equals(restrictHost, ignoreCase = true)) {
                                            return true // Block external navigation
                                        }
                                    }
                                    return false
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?,
                                ) {
                                    handler?.cancel()
                                    isLoading = false
                                    loadError = "اتصال امن به این سرویس برقرار نشد. لطفاً بعداً دوباره تلاش کنید."
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?,
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        loadError = "این سرویس در حال حاضر در دسترس نیست."
                                    }
                                }
                            }

                            loadUrl(url)
                            webViewInstance = this
                        }
                    },
                        update = { view ->
                            webViewInstance = view
                        },
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = loadError.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = VistaFontFamily,
                                color = fgColor,
                            ),
                        )
                    }
                }
            }
        }
    }
}
