package com.gamepass.pricechecker.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebView-based scraper for Cloudflare-protected sites.
 * Uses a real browser engine to execute JavaScript challenges automatically.
 * 
 * This is the legitimate way to access Cloudflare-protected pages:
 * - Uses a real browser (WebView) instead of HTTP client
 * - Executes JavaScript challenges naturally
 * - Handles Turnstile/CAPTCHA automatically in most cases
 * - Mimics real user behavior
 */
class WebViewScraper(private val context: Context) {

    companion object {
        // Timeout for page load (includes Cloudflare challenge time)
        private const val PAGE_LOAD_TIMEOUT = 30000L
        
        // Extra wait time after page loads to ensure JS completes
        private const val JS_COMPLETION_DELAY = 3000L
        
        // Sites that need WebView scraping (Cloudflare protected)
        val CLOUDFLARE_SITES = listOf(
            "kinguin.net",
            "gamivo.com",
            "gg.deals",
            "hrkgame.com",
            "2game.com",
            "play-asia.com",
            "gamestop.com"
        )
        
        // Check if URL needs WebView
        fun needsWebView(url: String): Boolean {
            return CLOUDFLARE_SITES.any { url.contains(it, ignoreCase = true) }
        }
    }

    private var webView: WebView? = null

    /**
     * Initialize WebView with stealth settings
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun createStealthWebView(): WebView {
        return WebView(context).apply {
            settings.apply {
                // Enable JavaScript (required for Cloudflare)
                javaScriptEnabled = true
                
                // Enable DOM storage
                domStorageEnabled = true
                
                // Set realistic user agent (Android Chrome)
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36"
                
                // Enable caching
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Allow mixed content
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                
                // Additional settings for better compatibility
                loadWithOverviewMode = true
                useWideViewPort = true
                
                // Enable database
                databaseEnabled = true
                
                // Allow file access
                allowFileAccess = false
                allowContentAccess = false
            }
            
            // Accept cookies (important for Cloudflare)
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(this@apply, true)
            }
        }
    }

    /**
     * Fetch page content using WebView
     * Handles Cloudflare JavaScript challenges automatically
     */
    suspend fun fetchPage(url: String): WebViewResult {
        return withTimeout(PAGE_LOAD_TIMEOUT + JS_COMPLETION_DELAY + 5000) {
            suspendCancellableCoroutine { continuation ->
                Handler(Looper.getMainLooper()).post {
                    try {
                        val wv = createStealthWebView()
                        webView = wv

                        var isCompleted = false
                        var lastUrl = url

                        wv.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                // Allow redirects (Cloudflare may redirect)
                                request?.url?.let { lastUrl = it.toString() }
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (isCompleted) return
                                
                                // Wait for JS to complete (Cloudflare challenge)
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (isCompleted) return@postDelayed
                                    
                                    // Extract page content
                                    wv.evaluateJavascript(
                                        "(function() { return document.documentElement.outerHTML; })();"
                                    ) { html ->
                                        if (!isCompleted) {
                                            isCompleted = true
                                            val cleanHtml = html
                                                ?.removePrefix("\"")
                                                ?.removeSuffix("\"")
                                                ?.replace("\\u003C", "<")
                                                ?.replace("\\u003E", ">")
                                                ?.replace("\\\"", "\"")
                                                ?.replace("\\n", "\n")
                                                ?: ""
                                            
                                            // Check if still on Cloudflare challenge page
                                            val isChallengePage = cleanHtml.contains("cf-challenge") ||
                                                    cleanHtml.contains("challenge-running") ||
                                                    cleanHtml.contains("Checking your browser")
                                            
                                            cleanup()
                                            
                                            if (isChallengePage) {
                                                continuation.resume(WebViewResult.ChallengeDetected(lastUrl))
                                            } else {
                                                continuation.resume(WebViewResult.Success(cleanHtml, lastUrl))
                                            }
                                        }
                                    }
                                }, JS_COMPLETION_DELAY)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                if (!isCompleted) {
                                    isCompleted = true
                                    cleanup()
                                    continuation.resume(
                                        WebViewResult.Error("Error $errorCode: $description")
                                    )
                                }
                            }
                        }

                        // Load the URL
                        wv.loadUrl(url)

                        // Set timeout
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!isCompleted) {
                                isCompleted = true
                                cleanup()
                                continuation.resume(WebViewResult.Timeout)
                            }
                        }, PAGE_LOAD_TIMEOUT + JS_COMPLETION_DELAY)

                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }

                continuation.invokeOnCancellation {
                    Handler(Looper.getMainLooper()).post { cleanup() }
                }
            }
        }
    }

    /**
     * Extract specific element from page using JavaScript
     */
    suspend fun extractElement(url: String, jsSelector: String): String? {
        val result = fetchPage(url)
        return when (result) {
            is WebViewResult.Success -> {
                // Parse the HTML and extract using jsoup or regex
                result.html
            }
            else -> null
        }
    }

    private fun cleanup() {
        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        webView = null
    }
}

