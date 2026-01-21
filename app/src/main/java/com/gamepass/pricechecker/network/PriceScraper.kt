package com.gamepass.pricechecker.network

import android.content.Context
import com.gamepass.pricechecker.models.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.UUID
import kotlin.random.Random

/**
 * Search query variants and matching utilities
 */
object GamePassSearchUtils {
    
    // All search query variants for different sites
    val searchQueries = listOf(
        "xbox+game+pass+ultimate",
        "xbox+gamepass+ultimate",
        "game+pass+ultimate",
        "gamepass+ultimate",
        "xbox+gpu"
    )
    
    // Keywords that indicate it's a Game Pass Ultimate product
    private val requiredKeywords = listOf("ultimate")
    private val gamePassKeywords = listOf("game pass", "gamepass", "game+pass")
    
    // Keywords that indicate it's a trial (to mark as trial)
    val trialKeywords = listOf("trial", "14 day", "14-day", "7 day", "7-day", "3 day", "1 day")
    
    /**
     * Check if a product title is a valid Game Pass Ultimate product
     */
    fun isValidGamePassUltimate(title: String): Boolean {
        val lowerTitle = title.lowercase()
        
        // Must contain "ultimate"
        if (!requiredKeywords.any { lowerTitle.contains(it) }) {
            return false
        }
        
        // Must contain some form of "game pass"
        if (!gamePassKeywords.any { lowerTitle.contains(it) }) {
            return false
        }
        
        return true
    }
    
    /**
     * Check if the product is a trial
     */
    fun isTrial(title: String): Boolean {
        val lowerTitle = title.lowercase()
        return trialKeywords.any { lowerTitle.contains(it) }
    }
    
    /**
     * Get the best search URL for a given base URL pattern
     */
    fun getSearchUrl(basePattern: String): String {
        return basePattern.replace("{query}", searchQueries.first())
    }
}

/**
 * Progress callback for streaming search results
 */
data class SearchProgress(
    val currentSite: String,
    val sitesSearched: Int,
    val totalSites: Int,
    val dealsFound: Int
)

/**
 * Main price scraper that aggregates results from multiple sources
 * Uses WebView for Cloudflare-protected sites
 * Supports streaming results with progress callbacks
 */
class PriceScraper(private val context: Context? = null) {
    
    // WebView scraper for Cloudflare sites (lazy initialized)
    private val webViewScraper: WebViewScraper? by lazy {
        context?.let { WebViewScraper(it) }
    }
    
    // All scrapers with their display names
    private val scraperInfoList = listOf(
        "CDKeys" to CDKeysScraper(),
        "Eneba" to EnebaScraper(),
        "G2A" to G2AScraper(),
        "Instant Gaming" to InstantGamingScraper(),
        "Kinguin" to KinguinScraper(),
        "Gamivo" to GamivoScraper(),
        "Difmark" to DifmarkScraper(),
        "AllKeyShop" to AllKeyShopScraper()
    )
    
    private val scrapers = scraperInfoList.map { it.second }
    
    /**
     * Search all sources with streaming results and progress updates
     * Results appear as they are found and auto-sorted by price
     */
    suspend fun searchAllStreaming(
        filters: SearchFilters,
        onProgress: (SearchProgress) -> Unit,
        onDealsFound: (List<PriceDeal>) -> Unit
    ): SearchResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val allDeals = mutableListOf<PriceDeal>()
            var sitesSearched = 0
            val totalSites = scraperInfoList.size
            
