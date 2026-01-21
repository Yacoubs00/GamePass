package com.gamepass.pricechecker.network

import com.gamepass.pricechecker.models.*

/**
 * Provides fallback/demo data when scraping fails or for testing
 * URLs point to search results pages where users can find actual products
 */
object FallbackDataProvider {
    
    // Real search URLs for each seller
    private object SearchUrls {
        const val CDKEYS = "https://www.cdkeys.com/catalogsearch/result/?q=xbox+game+pass+ultimate"
        const val ENEBA = "https://www.eneba.com/store/xbox?text=game%20pass%20ultimate"
        const val G2A = "https://www.g2a.com/search?query=xbox%20game%20pass%20ultimate"
        const val KINGUIN = "https://www.kinguin.net/listing?phrase=xbox+game+pass+ultimate"
        const val GAMIVO = "https://www.gamivo.com/search/xbox%20game%20pass%20ultimate"
        const val INSTANT_GAMING = "https://www.instant-gaming.com/en/search/?q=xbox+game+pass+ultimate"
        const val K4G = "https://k4g.com/search?search=xbox+game+pass+ultimate"
        const val GAMESEAL = "https://gameseal.com/search?q=game+pass+ultimate"
        const val MMOGA = "https://www.mmoga.com/advanced_search.php?keywords=xbox+game+pass+ultimate"
        const val DIFMARK = "https://www.difmark.com/search?q=xbox+game+pass+ultimate"
        const val ALLKEYSHOP = "https://www.allkeyshop.com/blog/buy-xbox-game-pass-ultimate-cd-key-compare-prices/"
        const val HUMBLE = "https://www.humblebundle.com/store/search?sort=bestselling&search=game%20pass"
        const val GMG = "https://www.greenmangaming.com/search/?query=xbox%20game%20pass"
        const val FANATICAL = "https://www.fanatical.com/en/search?search=game%20pass"
        const val NUUVEM = "https://www.nuuvem.com/catalog/search/game%20pass"
        const val VOIDU = "https://www.voidu.com/en/search?q=game+pass"
        const val MICROSOFT = "https://www.xbox.com/en-US/xbox-game-pass/ultimate"
    }
    
