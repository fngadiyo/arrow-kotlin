package com.fngadiyo.arrow

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var statusBarSpacer: View
    private val vibrator: Vibrator by lazy { getSystemService(Vibrator::class.java) }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusBarSpacer = findViewById(R.id.statusBarSpacer)
        webView = findViewById(R.id.webView)

        // Initial status bar setup
        updateSystemBars("light")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val statusInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val cutoutInset = insets.displayCutout?.safeInsetTop ?: 0
            val totalTopInset = maxOf(statusInset, cutoutInset)

            val params = statusBarSpacer.layoutParams as LinearLayout.LayoutParams
            params.height = totalTopInset
            statusBarSpacer.layoutParams = params
            insets
        }
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
        }

        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }

        webView.loadUrl("file:///android_asset/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("window.handleAndroidBack && window.handleAndroidBack();", null)
            }
        })
    }

    private fun updateSystemBars(theme: String) {
        runOnUiThread {
            val isDark = theme == "dark"
            val bgColor = if (isDark) {
                ContextCompat.getColor(this, android.R.color.black)
            } else {
                ContextCompat.getColor(this, android.R.color.white)
            }
            
            window.statusBarColor = bgColor
            statusBarSpacer.setBackgroundColor(bgColor)
            
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !isDark
            }
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun setTheme(theme: String) {
            updateSystemBars(theme)
        }

        @JavascriptInterface
        fun vibrate(pattern: String) {
            try {
                val timings = pattern.split(",").map { it.trim().toLong() }.toLongArray()
                vibrator?.let {
                    it.vibrate(VibrationEffect.createWaveform(timings, -1))
                }
            } catch (e: Exception) {
                android.util.Log.e("ArrowEntangled", "Haptic vibrate failed: ${e.message}")
            }
        }

        @JavascriptInterface
        fun vibrateOneShot(durationMs: Long, amplitude: Int) {
            try {
                vibrator?.let {
                    it.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                }
            } catch (e: Exception) {
                android.util.Log.e("ArrowEntangled", "Haptic vibrateOneShot failed: ${e.message}")
            }
        }

        @JavascriptInterface
        fun exitApp() {
            finish()
        }
    }
}