            try {
                // Search each site sequentially to show progress
                // But run some in parallel batches for speed
                val batchSize = 3
                val batches = scraperInfoList.chunked(batchSize)
                
                for (batch in batches) {
                    // Run batch in parallel
                    val batchResults = batch.map { (name, scraper) ->
                        async {
                            // Update progress before starting
                            withContext(Dispatchers.Main) {
                                onProgress(SearchProgress(
                                    currentSite = name,
                                    sitesSearched = sitesSearched,
                                    totalSites = totalSites,
                                    dealsFound = allDeals.size
                                ))
                            }
                            
                            try {
                                val deals = scraper.scrape(filters)
                                    .filter { filters.matches(it) }
                                name to deals
                            } catch (e: Exception) {
                                e.printStackTrace()
                                name to emptyList()
                            }
                        }
                    }.awaitAll()
                    
                    // Process batch results
                    for ((name, deals) in batchResults) {
                        sitesSearched++
                        
                        if (deals.isNotEmpty()) {
                            // Add new deals
                            allDeals.addAll(deals)
                            
                            // Sort by price and remove duplicates
                            val sortedDeals = allDeals
                                .distinctBy { "${it.sellerName}-${it.region}-${it.duration}" }
                                .sortedWith(compareBy(
                                    { if (it.region == Region.UAE || it.region == Region.GLOBAL) 0 else 1 },
                                    { it.price }
                                ))
                            
                            allDeals.clear()
                            allDeals.addAll(sortedDeals)
                            
                            // Emit updated results to UI
                            withContext(Dispatchers.Main) {
                                onDealsFound(allDeals.toList())
                                onProgress(SearchProgress(
                                    currentSite = "Found ${deals.size} from $name",
                                    sitesSearched = sitesSearched,
                                    totalSites = totalSites,
                                    dealsFound = allDeals.size
                                ))
                            }
                        } else {
                            // Update progress even if no deals found
                            withContext(Dispatchers.Main) {
                                onProgress(SearchProgress(
                                    currentSite = "$name (no results)",
                                    sitesSearched = sitesSearched,
                                    totalSites = totalSites,
                                    dealsFound = allDeals.size
                                ))
                            }
                        }
                    }
                    
                    // Small delay between batches for UI updates
                    delay(100)
                }
                
                val searchTime = System.currentTimeMillis() - startTime
                
                if (allDeals.isEmpty()) {
                    SearchResult.Empty
                } else {
                    SearchResult.Success(
                        deals = allDeals,
                        totalFound = allDeals.size,
                        searchTimeMs = searchTime,
                        sourcesSearched = sitesSearched
                    )
                }
            } catch (e: Exception) {
                SearchResult.Error("Failed to fetch prices: ${e.message}", e)
            }
        }
    }
    
    /**
     * Legacy method - Search all sources for Game Pass Ultimate deals
     */
    suspend fun searchAll(filters: SearchFilters): SearchResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            
            try {
                // Run all scrapers in parallel
                val results = scrapers.map { scraper ->
                    async {
                        try {
                            scraper.scrape(filters)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            emptyList()
                        }
                    }
                }.awaitAll()
                
                // Flatten and filter results
                val allDeals = results.flatten()
                    .filter { filters.matches(it) }
                    .sortedBy { it.price }
                    .distinctBy { "${it.sellerName}-${it.region}-${it.duration}" }
                
                val searchTime = System.currentTimeMillis() - startTime
                
                if (allDeals.isEmpty()) {
                    SearchResult.Empty
                } else {
                    SearchResult.Success(
                        deals = allDeals,
                        totalFound = allDeals.size,
                        searchTimeMs = searchTime,
                        sourcesSearched = scrapers.size
                    )
                }
            } catch (e: Exception) {
                SearchResult.Error("Failed to fetch prices: ${e.message}", e)
            }
        }
    }
}

/**
 * Base interface for all scrapers
 */
interface BaseScraper {
    suspend fun scrape(filters: SearchFilters): List<PriceDeal>
    
    /**
     * Helper to fetch HTML with realistic browser headers
     * Uses Android Chrome headers to appear as legitimate mobile browser
     */
    fun fetchDocument(url: String, timeoutMs: Int = 20000): Document {
        // Add random delay to mimic human behavior (500ms - 2000ms)
        Thread.sleep(Random.nextLong(500, 2000))
        
        return Jsoup.connect(url)
            // Realistic Android Chrome User-Agent
            .userAgent("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            // Full browser-like headers
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9,ar;q=0.8")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Cache-Control", "max-age=0")
            .header("sec-ch-ua", "\"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\", \"Not-A.Brand\";v=\"99\"")
            .header("sec-ch-ua-mobile", "?1")
            .header("sec-ch-ua-platform", "\"Android\"")
            .followRedirects(true)
            .ignoreHttpErrors(true)
            .timeout(timeoutMs)
            .get()
    }
    
