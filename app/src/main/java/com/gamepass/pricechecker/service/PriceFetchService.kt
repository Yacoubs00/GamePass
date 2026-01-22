package com.gamepass.pricechecker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.webkit.WebSettings
import android.widget.FrameLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ead.lib.cloudflare_bypass.BypassClient
import com.gamepass.pricechecker.R
import com.gamepass.pricechecker.data.CachedPrice
import com.gamepass.pricechecker.data.PriceDatabase
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * ForegroundService that uses offscreen WebView with Cloudflare bypass
 * to scrape prices from JS-rendered and protected sites.
 * 
 * Key features:
 * - Offscreen WebView (1x1 pixel, not visible)
 * - Cloudflare-Bypass library for 403/JS challenges
 * - Human-like delays (5-30 seconds between requests)
 * - Parallel fetching (up to 3 WebViews)
 * - Results cached to Room DB
 * - Broadcasts results as they arrive
 */
class PriceFetchService : Service() {

    companion object {
        private const val TAG = "PriceFetchService"
        const val CHANNEL_ID = "price_fetch_channel"
        
        // Actions
        const val ACTION_FETCH_PRICES = "com.gamepass.pricechecker.FETCH_PRICES"
        const val ACTION_PRICE_RESULT = "com.gamepass.pricechecker.PRICE_RESULT"
        const val ACTION_FETCH_COMPLETE = "com.gamepass.pricechecker.FETCH_COMPLETE"
        const val ACTION_FETCH_PROGRESS = "com.gamepass.pricechecker.FETCH_PROGRESS"
        
        // Extras
        const val EXTRA_SITE_LIST = "SITE_LIST"
        const val EXTRA_DEALS = "DEALS"
        const val EXTRA_SITE_NAME = "SITE_NAME"
        const val EXTRA_PROGRESS = "PROGRESS"
        const val EXTRA_TOTAL = "TOTAL"
        
        // Configuration
        private const val PARALLEL_WEBVIEWS = 3
        private const val PAGE_LOAD_TIMEOUT_MS = 25000L
        private const val JS_RENDER_DELAY_MS = 3500L
        private const val MIN_DELAY_BETWEEN_SITES_MS = 5000L
        private const val MAX_DELAY_BETWEEN_SITES_MS = 15000L
        
        // Modern User Agent
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        
        /**
         * Start the service to fetch prices from specified sites
         */
        fun startFetch(context: Context, sites: List<String>? = null) {
            val intent = Intent(context, PriceFetchService::class.java).apply {
                action = ACTION_FETCH_PRICES
                sites?.let { putStringArrayListExtra(EXTRA_SITE_LIST, ArrayList(it)) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    /**
     * Site configuration with URL and extraction JavaScript
     */
    data class SiteConfig(
        val name: String,
        val url: String,
        val extractionJs: String,
        val needsBypass: Boolean = true
    )
    
    // All supported sites with their extraction logic
    private val siteConfigs = mapOf(
        "CDKeys" to SiteConfig(
            name = "CDKeys",
            url = "https://www.cdkeys.com/xbox-live/subscriptions?q=game+pass+ultimate",
            needsBypass = false,  // Works without bypass
            extractionJs = """
                (function() {
                    var products = [];
                    document.querySelectorAll('.product-item, .product-card, [class*="product"]').forEach(function(item) {
                        var link = item.querySelector('a[href*="game-pass"]');
                        var priceEl = item.querySelector('.price, [data-price-amount], .special-price');
                        if (link && priceEl) {
                            var price = priceEl.getAttribute('data-price-amount') || priceEl.innerText.replace(/[^0-9.]/g, '');
                            if (parseFloat(price) > 0 && parseFloat(price) < 200) {
                                products.push({
                                    title: link.innerText.trim().substring(0, 100) || 'Xbox Game Pass Ultimate',
                                    price: parseFloat(price),
                                    url: link.href,
                                    currency: 'USD'
                                });
                            }
                        }
                    });
                    return JSON.stringify(products.slice(0, 15));
                })();
            """.trimIndent()
        ),
        
        "Eneba" to SiteConfig(
            name = "Eneba",
            url = "https://www.eneba.com/store/xbox?text=xbox%20game%20pass%20ultimate",
            needsBypass = true,
            extractionJs = """
                (function() {
                    var products = [];
                    document.querySelectorAll('[class*="product"], [class*="card"], [data-testid*="product"]').forEach(function(item) {
                        var link = item.querySelector('a[href*="game-pass"], a[href*="xbox"]');
                        var priceEl = item.querySelector('[class*="price"], [class*="Price"], [data-testid*="price"]');
                        var titleEl = item.querySelector('[class*="title"], [class*="name"], h3, h4');
                        if (priceEl) {
                            var priceText = priceEl.innerText.replace(',', '.').replace(/[^0-9.]/g, '');
                            var price = parseFloat(priceText);
                            if (price > 0 && price < 200) {
                                products.push({
                                    title: titleEl ? titleEl.innerText.trim().substring(0, 100) : 'Game Pass Ultimate',
                                    price: price,
                                    url: link ? link.href : window.location.href,
                                    currency: 'EUR'
                                });
                            }
                        }
                    });
                    return JSON.stringify(products.slice(0, 15));
                })();
            """.trimIndent()
        ),
        
        "G2A" to SiteConfig(
            name = "G2A",
            url = "https://www.g2a.com/search?query=xbox%20game%20pass%20ultimate",
            needsBypass = true,
            extractionJs = """
                (function() {
                    var products = [];
                    document.querySelectorAll('[class*="product"], [data-locator*="product"], [class*="ProductCard"]').forEach(function(item) {
                        var link = item.querySelector('a[href*="game-pass"], a[href*="xbox"]');
                        var priceEl = item.querySelector('[class*="price"], [data-locator*="price"]');
                        var titleEl = item.querySelector('[class*="title"], [data-locator*="title"]');
                        if (priceEl) {
                            var priceText = priceEl.innerText.replace(',', '.').replace(/[^0-9.]/g, '');
                            var price = parseFloat(priceText);
                            if (price > 0 && price < 200) {
                                products.push({
                                    title: titleEl ? titleEl.innerText.trim().substring(0, 100) : 'Game Pass Ultimate',
                                    price: price,
                                    url: link ? link.href : window.location.href,
                                    currency: 'EUR'
                                });
                            }
                        }
                    });
                    return JSON.stringify(products.slice(0, 15));
                })();
            """.trimIndent()
        ),
        
        "Instant Gaming" to SiteConfig(
            name = "Instant Gaming",
            url = "https://www.instant-gaming.com/en/search/?q=xbox+game+pass+ultimate",
            needsBypass = true,
            extractionJs = """
                (function() {
                    var products = [];
                    document.querySelectorAll('.item, .product, [class*="search-result"]').forEach(function(item) {
                        var link = item.querySelector('a[href*="game-pass"], a');
                        var priceEl = item.querySelector('.price, .discount, [class*="price"]');
                        var titleEl = item.querySelector('.title, .name, [class*="title"]');
                        if (link && priceEl) {
                            var priceText = priceEl.innerText.replace(',', '.').replace(/[^0-9.]/g, '');
                            var price = parseFloat(priceText);
                            if (price > 0 && price < 200) {
                                products.push({
                                    title: titleEl ? titleEl.innerText.trim().substring(0, 100) : link.innerText.trim().substring(0, 100),
                                    price: price,
                                    url: link.href.startsWith('http') ? link.href : 'https://www.instant-gaming.com' + link.getAttribute('href'),
                                    currency: 'EUR'
                                });
                            }
                        }
                    });
                    return JSON.stringify(products.slice(0, 15));
                })();
            """.trimIndent()
        ),
        
        "K4G" to SiteConfig(
            name = "K4G",
            url = "https://k4g.com/?s=xbox+game+pass+ultimate",
            needsBypass = true,
            extractionJs = """
                (function() {
                    var products = [];
                    document.querySelectorAll('.product, [class*="product"], article').forEach(function(item) {
                        var link = item.querySelector('a[href*="game-pass"], a[href*="product"]');
                        var priceEl = item.querySelector('.price, [class*="price"], .amount');
                        var titleEl = item.querySelector('.title, h2, h3, [class*="title"]');
                        if (link && priceEl) {
                            var priceText = priceEl.innerText.replace(',', '.').replace(/[^0-9.]/g, '');
                            var price = parseFloat(priceText);
                            if (price > 0 && price < 200) {
                                products.push({
                                    title: titleEl ? titleEl.innerText.trim().substring(0, 100) : 'Game Pass Ultimate',
                                    price: price,
                                    url: link.href,
                                    currency: 'USD'
                                });
                            }
                        }
                    });
                    return JSON.stringify(products.slice(0, 15));
                })();
            """.trimIndent()
        ),
        
        "AllKeyShop" to SiteConfig(
            name = "AllKeyShop",
            url = "https://www.allkeyshop.com/blog/buy-xbox-game-pass-ultimate-cd-key-compare-prices/",
            needsBypass = false,  // Usually works without bypass
            extractionJs = """
                (function() {
                    var products = [];
                    // AllKeyShop has offers in specific structure
                    document.querySelectorAll('a.recomended_offers, .offers-table tr, [class*="offer"]').forEach(function(item) {
                        var priceText = item.innerText;
                        var match = priceText.match(/(\d+[.,]\d+)\s*[€$]/);
                        if (match) {
                            var price = parseFloat(match[1].replace(',', '.'));
                            var link = item.tagName === 'A' ? item.href : (item.querySelector('a') ? item.querySelector('a').href : '');
                            if (price > 0 && price < 200 && link) {
                                // Extract merchant name
                                var merchantMatch = priceText.match(/([A-Z][a-z]+(?:[A-Z][a-z]+)*)/);
                                var merchant = merchantMatch ? merchantMatch[1] : 'AllKeyShop';
                                products.push({
                                    title: 'Game Pass Ultimate - ' + merchant,
                                    price: price,
                                    url: link,
                                    currency: 'EUR'
                                });
                            }
                        }
                    });
                    return JSON.stringify(products.slice(0, 20));
                })();
            """.trimIndent()
        )
    )

    // Service state
    private var handler: Handler? = null
    private val webViews = mutableListOf<WebView>()
    private val activeJobs = mutableMapOf<String, Job>()
    private var sitesToFetch = mutableListOf<String>()
    private val currentSiteIndex = AtomicInteger(0)
    private val completedSites = AtomicInteger(0)
    private val allResults = mutableListOf<CachedPrice>()
    private val gson = Gson()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isRunning = false
    
    private lateinit var database: PriceDatabase
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        handler = Handler(Looper.getMainLooper())
        database = PriceDatabase.getInstance(applicationContext)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_FETCH_PRICES -> {
                if (isRunning) {
                    Log.w(TAG, "Service already running, ignoring request")
                    return START_NOT_STICKY
                }
                
                // Start foreground immediately
                startForeground(1, buildNotification("Initializing..."))
                isRunning = true
                
                // Get sites to fetch (default: all configured sites)
                val requestedSites = intent.getStringArrayListExtra(EXTRA_SITE_LIST)
                sitesToFetch.clear()
                sitesToFetch.addAll(
                    requestedSites?.filter { siteConfigs.containsKey(it) }
                        ?: siteConfigs.keys.toList()
                )
                
                // Ensure AllKeyShop is last (it's an aggregator, slower)
                if (sitesToFetch.remove("AllKeyShop")) {
                    sitesToFetch.add("AllKeyShop")
                }
                
                Log.d(TAG, "Starting fetch for ${sitesToFetch.size} sites: $sitesToFetch")
                
                currentSiteIndex.set(0)
                completedSites.set(0)
                allResults.clear()
                
                // Start parallel fetching
                startParallelFetching()
            }
        }
        
        return START_NOT_STICKY
    }
    
    /**
     * Start multiple WebView fetches in parallel
     */
    private fun startParallelFetching() {
        repeat(minOf(PARALLEL_WEBVIEWS, sitesToFetch.size)) {
            fetchNextSite()
        }
    }
    
    /**
     * Fetch the next site in queue
     */
    private fun fetchNextSite() {
        val index = currentSiteIndex.getAndIncrement()
        
        if (index >= sitesToFetch.size) {
            // Check if all done
            checkCompletion()
            return
        }
        
        val siteName = sitesToFetch[index]
        val config = siteConfigs[siteName]
        
        if (config == null) {
            Log.w(TAG, "No config for site: $siteName")
            completedSites.incrementAndGet()
            fetchNextSite()
            return
        }
        
        Log.d(TAG, "Starting fetch for: $siteName (${index + 1}/${sitesToFetch.size})")
        updateNotification("Fetching $siteName (${completedSites.get() + 1}/${sitesToFetch.size})...")
        broadcastProgress(completedSites.get(), sitesToFetch.size, siteName)
        
        // Add human-like delay before starting (except first batch)
        val delay = if (index < PARALLEL_WEBVIEWS) 0L else Random.nextLong(MIN_DELAY_BETWEEN_SITES_MS, MAX_DELAY_BETWEEN_SITES_MS)
        
        handler?.postDelayed({
            if (isRunning) {
                createWebViewAndFetch(siteName, config)
            }
        }, delay)
    }
    
    /**
     * Create an offscreen WebView and load the site
     */
    private fun createWebViewAndFetch(siteName: String, config: SiteConfig) {
        try {
            val webView = WebView(applicationContext).apply {
                // Offscreen - 1x1 pixel, not visible
                layoutParams = FrameLayout.LayoutParams(1, 1)
                
                // Configure WebView settings
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    userAgentString = USER_AGENT
                    cacheMode = WebSettings.LOAD_DEFAULT
                    
                    // Additional settings for better JS support
                    setSupportMultipleWindows(false)
                    javaScriptCanOpenWindowsAutomatically = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    
                    // Allow mixed content
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                }
                
                // Use Cloudflare Bypass client
                webViewClient = object : BypassClient() {
                    private var hasExtracted = false
                    
                    override fun onPageFinishedByPassed(view: WebView?, url: String?) {
                        super.onPageFinishedByPassed(view, url)
                        Log.d(TAG, "Page finished (bypassed): $siteName - $url")
                        
                        if (hasExtracted) return
                        hasExtracted = true
                        
                        // Wait for JS to render content, then extract
                        handler?.postDelayed({
                            extractPricesFromWebView(view, siteName, config)
                        }, JS_RENDER_DELAY_MS)
                    }
                }
            }
            
            // Track this WebView
            synchronized(webViews) {
                webViews.add(webView)
            }
            
            // Load the URL
            Log.d(TAG, "Loading URL: ${config.url}")
            webView.loadUrl(config.url)
            
            // Set timeout
            handler?.postDelayed({
                handleTimeout(siteName, webView)
            }, PAGE_LOAD_TIMEOUT_MS)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating WebView for $siteName", e)
            onSiteComplete(siteName, emptyList())
        }
    }

    /**
     * Extract prices from WebView using JavaScript
     */
    private fun extractPricesFromWebView(webView: WebView?, siteName: String, config: SiteConfig) {
        if (webView == null) {
            Log.w(TAG, "WebView is null for $siteName")
            onSiteComplete(siteName, emptyList())
            return
        }
        
        Log.d(TAG, "Extracting prices from $siteName")
        
        webView.evaluateJavascript(config.extractionJs) { result ->
            try {
                // Clean the result (remove escaping)
                val cleanResult = result
                    ?.trim('"')
                    ?.replace("\\\"", "\"")
                    ?.replace("\\n", "\n")
                    ?.replace("\\\\", "\\")
                    ?: "[]"
                
                Log.d(TAG, "Extraction result for $siteName: ${cleanResult.take(200)}")
                
                // Parse products
                val products = parseProducts(siteName, cleanResult)
                Log.d(TAG, "Parsed ${products.size} products from $siteName")
                
                onSiteComplete(siteName, products)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing extraction result for $siteName", e)
                onSiteComplete(siteName, emptyList())
            }
            
            // Clean up WebView
            cleanupWebView(webView)
        }
    }
    
    /**
     * Parse products from JSON result
     */
    private fun parseProducts(siteName: String, jsonResult: String): List<CachedPrice> {
        val products = mutableListOf<CachedPrice>()
        
        try {
            @Suppress("UNCHECKED_CAST")
            val jsonArray = gson.fromJson(jsonResult, Array::class.java) as? Array<Map<String, Any>> ?: emptyArray()
            
            for (item in jsonArray) {
                val title = item["title"]?.toString() ?: "Game Pass Ultimate"
                val price = (item["price"] as? Number)?.toDouble() ?: continue
                val url = item["url"]?.toString() ?: continue
                val currency = item["currency"]?.toString() ?: "USD"
                
                if (price <= 0 || price >= 200) continue
                
                // Detect region from title
                val region = detectRegion(title, url)
                
                // Detect duration from title
                val duration = detectDuration(title)
                
                products.add(CachedPrice(
                    id = "${siteName}_${region}_${duration}_${price}",
                    sellerName = siteName,
                    price = price,
                    currency = currency,
                    region = region,
                    duration = duration,
                    productUrl = url,
                    productTitle = title,
                    sourceType = "WEBVIEW"
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON for $siteName", e)
        }
        
        return products.distinctBy { "${it.price}_${it.productUrl}" }
    }
    
    /**
     * Detect region from product title or URL
     */
    private fun detectRegion(title: String, url: String): String {
        val text = "$title $url".lowercase()
        return when {
            "uae" in text || "emirates" in text -> "UAE"
            "turkey" in text || "tr " in text || "/tr" in text -> "TURKEY"
            "argentina" in text || " ar " in text || "/ar" in text -> "ARGENTINA"
            "brazil" in text || "brasil" in text || " br " in text -> "BRAZIL"
            "usa" in text || "united states" in text -> "US"
            "europe" in text || " eu " in text || "/eu" in text -> "EU"
            "uk " in text || "united kingdom" in text || "gbp" in text -> "UK"
            "global" in text -> "GLOBAL"
            else -> "GLOBAL"
        }
    }
    
    /**
     * Detect subscription duration from title
     */
    private fun detectDuration(title: String): String {
        val text = title.lowercase()
        return when {
            "12 month" in text || "1 year" in text || "annual" in text -> "12_MONTHS"
            "6 month" in text -> "6_MONTHS"
            "3 month" in text -> "3_MONTHS"
            "2 month" in text -> "2_MONTHS"
            "14 day" in text || "2 week" in text -> "14_DAYS"
            "7 day" in text || "1 week" in text || "trial" in text -> "7_DAYS"
            else -> "1_MONTH"
        }
    }
    
    /**
     * Handle page load timeout
     */
    private fun handleTimeout(siteName: String, webView: WebView) {
        synchronized(webViews) {
            if (webViews.contains(webView)) {
                Log.w(TAG, "Timeout for $siteName")
                cleanupWebView(webView)
                onSiteComplete(siteName, emptyList())
            }
        }
    }
    
    /**
     * Clean up a WebView
     */
    private fun cleanupWebView(webView: WebView) {
        synchronized(webViews) {
            webViews.remove(webView)
        }
        handler?.post {
            try {
                webView.stopLoading()
                webView.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying WebView", e)
            }
        }
    }
    
    /**
     * Called when a site fetch completes (success or failure)
     */
    private fun onSiteComplete(siteName: String, products: List<CachedPrice>) {
        val completed = completedSites.incrementAndGet()
        
        Log.d(TAG, "Site complete: $siteName with ${products.size} products ($completed/${sitesToFetch.size})")
        
        if (products.isNotEmpty()) {
            // Add to results
            synchronized(allResults) {
                allResults.addAll(products)
            }
            
            // Save to database
            serviceScope.launch(Dispatchers.IO) {
                try {
                    database.priceDao().insertPrices(products)
                    Log.d(TAG, "Saved ${products.size} prices to database")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving to database", e)
                }
            }
            
            // Broadcast results
            broadcastResults(siteName, products)
        }
        
        // Update progress
        broadcastProgress(completed, sitesToFetch.size, siteName)
        
        // Fetch next site
        fetchNextSite()
        
        // Check if all done
        checkCompletion()
    }
    
    /**
     * Check if all sites are done
     */
    private fun checkCompletion() {
        if (completedSites.get() >= sitesToFetch.size) {
            Log.d(TAG, "All sites complete! Total results: ${allResults.size}")
            
            // Broadcast completion
            broadcastComplete()
            
            // Stop service
            handler?.postDelayed({
                stopSelf()
            }, 1000)
        }
    }

    // ==================== Broadcasts ====================
    
    private fun broadcastResults(siteName: String, products: List<CachedPrice>) {
        val intent = Intent(ACTION_PRICE_RESULT).apply {
            putExtra(EXTRA_SITE_NAME, siteName)
            putExtra(EXTRA_DEALS, gson.toJson(products))
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    private fun broadcastProgress(completed: Int, total: Int, currentSite: String) {
        val intent = Intent(ACTION_FETCH_PROGRESS).apply {
            putExtra(EXTRA_PROGRESS, completed)
            putExtra(EXTRA_TOTAL, total)
            putExtra(EXTRA_SITE_NAME, currentSite)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    private fun broadcastComplete() {
        val intent = Intent(ACTION_FETCH_COMPLETE).apply {
            putExtra(EXTRA_DEALS, gson.toJson(allResults))
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    // ==================== Notifications ====================
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Price Checker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background price checking"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
    
    private fun buildNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        
        return builder
            .setContentTitle("Game Pass Price Checker")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)?.notify(1, buildNotification(text))
    }
    
    // ==================== Lifecycle ====================
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        isRunning = false
        
        // Cancel all coroutines
        serviceScope.cancel()
        
        // Clean up all WebViews
        synchronized(webViews) {
            webViews.forEach { webView ->
                try {
                    webView.stopLoading()
                    webView.destroy()
                } catch (e: Exception) {
                    Log.e(TAG, "Error destroying WebView", e)
                }
            }
            webViews.clear()
        }
        
        // Clear handler
        handler?.removeCallbacksAndMessages(null)
        handler = null
        
        super.onDestroy()
    }
}
