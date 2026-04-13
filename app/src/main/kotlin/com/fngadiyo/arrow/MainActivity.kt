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
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.android.billingclient.api.*

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var statusBarSpacer: View
    private lateinit var adView: AdView
    private var interstitialAd: InterstitialAd? = null
    private lateinit var billingClient: BillingClient
    private var isAdFree = false
    private val vibrator: Vibrator by lazy { getSystemService(Vibrator::class.java) }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusBarSpacer = findViewById(R.id.statusBarSpacer)
        webView = findViewById(R.id.webView)
        adView = findViewById(R.id.adView)

        // Load purchase status
        val prefs = getSharedPreferences("arrow_prefs", MODE_PRIVATE)
        isAdFree = prefs.getBoolean("ad_free", false)

        // Initialize Mobile Ads SDK
        val testDeviceIds = listOf(AdRequest.DEVICE_ID_EMULATOR, "INSERT_YOUR_TEST_DEVICE_ID_HERE")
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)
        MobileAds.initialize(this) { }
        
        if (isAdFree) {
            adView.visibility = View.GONE
        } else {
            // Load Banner Ad
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            // Pre-load Interstitial
            loadInterstitial()
        }

        initializeBillingClient()

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

    private fun showInterstitial() {
        if (isAdFree) return
        runOnUiThread {
            interstitialAd?.let { ad ->
                ad.show(this)
                interstitialAd = null
                loadInterstitial() 
            } ?: run {
                loadInterstitial()
            }
        }
    }

    private fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, "ca-app-pub-6838652186707908/9821819196", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
            }
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                interstitialAd = null
            }
        })
    }

    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.products.contains("remove_ads") && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        applyAdFreeStatus()
                    }
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    applyAdFreeStatus()
                }
            }
        }
    }

    private fun applyAdFreeStatus() {
        isAdFree = true
        getSharedPreferences("arrow_prefs", MODE_PRIVATE).edit().putBoolean("ad_free", true).apply()
        runOnUiThread {
            adView.visibility = View.GONE
            webView.evaluateJavascript("window.onAdsRemoved && window.onAdsRemoved();", null)
        }
    }

    private fun launchBillingFlow() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("remove_ads")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetailsList[0])
                        .build()
                )
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                billingClient.launchBillingFlow(this, billingFlowParams)
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

        @JavascriptInterface
        fun showInterstitial() {
            this@MainActivity.showInterstitial()
        }

        @JavascriptInterface
        fun removeAds() {
            launchBillingFlow()
        }
    }
}
