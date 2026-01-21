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
import com.darkryh.cloudflarebypass.ByPassWebClient
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
    
    private var webView: WebView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var sitesToFetch = mutableListOf<String>()
    private var currentSiteIndex = AtomicInteger(0)
    private val allDeals = mutableListOf<PriceDeal>()
    private val gson = Gson()
    
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
                sitesToFetch.addAll(sites.filter { CLOUDFLARE_SITES.containsKey(it) })
                currentSiteIndex.set(0)
                allDeals.clear()
                
                if (sitesToFetch.isNotEmpty()) {
                    handler.post { fetchNextSite() }
                } else {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }
    
    private fun fetchNextSite() {
        val index = currentSiteIndex.getAndIncrement()
        
        if (index >= sitesToFetch.size) {
            // All sites fetched
            broadcastComplete()
            stopSelf()
            return
        }
        
        val siteName = sitesToFetch[index]
        val site = CLOUDFLARE_SITES[siteName] ?: run {
            fetchNextSite()
            return
        }
        
        updateNotification("Fetching $siteName (${index + 1}/${sitesToFetch.size})...")
        
        // Clean up previous WebView
        webView?.destroy()
        
        // Create new WebView on main thread
        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(1, 1) // Offscreen
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            
            webViewClient = object : ByPassWebClient() {
                private var pageLoaded = false
                
                override fun onPageFinishedByPassed(view: WebView?, url: String?) {
                    super.onPageFinishedByPassed(view, url)
                    
                    if (pageLoaded) return
                    pageLoaded = true
                    
                    // Wait for JS to render content
                    handler.postDelayed({
                        extractPrices(view, siteName, site)
                    }, 3000) // 3 second delay for JS rendering
                }
            }
        }
        
        webView?.loadUrl(site.url)
        
        // Timeout after 30 seconds
        handler.postDelayed({
            if (currentSiteIndex.get() == index + 1) {
                // Still on this site, move to next
                fetchNextSite()
            }
        }, 30000)
    }
    
    private fun extractPrices(webView: WebView?, siteName: String, site: CloudflareSite) {
        val js = """
            (function() {
                var prices = [];
                var priceElements = document.querySelectorAll('${site.priceSelector}');
                priceElements.forEach(function(el) {
                    var text = el.innerText || el.textContent || '';
                    if (text.match(/[\d.,]+/)) {
                        prices.push(text.trim());
                    }
                });
                return JSON.stringify({
                    html: document.body.innerHTML.substring(0, 50000),
                    prices: prices.slice(0, 10),
                    url: window.location.href
                });
            })();
        """.trimIndent()
        
        webView?.evaluateJavascript(js) { result ->
            try {
                val cleanResult = result?.trim('"')?.replace("\\\"", "\"")?.replace("\\n", "\n") ?: "{}"
                val data = gson.fromJson(cleanResult, Map::class.java) as? Map<String, Any>
                
                val html = data?.get("html") as? String ?: ""
                val prices = (data?.get("prices") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val finalUrl = data?.get("url") as? String ?: site.url
                
                // Parse prices and create deals
                val deals = parsePrices(siteName, site, html, prices, finalUrl)
                
                if (deals.isNotEmpty()) {
                    allDeals.addAll(deals)
                    broadcastDeals(siteName, deals)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Small delay before next site (human-like)
            handler.postDelayed({
                fetchNextSite()
            }, 1500)
        }
    }
    
    private fun parsePrices(
        siteName: String,
        site: CloudflareSite,
        html: String,
        extractedPrices: List<String>,
        url: String
    ): List<PriceDeal> {
        val deals = mutableListOf<PriceDeal>()
        
        // Try to parse prices from extracted JS data
        for (priceText in extractedPrices) {
            val price = extractPrice(priceText)
            if (price != null && price > 0 && price < 200) {
                deals.add(createDeal(siteName, price, url))
            }
        }
        
        // Fallback: Parse HTML with Jsoup
        if (deals.isEmpty() && html.isNotEmpty()) {
            try {
                val doc = Jsoup.parse(html)
                doc.select(site.priceSelector).forEach { elem ->
                    val price = extractPrice(elem.text())
                    if (price != null && price > 0 && price < 200) {
                        deals.add(createDeal(siteName, price, url))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return deals.distinctBy { it.price }.take(5)
    }
    
    private fun extractPrice(text: String): Double? {
        // Extract numeric price from text like "$12.99", "12,99 €", "EUR 12.99"
        val cleanText = text.replace(",", ".").replace(" ", "")
        val regex = Regex("""(\d+\.?\d*)""")
        val match = regex.find(cleanText)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
    
    private fun createDeal(siteName: String, price: Double, url: String): PriceDeal {
        val currency = when {
            siteName in listOf("Amazon") -> "USD"
            siteName in listOf("GG.deals", "Kinguin", "Gamivo") -> "EUR"
            else -> "USD"
        }
        
        return PriceDeal(
            id = UUID.randomUUID().toString(),
            sellerName = siteName,
            price = price,
            currency = currency,
            region = Region.GLOBAL,
            type = DealType.KEY,
            duration = Duration.ONE_MONTH,
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
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
