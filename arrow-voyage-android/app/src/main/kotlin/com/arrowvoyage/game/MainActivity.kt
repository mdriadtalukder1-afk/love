package com.arrowvoyage.game

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader

/** Full-screen WebView that serves the game from assets/www over https://appassets.androidplatform.net so storage works. Ads come from [Ads]. */
class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private lateinit var ads: Ads

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        web = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#0b0f1f"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.textZoom = 100
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            val loader = WebViewAssetLoader.Builder()
                .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this@MainActivity))
                .build()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    loader.shouldInterceptRequest(request.url)
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
            }
        }
        // the bridge must be added before the page loads so window.ArrowAds exists when the game script runs
        ads = Ads(this, web)
        web.addJavascriptInterface(ads, "ArrowAds")
        web.loadUrl("https://appassets.androidplatform.net/www/index.html")
        setContentView(web)
        ads.start()

        // Android back: close the game's own overlay or go home inside the page; leave the app only from the home screen.
        onBackPressedDispatcher.addCallback(this) {
            web.evaluateJavascript("(function(){var o=document.getElementById('overlay');if(o&&!o.hidden){o.hidden=true;return 'o'}var b=document.getElementById('backBtn');if(b&&!document.getElementById('game').hidden){b.click();return 'g'}return 'x'})()") { r ->
                if (r == "\"x\"") { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        web.onResume()
    }

    override fun onPause() { web.onPause(); super.onPause() }

    private fun hideSystemBars() {
        val c = WindowInsetsControllerCompat(window, web)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c.hide(WindowInsetsCompat.Type.systemBars())
    }
}