    /**
     * Get sample deals for demonstration/fallback
     * These represent typical price ranges you'd find
     */
    fun getSampleDeals(): List<PriceDeal> {
        return listOf(
            // CDKeys deals
            PriceDeal(
                id = "cdkeys-1m-global",
                sellerName = "CDKeys",
                price = 12.99,
                currency = "USD",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.CDKEYS,
                trustLevel = TrustLevel.HIGH,
                rating = 4.7f,
                reviewCount = 15420
            ),
            PriceDeal(
                id = "cdkeys-3m-global",
                sellerName = "CDKeys",
                price = 32.99,
                currency = "USD",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.THREE_MONTHS,
                url = SearchUrls.CDKEYS,
                trustLevel = TrustLevel.HIGH,
                rating = 4.7f,
                reviewCount = 8920
            ),
            
            // Eneba deals
            PriceDeal(
                id = "eneba-1m-global",
                sellerName = "Eneba",
                price = 11.49,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.ENEBA,
                trustLevel = TrustLevel.HIGH,
                rating = 4.5f,
                reviewCount = 12300
            ),
            PriceDeal(
                id = "eneba-1m-turkey",
                sellerName = "Eneba",
                price = 7.99,
                currency = "EUR",
                region = Region.TURKEY,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.ENEBA,
                trustLevel = TrustLevel.HIGH,
                rating = 4.4f,
                reviewCount = 5670
            ),
            
            // Instant Gaming
            PriceDeal(
                id = "ig-1m-eu",
                sellerName = "Instant Gaming",
                price = 12.29,
                currency = "EUR",
                region = Region.EU,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.INSTANT_GAMING,
                trustLevel = TrustLevel.HIGH,
                rating = 4.6f,
                reviewCount = 9800
            ),
            
            // Kinguin
            PriceDeal(
                id = "kinguin-1m-global",
                sellerName = "Kinguin",
                price = 10.99,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.KINGUIN,
                trustLevel = TrustLevel.HIGH,
                rating = 4.2f,
                reviewCount = 7650
            ),
            PriceDeal(
                id = "kinguin-3m-global",
                sellerName = "Kinguin",
                price = 28.99,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.THREE_MONTHS,
                url = SearchUrls.KINGUIN,
                trustLevel = TrustLevel.HIGH,
                rating = 4.1f,
                reviewCount = 3420
            ),
            
            // G2A
            PriceDeal(
                id = "g2a-1m-global",
                sellerName = "G2A",
                price = 9.89,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.G2A,
                trustLevel = TrustLevel.HIGH,
                rating = 4.5f,
                reviewCount = 25000
            ),
            PriceDeal(
                id = "g2a-1m-brazil",
                sellerName = "G2A",
                price = 5.99,
                currency = "EUR",
                region = Region.BRAZIL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.G2A,
                trustLevel = TrustLevel.HIGH,
                rating = 4.4f,
                reviewCount = 4200
            ),
            
            // Gamivo
            PriceDeal(
                id = "gamivo-1m-global",
                sellerName = "Gamivo",
                price = 11.29,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.GAMIVO,
                trustLevel = TrustLevel.HIGH,
                rating = 4.3f,
                reviewCount = 6780
            ),
            
            // UAE specific deals
            PriceDeal(
                id = "eneba-1m-uae",
                sellerName = "Eneba",
                price = 45.00,
                currency = "AED",
                region = Region.UAE,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.ENEBA,
                trustLevel = TrustLevel.HIGH,
                rating = 4.5f,
                reviewCount = 890
            ),
            PriceDeal(
                id = "g2a-1m-uae",
                sellerName = "G2A",
                price = 42.00,
                currency = "AED",
                region = Region.UAE,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.G2A,
                trustLevel = TrustLevel.HIGH,
                rating = 4.4f,
                reviewCount = 650
            ),
            PriceDeal(
                id = "cdkeys-3m-uae",
                sellerName = "CDKeys",
                price = 125.00,
                currency = "AED",
                region = Region.UAE,
                type = DealType.KEY,
                duration = Duration.THREE_MONTHS,
                url = SearchUrls.CDKEYS,
                trustLevel = TrustLevel.HIGH,
                rating = 4.6f,
                reviewCount = 450
            ),
            
            // AllKeyShop (Aggregator)
            PriceDeal(
                id = "allkeyshop-1m-global",
                sellerName = "AllKeyShop",
                price = 9.50,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.ALLKEYSHOP,
                trustLevel = TrustLevel.HIGH,
                rating = 4.6f,
                reviewCount = 50000
            ),
            
            // K4G
            PriceDeal(
                id = "k4g-1m-global",
                sellerName = "K4G",
                price = 10.99,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.K4G,
                trustLevel = TrustLevel.HIGH,
                rating = 4.5f,
                reviewCount = 3200
            ),
            
            // GAMESEAL
            PriceDeal(
                id = "gameseal-1m-global",
                sellerName = "GAMESEAL",
                price = 11.29,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.GAMESEAL,
                trustLevel = TrustLevel.HIGH,
                rating = 4.4f,
                reviewCount = 2100
            ),
            
            // MMOGA
            PriceDeal(
                id = "mmoga-1m-eu",
                sellerName = "MMOGA",
                price = 12.49,
                currency = "EUR",
                region = Region.EU,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.MMOGA,
                trustLevel = TrustLevel.HIGH,
                rating = 4.6f,
                reviewCount = 8900
            ),
            
            // Difmark - Turkey specialist
            PriceDeal(
                id = "difmark-1m-turkey",
                sellerName = "Difmark",
                price = 6.99,
                currency = "EUR",
                region = Region.TURKEY,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.DIFMARK,
                trustLevel = TrustLevel.HIGH,
                rating = 4.3f,
                reviewCount = 1500
            ),
            PriceDeal(
                id = "difmark-1m-brazil",
                sellerName = "Difmark",
                price = 5.99,
                currency = "EUR",
                region = Region.BRAZIL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.DIFMARK,
                trustLevel = TrustLevel.HIGH,
                rating = 4.3f,
                reviewCount = 980
            ),
            
            // Humble Bundle
            PriceDeal(
                id = "humble-1m-global",
                sellerName = "Humble Bundle",
                price = 14.99,
                currency = "USD",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.HUMBLE,
                trustLevel = TrustLevel.HIGH,
                rating = 4.8f,
                reviewCount = 15000
            ),
            
            // Green Man Gaming
            PriceDeal(
                id = "gmg-1m-global",
                sellerName = "Green Man Gaming",
                price = 13.99,
                currency = "USD",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.GMG,
                trustLevel = TrustLevel.HIGH,
                rating = 4.7f,
                reviewCount = 12000
            ),
            
            // TRIAL OFFERS (will be filtered out by default)
            PriceDeal(
                id = "cdkeys-14d-trial",
                sellerName = "CDKeys",
                price = 1.99,
                currency = "USD",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.CDKEYS,
                trustLevel = TrustLevel.HIGH,
                rating = 4.2f,
                reviewCount = 5600,
                isTrial = true
            ),
            PriceDeal(
                id = "eneba-7d-trial",
                sellerName = "Eneba",
                price = 0.99,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.ENEBA,
                trustLevel = TrustLevel.HIGH,
                rating = 4.0f,
                reviewCount = 3400,
                isTrial = true
            ),
            PriceDeal(
                id = "g2a-trial-14d",
                sellerName = "G2A",
                price = 1.49,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.G2A,
                trustLevel = TrustLevel.HIGH,
                rating = 4.1f,
                reviewCount = 7800,
                isTrial = true
            ),
            PriceDeal(
                id = "kinguin-trial",
                sellerName = "Kinguin",
                price = 1.29,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.KINGUIN,
                trustLevel = TrustLevel.HIGH,
                rating = 4.0f,
                reviewCount = 2100,
                isTrial = true
            )
        )
    }
    
    /**
     * Get the official Microsoft price for reference
     */
    fun getOfficialPrice(region: Region): PriceDeal {
        val (price, currency) = when (region) {
            Region.US -> 17.99 to "USD"
            Region.UK -> 14.99 to "GBP"
            Region.EU -> 14.99 to "EUR"
            Region.UAE -> 55.00 to "AED"
            Region.TURKEY -> 129.00 to "TRY"
            Region.BRAZIL -> 49.99 to "BRL"
            Region.INDIA -> 699.00 to "INR"
            Region.ARGENTINA -> 2199.00 to "ARS"
            else -> 17.99 to "USD"
        }
        
        return PriceDeal(
            id = "official-ms-${region.code}",
            sellerName = "Microsoft Store (Official)",
            price = price,
            currency = currency,
            region = region,
            type = DealType.KEY,
            duration = Duration.ONE_MONTH,
            url = SearchUrls.MICROSOFT,
            trustLevel = TrustLevel.HIGH,
            rating = 5.0f,
            reviewCount = 0
        )
    }
}
