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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ead.lib.cloudflare_bypass.BypassClient
import com.gamepass.pricechecker.R
import com.gamepass.pricechecker.models.PriceDeal
import com.gamepass.pricechecker.models.Region
import com.gamepass.pricechecker.models.DealType
import com.gamepass.pricechecker.models.Duration
import com.gamepass.pricechecker.models.TrustLevel
import com.google.gson.Gson
import org.jsoup.Jsoup
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Background service that uses WebView with Cloudflare bypass to scrape
 * prices from protected sites (G2A, Kinguin, Gamivo, etc.)
 */
class PriceFetchService : Service() {
    
    companion object {
        const val CHANNEL_ID = "price_fetch_channel"
        const val ACTION_FETCH_PRICES = "com.gamepass.pricechecker.FETCH_PRICES"
        const val ACTION_PRICE_RESULT = "com.gamepass.pricechecker.PRICE_RESULT"
        const val ACTION_FETCH_COMPLETE = "com.gamepass.pricechecker.FETCH_COMPLETE"
        const val EXTRA_SITE_URL = "SITE_URL"
        const val EXTRA_SITE_NAME = "SITE_NAME"
        const val EXTRA_SITE_LIST = "SITE_LIST"
        const val EXTRA_DEALS = "DEALS"
        const val EXTRA_SITE_COMPLETED = "SITE_COMPLETED"
        
        // Sites that need Cloudflare bypass
        val CLOUDFLARE_SITES = mapOf(
            "G2A" to CloudflareSite(
                url = "https://www.g2a.com/search?query=xbox+game+pass+ultimate",
                priceSelector = ".sc-iqAclL, .price, [data-locator='zth-price']",
                titleSelector = ".sc-cAhXWc, .title, [data-locator='zth-product-title']",
                linkSelector = "a[href*='game-pass']"
            ),
            "Kinguin" to CloudflareSite(
                url = "https://www.kinguin.net/listing?phrase=xbox+game+pass+ultimate",
                priceSelector = ".price, .product-price, [class*='price']",
                titleSelector = ".product-name, .title, [class*='title']",
                linkSelector = "a[href*='game-pass']"
            ),
            "Gamivo" to CloudflareSite(
                url = "https://www.gamivo.com/search/xbox+game+pass+ultimate",
                priceSelector = ".price, .product-price, [class*='price']",
                titleSelector = ".product-name, .title",
                linkSelector = "a[href*='game-pass']"
            ),
            "GG.deals" to CloudflareSite(
                url = "https://gg.deals/eu/game/xbox-game-pass-ultimate/",
                priceSelector = ".price-inner, .game-price, [class*='price']",
                titleSelector = ".game-name, .title",
                linkSelector = "a[href*='game-pass']"
            ),
            "HRK Game" to CloudflareSite(
                url = "https://www.hrkgame.com/en/search/?query=game+pass",
                priceSelector = ".price, .product-price",
                titleSelector = ".product-name, .title",
                linkSelector = "a[href*='game-pass']"
            ),
            "2Game" to CloudflareSite(
                url = "https://2game.com/catalogsearch/result/?q=game+pass",
                priceSelector = ".price, .product-price",
                titleSelector = ".product-name, .title",
                linkSelector = "a[href*='game-pass']"
            ),
            "Play-Asia" to CloudflareSite(
                url = "https://www.play-asia.com/search/game+pass+ultimate",
                priceSelector = ".price, .product-price",
                titleSelector = ".product-name, .title",
                linkSelector = "a[href*='game-pass']"
            ),
            "GameStop" to CloudflareSite(
                url = "https://www.gamestop.com/search/?q=xbox+game+pass+ultimate",
                priceSelector = ".price, .product-price, [class*='price']",
                titleSelector = ".product-name, .title",
                linkSelector = "a[href*='game-pass']"
            ),
            "Amazon" to CloudflareSite(
                url = "https://www.amazon.com/s?k=xbox+game+pass+ultimate",
                priceSelector = ".a-price-whole, .a-offscreen, [data-a-price]",
                titleSelector = ".a-text-normal, .s-line-clamp-2",
                linkSelector = "a[href*='game-pass'], a[href*='Game-Pass']"
            )
        )
        
        fun startFetch(context: Context, siteNames: List<String>) {
            val intent = Intent(context, PriceFetchService::class.java).apply {
                action = ACTION_FETCH_PRICES
                putStringArrayListExtra(EXTRA_SITE_LIST, ArrayList(siteNames))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    data class CloudflareSite(
        val url: String,
        val priceSelector: String,
        val titleSelector: String,
        val linkSelector: String
    )
    
    private val webViews = mutableListOf<WebView>()
    private var handler: Handler? = Handler(Looper.getMainLooper())
    private var isServiceRunning = true
    private var sitesToFetch = mutableListOf<String>()
    private var currentSiteIndex = AtomicInteger(0)
    private var completedSites = AtomicInteger(0)
    private val allDeals = mutableListOf<PriceDeal>()
    private val gson = Gson()
    
    // Number of parallel WebViews (3-5 simultaneous)
    private val PARALLEL_WEBVIEWS = 3
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification("Starting price fetch..."))
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_FETCH_PRICES -> {
                val sites = intent.getStringArrayListExtra(EXTRA_SITE_LIST) ?: ArrayList(CLOUDFLARE_SITES.keys)
                sitesToFetch.clear()
                
                // Filter valid sites and ensure AllKeyShop is LAST
                val validSites = sites.filter { CLOUDFLARE_SITES.containsKey(it) }.toMutableList()
                if (validSites.remove("AllKeyShop")) {
                    validSites.add("AllKeyShop") // Add at end
                }
                sitesToFetch.addAll(validSites)
                
                currentSiteIndex.set(0)
                completedSites.set(0)
                allDeals.clear()
                
                if (sitesToFetch.isNotEmpty()) {
                    // Start multiple parallel WebViews
                    handler?.post { startParallelFetching() }
                } else {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }
    
    private fun startParallelFetching() {
        // Launch up to PARALLEL_WEBVIEWS simultaneous fetches
        repeat(PARALLEL_WEBVIEWS) {
            fetchNextSite()
        }
    }
    
    private fun fetchNextSite() {
        val index = currentSiteIndex.getAndIncrement()
        
        if (index >= sitesToFetch.size) {
            // Check if all sites are done
            if (completedSites.get() >= sitesToFetch.size) {
                broadcastComplete()
                stopSelf()
            }
            return
        }
        
        val siteName = sitesToFetch[index]
        val site = CLOUDFLARE_SITES[siteName] ?: run {
            completedSites.incrementAndGet()
            fetchNextSite()
            return
        }
        
        updateNotification("Fetching $siteName (${completedSites.get() + 1}/${sitesToFetch.size})...")
        
        try {
            // Create new WebView on main thread (each site gets its own WebView for parallel fetching)
            // Use applicationContext to avoid memory leaks and crashes
            val webView = WebView(applicationContext).apply {
                layoutParams = FrameLayout.LayoutParams(1, 1) // Offscreen
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                
                webViewClient = object : BypassClient() {
                    private var pageLoaded = false
                    private val localHandler = this@PriceFetchService.handler
                    
                    override fun onPageFinishedByPassed(view: WebView?, url: String?) {
                        super.onPageFinishedByPassed(view, url)
                        
                        if (pageLoaded) return
                        pageLoaded = true
                        
                        // Wait for JS to render content
                        localHandler?.postDelayed({
                            extractPrices(view, siteName, site)
                        }, 3000) // 3 second delay for JS rendering
                    }
                }
            }
            
            // Track this WebView for cleanup
            synchronized(webViews) {
                webViews.add(webView)
            }
            
            webView.loadUrl(site.url)
            
            // Timeout after 20 seconds per site
            handler?.postDelayed({
                // If this WebView is still active, force complete and move on
                synchronized(webViews) {
                    if (webViews.contains(webView)) {
                        webViews.remove(webView)
                        webView.destroy()
                        val completed = completedSites.incrementAndGet()
                        if (completed >= sitesToFetch.size) {
                            broadcastComplete()
                            stopSelf()
                        } else if (currentSiteIndex.get() < sitesToFetch.size) {
                            fetchNextSite()
                        }
                    }
                }
            }, 20000)
        } catch (e: Exception) {
            // WebView creation failed (can happen on some devices/Android versions)
            e.printStackTrace()
            val completed = completedSites.incrementAndGet()
            handler?.post {
                if (completed >= sitesToFetch.size) {
                    broadcastComplete()
                    stopSelf()
                } else {
                    fetchNextSite()
                }
            }
        }
    }
    
    private fun extractPrices(webView: WebView?, siteName: String, site: CloudflareSite) {
        // JavaScript to extract product cards with title, price, AND URL
        val js = """
            (function() {
                var products = [];
                
                // Generic product card selectors that work across most sites
                var cardSelectors = [
                    'a[href*="game-pass"]',
                    'a[href*="gamepass"]', 
                    'a[href*="xbox"]',
                    '.product-card a',
                    '.product-item a',
                    '.product a',
                    '[class*="product"] a',
                    '[class*="offer"] a',
                    '[class*="deal"] a',
                    '.item a',
                    '.card a'
                ];
                
                var processedUrls = new Set();
                
                cardSelectors.forEach(function(selector) {
                    document.querySelectorAll(selector).forEach(function(link) {
                        var href = link.href || link.getAttribute('href') || '';
                        if (!href || processedUrls.has(href)) return;
                        if (!href.includes('game') && !href.includes('xbox') && !href.includes('pass')) return;
                        
                        processedUrls.add(href);
                        
                        // Find price near this link
                        var parent = link.closest('[class*="product"], [class*="card"], [class*="item"], [class*="offer"], li, article, div');
                        if (!parent) parent = link.parentElement;
                        
                        var priceText = '';
                        var titleText = '';
                        
                        if (parent) {
                            // Find price
                            var priceEl = parent.querySelector('[class*="price"], [class*="Price"], .amount, [data-price]');
                            if (priceEl) priceText = priceEl.innerText || priceEl.textContent || '';
                            
                            // Find title  
                            var titleEl = parent.querySelector('[class*="title"], [class*="name"], h1, h2, h3, h4, .product-name');
                            if (titleEl) titleText = titleEl.innerText || titleEl.textContent || '';
                            if (!titleText) titleText = link.innerText || link.textContent || link.title || '';
                        }
                        
                        // Extract numeric price
                        var priceMatch = priceText.replace(/,/g, '.').match(/(\d+\.?\d*)/);
                        var price = priceMatch ? parseFloat(priceMatch[1]) : 0;
                        
                        if (price > 0 && price < 200 && titleText.length > 3) {
                            products.push({
                                title: titleText.trim().substring(0, 100),
                                price: price,
                                priceText: priceText.trim(),
                                url: href.startsWith('http') ? href : (window.location.origin + href)
                            });
                        }
                    });
                });
                
                // Also try to find standalone price elements and associate with nearest link
                if (products.length === 0) {
                    document.querySelectorAll('${site.priceSelector}').forEach(function(priceEl) {
                        var priceText = priceEl.innerText || priceEl.textContent || '';
                        var priceMatch = priceText.replace(/,/g, '.').match(/(\d+\.?\d*)/);
                        var price = priceMatch ? parseFloat(priceMatch[1]) : 0;
                        
                        if (price > 0 && price < 200) {
                            var parent = priceEl.closest('a, [class*="product"], [class*="card"], li, article');
                            var link = parent ? (parent.tagName === 'A' ? parent : parent.querySelector('a')) : null;
                            var url = link ? (link.href || link.getAttribute('href') || '') : window.location.href;
                            
                            products.push({
                                title: 'Xbox Game Pass Ultimate',
                                price: price,
                                priceText: priceText.trim(),
                                url: url.startsWith('http') ? url : (window.location.origin + (url || ''))
                            });
                        }
                    });
                }
                
                return JSON.stringify({
                    products: products.slice(0, 15),
                    pageUrl: window.location.href,
                    pageTitle: document.title
                });
            })();
        """.trimIndent()
        
        webView?.evaluateJavascript(js) { result ->
            try {
                val cleanResult = result?.trim('"')?.replace("\\\"", "\"")?.replace("\\n", "\n")?.replace("\\\\", "\\") ?: "{}"
                val data = gson.fromJson(cleanResult, Map::class.java) as? Map<String, Any>
                
                val products = (data?.get("products") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                val pageUrl = data?.get("pageUrl") as? String ?: site.url
                
                // Parse products with their actual URLs
                val deals = parseProducts(siteName, products, pageUrl)
                
                if (deals.isNotEmpty()) {
                    allDeals.addAll(deals)
                    broadcastDeals(siteName, deals)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Mark this site as complete and fetch next
            val completed = completedSites.incrementAndGet()
            
            // Clean up this WebView
            synchronized(webViews) {
                webViews.remove(webView)
            }
            webView?.destroy()
            
            // Small delay before next site (human-like)
            handler?.postDelayed({
                if (completed >= sitesToFetch.size) {
                    broadcastComplete()
                    stopSelf()
                } else {
                    fetchNextSite()
                }
            }, 500) // Shorter delay since we're running parallel
        }
    }
    
    private fun parseProducts(
        siteName: String,
        products: List<Map<*, *>>,
        fallbackUrl: String
    ): List<PriceDeal> {
        val deals = mutableListOf<PriceDeal>()
        
        for (product in products) {
            try {
                val title = product["title"] as? String ?: "Xbox Game Pass Ultimate"
                val price = (product["price"] as? Number)?.toDouble() ?: continue
                val url = product["url"] as? String ?: fallbackUrl
                
                if (price > 0 && price < 200 && url.isNotEmpty()) {
                    deals.add(createDeal(siteName, price, url, title))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return deals.distinctBy { "${it.price}-${it.url}" }.take(10)
    }
    
    private fun extractPrice(text: String): Double? {
        // Extract numeric price from text like "$12.99", "12,99 €", "EUR 12.99"
        val cleanText = text.replace(",", ".").replace(" ", "")
        val regex = Regex("""(\d+\.?\d*)""")
        val match = regex.find(cleanText)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
    
    private fun createDeal(siteName: String, price: Double, url: String, title: String = "Xbox Game Pass Ultimate"): PriceDeal {
        val currency = when {
            siteName in listOf("Amazon") -> "USD"
            siteName in listOf("GG.deals", "Kinguin", "Gamivo") -> "EUR"
            else -> "USD"
        }
        
        // Detect duration from title
        val duration = when {
            title.contains("12", ignoreCase = true) || title.contains("year", ignoreCase = true) -> Duration.TWELVE_MONTHS
            title.contains("6", ignoreCase = true) -> Duration.SIX_MONTHS
            title.contains("3", ignoreCase = true) -> Duration.THREE_MONTHS
            else -> Duration.ONE_MONTH
        }
        
        // Detect region from title or URL
        val region = when {
            title.contains("UAE", ignoreCase = true) || url.contains("uae", ignoreCase = true) -> Region.UAE
            title.contains("Turkey", ignoreCase = true) || title.contains("TR", ignoreCase = true) -> Region.TURKEY
            title.contains("Argentina", ignoreCase = true) || title.contains("AR", ignoreCase = true) -> Region.ARGENTINA
            title.contains("Brazil", ignoreCase = true) || title.contains("BR", ignoreCase = true) -> Region.BRAZIL
            title.contains("US", ignoreCase = true) || title.contains("USA", ignoreCase = true) -> Region.US
            title.contains("EU", ignoreCase = true) || title.contains("Europe", ignoreCase = true) -> Region.EU
            else -> Region.GLOBAL
        }
        
        return PriceDeal(
            id = UUID.randomUUID().toString(),
            sellerName = siteName,
            price = price,
            currency = currency,
            region = region,
            type = DealType.KEY,
            duration = duration,
            url = url,
            trustLevel = TrustLevel.HIGH,
            rating = 4.0f,
            reviewCount = 1000
        )
    }
    
    private fun broadcastDeals(siteName: String, deals: List<PriceDeal>) {
        val intent = Intent(ACTION_PRICE_RESULT).apply {
            putExtra(EXTRA_SITE_COMPLETED, siteName)
            putExtra(EXTRA_DEALS, gson.toJson(deals))
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    private fun broadcastComplete() {
        val intent = Intent(ACTION_FETCH_COMPLETE).apply {
            putExtra(EXTRA_DEALS, gson.toJson(allDeals))
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Price Fetch Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background price fetching from game key sites"
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
        val notification = buildNotification(text)
        getSystemService(NotificationManager::class.java)?.notify(1, notification)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        isServiceRunning = false
        // Clean up all WebViews
        synchronized(webViews) {
            webViews.forEach { it.destroy() }
            webViews.clear()
        }
        handler?.removeCallbacksAndMessages(null)
        handler = null
        super.onDestroy()
    }
}