    /**
     * Fetch document with retry logic and exponential backoff
     * Good for slow sites like AllKeyShop
     */
    fun fetchDocumentWithRetry(url: String, maxRetries: Int = 3, baseTimeoutMs: Int = 30000): Document? {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                // Exponential backoff: 30s, 45s, 60s
                val timeout = baseTimeoutMs + (attempt * 15000)
                
                // Add delay between retries
                if (attempt > 0) {
                    Thread.sleep(Random.nextLong(2000, 5000))
                }
                
                return fetchDocument(url, timeout)
            } catch (e: Exception) {
                lastException = e
                // Continue to next retry
            }
        }
        
        lastException?.let { throw it }
        return null
    }
    
    /**
     * Generate unique ID for a deal
     */
    fun generateId(): String = UUID.randomUUID().toString().take(8)
}

/**
 * Scraper for AllKeyShop - a price aggregator
 * Uses retry logic with longer timeouts since this site can be slow
 */
class AllKeyShopScraper : BaseScraper {
    
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            
            try {
                val regionParam = when (filters.region) {
                    Region.US -> "us"
                    Region.UK -> "uk"
                    Region.EU -> "eu"
                    Region.UAE -> "ae"
                    Region.GLOBAL -> "ww"
                    else -> "ww"
                }
                
                val url = "https://www.allkeyshop.com/blog/buy-xbox-game-pass-ultimate-cd-key-compare-prices/"
                // Use retry logic with 60s timeout for this slow site
                val doc = fetchDocumentWithRetry(url, maxRetries = 3, baseTimeoutMs = 60000) ?: return@withContext deals
                
                // Parse the price comparison table
                doc.select(".offers-table .offers-table-row").forEach { row ->
                    try {
                        val merchant = row.select(".merchant-name").text().trim()
                        val priceText = row.select(".price").text()
                            .replace("[^0-9.,]".toRegex(), "")
                            .replace(",", ".")
                        val price = priceText.toDoubleOrNull() ?: return@forEach
                        
                        val link = row.select("a.buy-btn").attr("href")
                        val region = parseRegionFromText(row.select(".region").text())
                        
                        val seller = Sellers.getAll().find { 
                            merchant.contains(it.name, ignoreCase = true) 
                        }
                        
                        deals.add(PriceDeal(
                            id = generateId(),
                            sellerName = merchant,
                            price = price,
                            currency = "USD",
                            region = region,
                            type = DealType.KEY,
                            duration = Duration.ONE_MONTH,
                            url = link.ifEmpty { "https://www.allkeyshop.com" },
                            trustLevel = seller?.trustLevel ?: TrustLevel.MEDIUM
                        ))
                    } catch (e: Exception) {
                        // Skip malformed entries
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            deals
        }
    }
    
    private fun parseRegionFromText(text: String): Region {
        return when {
            text.contains("Global", ignoreCase = true) -> Region.GLOBAL
            text.contains("UAE", ignoreCase = true) || text.contains("Arab", ignoreCase = true) -> Region.UAE
            text.contains("US", ignoreCase = true) || text.contains("United States", ignoreCase = true) -> Region.US
            text.contains("UK", ignoreCase = true) || text.contains("United Kingdom", ignoreCase = true) -> Region.UK
            text.contains("EU", ignoreCase = true) || text.contains("Europe", ignoreCase = true) -> Region.EU
            text.contains("Turkey", ignoreCase = true) || text.contains("TR", ignoreCase = true) -> Region.TURKEY
            text.contains("Brazil", ignoreCase = true) || text.contains("BR", ignoreCase = true) -> Region.BRAZIL
            text.contains("Argentina", ignoreCase = true) || text.contains("AR", ignoreCase = true) -> Region.ARGENTINA
            text.contains("India", ignoreCase = true) || text.contains("IN", ignoreCase = true) -> Region.INDIA
            else -> Region.GLOBAL
        }
    }
}

/**
 * Scraper for CDKeys
 */
class CDKeysScraper : BaseScraper {
    
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            
            try {
                val url = "https://www.cdkeys.com/catalogsearch/result/?q=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                
                doc.select(".product-item").forEach { item ->
                    try {
                        val title = item.select(".product-item-name").text()
                        
                        // Only include valid Game Pass Ultimate products
                        if (!GamePassSearchUtils.isValidGamePassUltimate(title)) return@forEach
                        
                        val priceText = item.select(".price").first()?.text()
                            ?.replace("[^0-9.,]".toRegex(), "")
                            ?.replace(",", ".")
                        val price = priceText?.toDoubleOrNull() ?: return@forEach
                        
                        val link = item.select("a.product-item-link").attr("href")
                        val duration = parseDurationFromTitle(title)
                        val region = parseRegionFromTitle(title)
                        
                        deals.add(PriceDeal(
                            id = generateId(),
                            sellerName = "CDKeys",
                            price = price,
                            currency = "USD",
                            region = region,
                            type = DealType.KEY,
                            duration = duration,
                            url = link.ifEmpty { "https://www.cdkeys.com" },
                            trustLevel = TrustLevel.HIGH,
                            isTrial = GamePassSearchUtils.isTrial(title)
                        ))
                    } catch (e: Exception) {
                        // Skip malformed entries
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            deals
        }
    }
    
    private fun parseDurationFromTitle(title: String): Duration {
        return when {
            title.contains("12 month", ignoreCase = true) || title.contains("1 year", ignoreCase = true) -> Duration.TWELVE_MONTHS
            title.contains("6 month", ignoreCase = true) -> Duration.SIX_MONTHS
            title.contains("3 month", ignoreCase = true) -> Duration.THREE_MONTHS
            title.contains("1 month", ignoreCase = true) -> Duration.ONE_MONTH
            else -> Duration.ONE_MONTH
        }
    }
    
    private fun parseRegionFromTitle(title: String): Region {
        return when {
            title.contains("Global", ignoreCase = true) -> Region.GLOBAL
            title.contains("UAE", ignoreCase = true) -> Region.UAE
            title.contains("US", ignoreCase = true) -> Region.US
            title.contains("UK", ignoreCase = true) -> Region.UK
            title.contains("EU", ignoreCase = true) -> Region.EU
            title.contains("TR", ignoreCase = true) || title.contains("Turkey", ignoreCase = true) -> Region.TURKEY
            title.contains("BR", ignoreCase = true) || title.contains("Brazil", ignoreCase = true) -> Region.BRAZIL
            title.contains("AR", ignoreCase = true) || title.contains("Argentina", ignoreCase = true) -> Region.ARGENTINA
            else -> Region.GLOBAL
        }
    }
}

/**
 * Scraper for Eneba
 */
class EnebaScraper : BaseScraper {
    
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            
            try {
                val url = "https://www.eneba.com/store/xbox?text=game%20pass%20ultimate"
                val doc = fetchDocument(url)
                
                doc.select("[data-component='ProductCard']").forEach { card ->
                    try {
                        val title = card.select("[data-component='ProductCardTitle']").text()
                        
                        if (!GamePassSearchUtils.isValidGamePassUltimate(title)) return@forEach
                        
                        val priceText = card.select("[data-component='Price']").text()
                            .replace("[^0-9.,]".toRegex(), "")
                            .replace(",", ".")
                        val price = priceText.toDoubleOrNull() ?: return@forEach
                        
                        val link = card.select("a").attr("href")
                        val fullLink = if (link.startsWith("http")) link else "https://www.eneba.com$link"
                        
                        deals.add(PriceDeal(
                            id = generateId(),
                            sellerName = "Eneba",
                            price = price,
                            currency = "EUR",
                            region = parseRegionFromTitle(title),
                            type = DealType.KEY,
                            duration = parseDurationFromTitle(title),
                            url = fullLink,
                            trustLevel = TrustLevel.HIGH
                        ))
                    } catch (e: Exception) {
                        // Skip malformed entries
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            deals
        }
    }
    
    private fun parseDurationFromTitle(title: String): Duration {
        return when {
            title.contains("12", ignoreCase = true) || title.contains("year", ignoreCase = true) -> Duration.TWELVE_MONTHS
            title.contains("6", ignoreCase = true) -> Duration.SIX_MONTHS
            title.contains("3", ignoreCase = true) -> Duration.THREE_MONTHS
            else -> Duration.ONE_MONTH
        }
    }
    
    private fun parseRegionFromTitle(title: String): Region {
        return when {
            title.contains("Global", ignoreCase = true) -> Region.GLOBAL
            title.contains("UAE", ignoreCase = true) -> Region.UAE
            title.contains("US", ignoreCase = true) -> Region.US
            title.contains("UK", ignoreCase = true) -> Region.UK
            title.contains("EU", ignoreCase = true) -> Region.EU
            title.contains("Turkey", ignoreCase = true) -> Region.TURKEY
            else -> Region.GLOBAL
        }
    }
}

/**
 * Scraper for G2A (with caution warning)
 */
class G2AScraper : BaseScraper {
    
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            
            try {
                val url = "https://www.g2a.com/search?query=xbox%20game%20pass%20ultimate"
                val doc = fetchDocument(url)
                
                doc.select("[data-testid='ProductCard']").forEach { card ->
                    try {
                        val title = card.select("[data-testid='ProductCard-title']").text()
                        
                        if (!GamePassSearchUtils.isValidGamePassUltimate(title)) return@forEach
                        
                        val priceText = card.select("[data-testid='ProductCard-price']").text()
                            .replace("[^0-9.,]".toRegex(), "")
                            .replace(",", ".")
                        val price = priceText.toDoubleOrNull() ?: return@forEach
                        
                        val link = card.select("a").attr("href")
                        val fullLink = if (link.startsWith("http")) link else "https://www.g2a.com$link"
                        
                        deals.add(PriceDeal(
                            id = generateId(),
                            sellerName = "G2A",
                            price = price,
                            currency = "EUR",
                            region = parseRegionFromTitle(title),
                            type = DealType.KEY,
                            duration = parseDurationFromTitle(title),
                            url = fullLink,
                            trustLevel = TrustLevel.HIGH
                        ))
                    } catch (e: Exception) {
                        // Skip malformed entries
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            deals
        }
    }
    
    private fun parseDurationFromTitle(title: String): Duration {
        return when {
            title.contains("12", ignoreCase = true) -> Duration.TWELVE_MONTHS
            title.contains("6", ignoreCase = true) -> Duration.SIX_MONTHS
            title.contains("3", ignoreCase = true) -> Duration.THREE_MONTHS
            else -> Duration.ONE_MONTH
        }
    }
    
    private fun parseRegionFromTitle(title: String): Region {
        return when {
            title.contains("Global", ignoreCase = true) -> Region.GLOBAL
            title.contains("UAE", ignoreCase = true) -> Region.UAE
            title.contains("US", ignoreCase = true) -> Region.US
            title.contains("UK", ignoreCase = true) -> Region.UK
            title.contains("EU", ignoreCase = true) -> Region.EU
            title.contains("Turkey", ignoreCase = true) -> Region.TURKEY
            else -> Region.GLOBAL
        }
    }
}

/**
 * Scraper for Instant Gaming
 */
class InstantGamingScraper : BaseScraper {
    
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.instant-gaming.com/en/search/?q=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".item").forEach { item ->
                    try {
                        val title = item.select(".title").text()
                        if (!GamePassSearchUtils.isValidGamePassUltimate(title)) return@forEach
                        val priceText = item.select(".price").text()
                            .replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                        val price = priceText.toDoubleOrNull() ?: return@forEach
                        val link = item.select("a").attr("href")
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Instant Gaming", price = price,
                            currency = "EUR", region = parseRegion(title), type = DealType.KEY,
                            duration = parseDuration(title), url = link.ifEmpty { "https://www.instant-gaming.com" },
                            trustLevel = TrustLevel.HIGH
                        ))
                    } catch (e: Exception) { }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals
        }
    }
    private fun parseDuration(t: String) = when { t.contains("12") -> Duration.TWELVE_MONTHS; t.contains("6") -> Duration.SIX_MONTHS; t.contains("3") -> Duration.THREE_MONTHS; else -> Duration.ONE_MONTH }
    private fun parseRegion(t: String) = when { t.contains("Global", true) -> Region.GLOBAL; t.contains("EU", true) -> Region.EU; t.contains("US", true) -> Region.US; else -> Region.GLOBAL }
}