/**
 * Result of WebView fetch operation
 */
sealed class WebViewResult {
    data class Success(val html: String, val finalUrl: String) : WebViewResult()
    data class Error(val message: String) : WebViewResult()
    data class ChallengeDetected(val url: String) : WebViewResult()
    object Timeout : WebViewResult()
}

/**
 * Configuration for site-specific scraping
 */
object SiteConfigs {
    
    data class SiteConfig(
        val domain: String,
        val searchUrl: String,
        val priceSelector: String,
        val titleSelector: String,
        val linkSelector: String,
        val needsWebView: Boolean = false,
        val extraWaitMs: Long = 0
    )
    
    val configs = mapOf(
        "kinguin" to SiteConfig(
            domain = "kinguin.net",
            searchUrl = "https://www.kinguin.net/listing?phrase=xbox+game+pass+ultimate",
            priceSelector = "[data-product-price]",
            titleSelector = "[data-product-name]",
            linkSelector = "a[href*='/product/']",
            needsWebView = true,
            extraWaitMs = 2000
        ),
        "gamivo" to SiteConfig(
            domain = "gamivo.com",
            searchUrl = "https://www.gamivo.com/search/xbox%20game%20pass%20ultimate",
            priceSelector = ".product-card__price",
            titleSelector = ".product-card__title",
            linkSelector = "a.product-card",
            needsWebView = true,
            extraWaitMs = 2000
        ),
        "ggdeals" to SiteConfig(
            domain = "gg.deals",
            searchUrl = "https://gg.deals/game/xbox-game-pass-ultimate/",
            priceSelector = ".price-inner",
            titleSelector = ".shop-name",
            linkSelector = "a.shop-link",
            needsWebView = true,
            extraWaitMs = 3000
        ),
        "hrkgame" to SiteConfig(
            domain = "hrkgame.com",
            searchUrl = "https://www.hrkgame.com/en/search/?q=game+pass",
            priceSelector = ".product-price",
            titleSelector = ".product-name",
            linkSelector = "a.product-link",
            needsWebView = true,
            extraWaitMs = 2000
        ),
        "2game" to SiteConfig(
            domain = "2game.com",
            searchUrl = "https://2game.com/search?query=game+pass",
            priceSelector = ".price",
            titleSelector = ".product-title",
            linkSelector = "a.product-link",
            needsWebView = true,
            extraWaitMs = 2000
        ),
        "playasia" to SiteConfig(
            domain = "play-asia.com",
            searchUrl = "https://www.play-asia.com/search/game+pass",
            priceSelector = ".price",
            titleSelector = ".title",
            linkSelector = "a.product-link",
            needsWebView = true,
            extraWaitMs = 2000
        ),
        "gamestop" to SiteConfig(
            domain = "gamestop.com",
            searchUrl = "https://www.gamestop.com/search/?q=xbox+game+pass",
            priceSelector = ".price",
            titleSelector = ".product-name",
            linkSelector = "a.product-link",
            needsWebView = true,
            extraWaitMs = 2000
        )
    )
    
    fun getConfig(url: String): SiteConfig? {
        return configs.values.find { url.contains(it.domain, ignoreCase = true) }
    }
}
