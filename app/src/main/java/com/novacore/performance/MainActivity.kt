package com.novacore.performance

import android.app.Activity
import android.os.Bundle
import android.view.Window
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var bridge: NovaBridge
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); requestWindowFeature(Window.FEATURE_NO_TITLE)
        web = WebView(this).apply { settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.mediaPlaybackRequiresUserGesture = false; settings.allowFileAccess = false; settings.allowContentAccess = true; webViewClient = WebViewClient(); webChromeClient = WebChromeClient() }
        bridge = NovaBridge(this, web); web.addJavascriptInterface(bridge, "NovaBridge"); setContentView(web); web.loadUrl("file:///android_asset/web/index.html")
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() { if (web.canGoBack()) web.goBack() else finish() } })
    }
    override fun onResume() { super.onResume(); bridge.resume() }
    override fun onPause() { bridge.pause(); super.onPause() }
    override fun onDestroy() { web.destroy(); super.onDestroy() }
}
