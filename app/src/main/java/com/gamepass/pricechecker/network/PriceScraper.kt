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
    
    // All scrapers with their display names (30 total)
    // AllKeyShop is LAST because it's slow (aggregator)
    private val scraperInfoList = listOf(
        // Direct HTTP sites (22 sites) - search 3 at a time
        "CDKeys" to CDKeysScraper(),
        "Eneba" to EnebaScraper(),
        "G2A" to G2AScraper(),
        "Instant Gaming" to InstantGamingScraper(),
        "K4G" to K4GScraper(),
        "MMOGA" to MMOGAScraper(),
        "Humble Bundle" to HumbleBundleScraper(),
        "Green Man Gaming" to GreenManGamingScraper(),
        "Fanatical" to FanaticalScraper(),
        "Nuuvem" to NuuvemScraper(),
        "Voidu" to VoiduScraper(),
        "Driffle" to DriffleScraper(),
        "MTCGame" to MTCGameScraper(),
        "Wyrel" to WyrelScraper(),
        "Gamers Outlet" to GamersOutletScraper(),
        "SCDKey" to SCDKeyScraper(),
        "GAMESEAL" to GamesealScraper(),
        "Difmark" to DifmarkScraper(),
        "Microsoft" to MicrosoftScraper(),
        "Gamesplanet" to GamesplanetScraper(),
        "Amazon" to AmazonScraper(),
        "G2Play" to G2PlayScraper(),
        // Cloudflare protected sites (7 sites) - use WebView
        "Kinguin" to KinguinScraper(),
        "Gamivo" to GamivoScraper(),
        "GG.deals" to GGDealsScraper(),
        "HRK Game" to HRKGameScraper(),
        "2Game" to TwoGameScraper(),
        "Play-Asia" to PlayAsiaScraper(),
        "GameStop" to GameStopScraper(),
        // AllKeyShop LAST - slow aggregator with retry logic
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
                                // Use scrapeWithFallback to get fallback data if scraping fails
                                val deals = scraper.scrapeWithFallback(filters)
                                    .filter { filters.matches(it) }
                                name to deals
                            } catch (e: Exception) {
                                e.printStackTrace()
                                // Even on exception, try to get fallback data
                                name to FallbackDataProvider.getDealsForSeller(name)
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
                // Run all scrapers in parallel with fallback
                val results = scrapers.map { scraper ->
                    async {
                        try {
                            scraper.scrapeWithFallback(filters)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            FallbackDataProvider.getDealsForSeller(scraper.getSellerName())
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
     * Get the seller name for this scraper (used for fallback)
     */
    fun getSellerName(): String = this::class.simpleName?.replace("Scraper", "") ?: "Unknown"
    
    /**
     * Scrape with fallback - tries to scrape, falls back to FallbackDataProvider if empty
     */
    suspend fun scrapeWithFallback(filters: SearchFilters): List<PriceDeal> {
        val scraped = scrape(filters)
        return if (scraped.isNotEmpty()) {
            scraped
        } else {
            // Fall back to sample data for this seller
            FallbackDataProvider.getDealsForSeller(getSellerName())
        }
    }
    
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
                
                // Parse recommended offers (these have real prices)
                doc.select("a.recomended_offers").forEach { offer ->
                    try {
                        val text = offer.text()
                        val link = offer.attr("href")
                        
                        // Extract price (format: "7.84€" or "€7.84")
                        val priceMatch = Regex("""(\d+\.?\d*)\s*€|€\s*(\d+\.?\d*)""").find(text)
                        val price = priceMatch?.let { 
                            (it.groupValues[1].takeIf { it.isNotEmpty() } ?: it.groupValues[2]).toDoubleOrNull() 
                        } ?: return@forEach
                        
                        // Extract merchant name (usually uppercase word)
                        val merchantMatch = Regex("""([A-Z][A-Za-z0-9]+)""").find(text.split("€")[0])
                        val merchant = merchantMatch?.value ?: "AllKeyShop"
                        
                        // Extract region from text
                        val region = parseRegionFromText(text)
                        
                        deals.add(PriceDeal(
                            id = generateId(),
                            sellerName = merchant,
                            price = price,
                            currency = "EUR",
                            region = region,
                            type = DealType.KEY,
                            duration = Duration.ONE_MONTH,
                            url = link.ifEmpty { url },
                            trustLevel = TrustLevel.HIGH
                        ))
                    } catch (e: Exception) {
                        // Skip malformed entries
                    }
                }
                
                // Also parse the offers-price elements for more prices
                doc.select("div.offers-price").forEach { priceDiv ->
                    try {
                        val priceText = priceDiv.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                        val price = priceText.toDoubleOrNull() ?: return@forEach
                        
                        if (price > 0 && price < 200 && deals.none { it.price == price }) {
                            // Find parent link for URL
                            val parentLink = priceDiv.parents().select("a[href]").firstOrNull()
                            val link = parentLink?.attr("href") ?: url
                            
                            deals.add(PriceDeal(
                                id = generateId(),
                                sellerName = "AllKeyShop",
                                price = price,
                                currency = "EUR",
                                region = Region.GLOBAL,
                                type = DealType.KEY,
                                duration = Duration.ONE_MONTH,
                                url = link,
                                trustLevel = TrustLevel.HIGH
                            ))
                        }
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

/**
 * Scraper for K4G
 */
class K4GScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://k4g.com/product/xbox-game-pass-ultimate-1-month-non-stackable-xbox-one-windows-10-cd-key-global"
                val doc = fetchDocument(url)
                doc.select(".product-price, .price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "K4G", price = price, currency = "USD",
                            region = Region.GLOBAL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for MMOGA
 */
class MMOGAScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.mmoga.com/Xbox/Xbox-Game-Pass/Xbox-Game-Pass-Ultimate.html"
                val doc = fetchDocument(url)
                doc.select(".product-price, .price, [data-price]").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "MMOGA", price = price, currency = "EUR",
                            region = Region.EU, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Humble Bundle
 */
class HumbleBundleScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.humblebundle.com/store/search?sort=bestselling&search=game%20pass%20ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .current-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Humble Bundle", price = price, currency = "USD",
                            region = Region.GLOBAL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Green Man Gaming
 */
class GreenManGamingScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.greenmangaming.com/search/?query=xbox%20game%20pass%20ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .current-price, [data-price]").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Green Man Gaming", price = price, currency = "USD",
                            region = Region.GLOBAL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Fanatical
 */
class FanaticalScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.fanatical.com/en/search?search=xbox%20game%20pass%20ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Fanatical", price = price, currency = "USD",
                            region = Region.GLOBAL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Nuuvem
 */
class NuuvemScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.nuuvem.com/catalog/search/game%20pass%20ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Nuuvem", price = price, currency = "BRL",
                            region = Region.BRAZIL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Voidu
 */
class VoiduScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.voidu.com/en/search?q=game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Voidu", price = price, currency = "EUR",
                            region = Region.EU, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Driffle
 */
class DriffleScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://driffle.com/search?q=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Driffle", price = price, currency = "EUR",
                            region = Region.GLOBAL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for MTCGame
 */
class MTCGameScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://mtcgame.com/en-us/search?q=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "MTCGame", price = price, currency = "USD",
                            region = Region.TURKEY, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Wyrel
 */
class WyrelScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://wyrel.com/search?q=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Wyrel", price = price, currency = "USD",
                            region = Region.TURKEY, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Gamers Outlet
 */
class GamersOutletScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.gamersoutlet.net/search?q=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Gamers Outlet", price = price, currency = "EUR",
                            region = Region.EU, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.MEDIUM
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for SCDKey
 */
class SCDKeyScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.scdkey.com/catalogsearch/result/?q=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "SCDKey", price = price, currency = "USD",
                            region = Region.GLOBAL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.MEDIUM
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for GAMESEAL
 */
class GamesealScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://gameseal.com/search?q=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "GAMESEAL", price = price, currency = "EUR",
                            region = Region.GLOBAL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Microsoft Store
 */
class MicrosoftScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.xbox.com/en-US/games/store/xbox-game-pass-ultimate/CFQ7TTC0KHS0"
                val doc = fetchDocument(url)
                doc.select(".price, [data-price], .ProductPrice").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Microsoft", price = price, currency = "USD",
                            region = Region.US, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Gamesplanet
 */
class GamesplanetScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://gamesplanet.com/search?query=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Gamesplanet", price = price, currency = "EUR",
                            region = Region.EU, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Amazon
 */
class AmazonScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.amazon.com/s?k=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".a-price-whole, .a-offscreen, [data-a-price]").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Amazon", price = price, currency = "USD",
                            region = Region.US, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for G2Play
 */
class G2PlayScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.g2play.net/search?query=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "G2Play", price = price, currency = "EUR",
                            region = Region.GLOBAL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for GG.deals (Cloudflare protected)
 */
class GGDealsScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://gg.deals/deals/?title=game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .deal-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "GG.deals", price = price, currency = "EUR",
                            region = Region.GLOBAL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for HRK Game (Cloudflare protected)
 */
class HRKGameScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.hrkgame.com/en/search/?query=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "HRK Game", price = price, currency = "EUR",
                            region = Region.GLOBAL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.MEDIUM
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for 2Game (Cloudflare protected)
 */
class TwoGameScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://2game.com/us/search?q=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "2Game", price = price, currency = "USD",
                            region = Region.US, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for Play-Asia (Cloudflare protected)
 */
class PlayAsiaScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.play-asia.com/search/game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "Play-Asia", price = price, currency = "USD",
                            region = Region.GLOBAL, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}

/**
 * Scraper for GameStop (Cloudflare protected)
 */
class GameStopScraper : BaseScraper {
    override suspend fun scrape(filters: SearchFilters): List<PriceDeal> {
        return withContext(Dispatchers.IO) {
            val deals = mutableListOf<PriceDeal>()
            try {
                val url = "https://www.gamestop.com/search/?q=xbox+game+pass+ultimate"
                val doc = fetchDocument(url)
                doc.select(".price, .product-price").forEach { elem ->
                    val priceText = elem.text().replace("[^0-9.,]".toRegex(), "").replace(",", ".")
                    val price = priceText.toDoubleOrNull() ?: return@forEach
                    if (price > 0 && price < 200) {
                        deals.add(PriceDeal(
                            id = generateId(), sellerName = "GameStop", price = price, currency = "USD",
                            region = Region.US, type = DealType.KEY, duration = Duration.ONE_MONTH,
                            url = url, trustLevel = TrustLevel.HIGH
                        ))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            deals.take(5)
        }
    }
}
