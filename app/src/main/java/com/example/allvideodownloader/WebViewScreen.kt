package com.example.allvideodownloader

import android.R
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.DelicateCoroutinesApi
import org.jsoup.Jsoup
import kotlin.math.log


@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun WebViewScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "All Video Downloader") },
                navigationIcon = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        }
    ) {
        val webview: WebView? = null
        AndroidView(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    addJavascriptInterface(JavaScriptInterface(context), "HTMLOUT")

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
//                            Log.d("WebViewScreen", "onPageFinished: $url")
//                            view?.loadUrl(
//                                "javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');"
//                            )
                        }

//                        override fun shouldOverrideUrlLoading(
//                            view: WebView?,
//                            request: WebResourceRequest?
//                        ): Boolean {
//                            Log.d("WebViewScreen", "shouldOverrideUrlLoading: ${request?.url}")
//                            view?.loadUrl(request?.url.toString())
//                            return super.shouldOverrideUrlLoading(view, request)
//                        }

                        override fun onLoadResource(view: WebView?, url: String?) {
                            super.onLoadResource(view, url)
                            Log.d("WebViewScreen", "onLoadResource: $url")
                        }

//                        override fun doUpdateVisitedHistory(
//                            view: WebView?,
//                            url: String?,
//                            isReload: Boolean
//                        ) {
//                            super.doUpdateVisitedHistory(view, url, isReload)
//                        }
                    }
                    settings.apply {
                        javaScriptEnabled = true
//                        loadWithOverviewMode = true
//                        useWideViewPort = true
//                        allowFileAccess = true
//                        allowContentAccess = true
//                        domStorageEnabled = true;
//                        loadWithOverviewMode = true
                    }

                    loadUrl("https://www.youtube.com/")
                }
            },
            update = { webView ->
//                webView.loadUrl("https://tiktok.com")
            }
        )
    }
}

class JavaScriptInterface(context: Context) {
    private val context: Context = context

    @android.webkit.JavascriptInterface
    fun processHTML(html: String) {
        getUrlsFromHtml(html)
//        Log.d("WebViewScreen", "processHTML: $html")
        AlertDialog.Builder(context).setMessage(html).setPositiveButton(R.string.ok, null)
            .setCancelable(false).create().show()
    }
}

private fun getUrlsFromHtml(html: String?): List<String> {
    val urls = mutableListOf<String>()
    val doc = Jsoup.parse(html)
    Log.i("WebViewScreen", "getUrlsFromHtml: $doc")
    val elements = doc.select("a[href]")
    for (element in elements) {
        Log.i("WebViewScreen", "getUrlsFromHtml: ${element.attr("href")}")
        val url = element.attr("abs:href")
        urls.add(url)
    }
    Log.i("WebViewScreen", "getUrlsFromHtml: $urls")
    return urls
}