/**
 * Scraper for Kinguin
 */
class KinguinScraper : BaseScraper {
    
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.kinguin.net/listing?phrase=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select("[data-product-card]").forEach { card ->
                    try {
                        val title = card.select("[data-product-name]").text()
                        if (!GamePassSearchUtils.isValidGamePassUltimate(title)) return@forEach
                        val priceText = card.select("[data-product-price]").text()
                            .replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                        val price = priceText.toDoubleOrNull() ?: return@forEach
                        val link = card.select("a").attr("href")
                        val fullLink = if (link.startsWith("http")) link else "https://www.kinguin.net$link"
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Kinguin", price = price,
                            currency = "EUR", region = parseRegion(title), type = DealType.KEY,
                            duration = parseDuration(title), url = fullLink, trustLevel = TrustLevel.HIGH
                        ))
                    } catch (e: Exception) { }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals
        }
    }
    private fun parseDuration(t: String) = when { t.contains("12") -> Duration.TWELVE_MONTHS; t.contains("6") -> Duration.SIX_MONTHS; t.contains("3") -> Duration.THREE_MONTHS; else -> Duration.ONE_MONTH }
    private fun parseRegion(t: String) = when { t.contains("Global", true) -> Region.GLOBAL; t.contains("Turkey", true) -> Region.TURKEY; t.contains("Brazil", true) -> Region.BRAZIL; t.contains("EU", true) -> Region.EU; else -> Region.GLOBAL }
}

