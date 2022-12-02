package com.example.allvideodownloader

import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.DelicateCoroutinesApi
import org.jsoup.Jsoup

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

        AndroidView(
            modifier = Modifier.padding(it),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d("WebViewScreen", "onPageFinished: $url")
                            evaluateJavascript(
                                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML.toString() +'</html>'); })();"
                            ) { html ->
                                Log.d("WebViewScreen", "onPageFinished: $html")

                                val html1 = html.replace("\\u003C", "<")
                                    .replace("\\u003E", ">")

//                                getUrlsFromHtml(StringEscapeUtils.unescapeJava(html1))
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            Log.d("WebViewScreen", "onProgressChanged: $newProgress")
                        }

                        override fun getVideoLoadingProgressView(): View? {
                            return super.getVideoLoadingProgressView()
                        }

                        override fun getDefaultVideoPoster(): Bitmap? {
                            val view = super.getDefaultVideoPoster()
                            Log.i("WebViewScreen", "getDefaultVideoPoster: ")
                            return view
                        }
                    }

                    settings.javaScriptEnabled = true
//                    GlobalScope.launch(Dispatchers.IO) {
//                        while (true) {
//                            Log.i("WebViewScreen", "WebViewScreen: ${view?.visibility}")
//                        }
//                    }
                    loadUrl("https://www.youtube.com/")
                }
            },
//            update = { webView ->
//            }
        )
    }
}

private fun getUrlsFromHtml(html: String): List<String> {
    val urls = mutableListOf<String>()
    val doc = Jsoup.parse(html)
    Log.i("WebViewScreen", "getUrlsFromHtml: $doc")
    val elements = doc.select("a[href]")
    for (element in elements) {
        val url = element.attr("abs:href")
        urls.add(url)
    }
    Log.i("WebViewScreen", "getUrlsFromHtml: $urls")
    return urls
}