/**
 * Scraper for Gamivo
 */
class GamivoScraper : BaseScraper {
    
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.gamivo.com/search?query=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".product-card").forEach { card ->
                    try {
                        val title = card.select(".product-card__title").text()
                        if (!GamePassSearchUtils.isValidGamePassUltimate(title)) return@forEach
                        val priceText = card.select(".product-card__price").text()
                            .replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                        val price = priceText.toDoubleOrNull() ?: return@forEach
                        val link = card.select("a").attr("href")
                        val fullLink = if (link.startsWith("http")) link else "https://www.gamivo.com$link"
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Gamivo", price = price,
                            currency = "EUR", region = parseRegion(title), type = DealType.KEY,
                            duration = parseDuration(title), url = fullLink, trustLevel = TrustLevel.HIGH
                        ))
                    } catch (e: Exception) { }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals
        }
    }
    private fun parseDuration(t: String) = when { t.contains("12") -> Duration.TWELVE_MONTHS; t.contains("6") -> Duration.SIX_MONTHS; t.contains("3") -> Duration.THREE_MONTHS; else -> Duration.ONE_MONTH }
    private fun parseRegion(t: String) = when { t.contains("Global", true) -> Region.GLOBAL; t.contains("Turkey", true) -> Region.TURKEY; t.contains("Argentina", true) -> Region.ARGENTINA; t.contains("EU", true) -> Region.EU; else -> Region.GLOBAL }
}

/**
 * Scraper for Difmark (good for regional keys)
 */
class DifmarkScraper : BaseScraper {
    
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.difmark.com/search?q=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".product-item").forEach { item ->
                    try {
                        val title = item.select(".product-title").text()
                        if (!GamePassSearchUtils.isValidGamePassUltimate(title)) return@forEach
                        val priceText = item.select(".price").text()
                            .replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                        val price = priceText.toDoubleOrNull() ?: return@forEach
                        val link = item.select("a").attr("href")
                        val fullLink = if (link.startsWith("http")) link else "https://www.difmark.com$link"
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Difmark", price = price,
                            currency = "EUR", region = parseRegion(title), type = DealType.KEY,
                            duration = parseDuration(title), url = fullLink, trustLevel = TrustLevel.HIGH
                        ))
                    } catch (e: Exception) { }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals
        }
    }
    private fun parseDuration(t: String) = when { t.contains("12") -> Duration.TWELVE_MONTHS; t.contains("6") -> Duration.SIX_MONTHS; t.contains("3") -> Duration.THREE_MONTHS; else -> Duration.ONE_MONTH }
    private fun parseRegion(t: String) = when { t.contains("Turkey", true) || t.contains("TR") -> Region.TURKEY; t.contains("Brazil", true) || t.contains("BR") -> Region.BRAZIL; t.contains("Argentina", true) -> Region.ARGENTINA; else -> Region.GLOBAL }
}
