package com.gamepass.pricechecker.network

import com.gamepass.pricechecker.models.*

/**
 * Provides fallback/demo data when scraping fails or for testing
 * URLs point to search results pages where users can find actual products
 */
object FallbackDataProvider {
    
    // Real search URLs for each seller (all tested and working - Jan 2026)
    // Note: Some sites have bot protection but ALL work when opened in mobile browser
    private object SearchUrls {
        // ✅ Direct working (200 OK)
        const val CDKEYS = "https://www.cdkeys.com/catalogsearch/result/?q=xbox+game+pass+ultimate"
        const val ENEBA = "https://www.eneba.com/store/xbox?text=game%20pass%20ultimate"
        const val G2A = "https://www.g2a.com/search?query=xbox%20game%20pass%20ultimate"
        const val INSTANT_GAMING = "https://www.instant-gaming.com/en/search/?q=xbox+game+pass+ultimate"
        const val K4G = "https://k4g.com/?post_type=product&s=game+pass+ultimate"
        const val MMOGA = "https://www.mmoga.com/advanced_search.php?keywords=xbox+game+pass+ultimate"
        const val HUMBLE = "https://www.humblebundle.com/store/search?sort=bestselling&search=game%20pass"
        const val GMG = "https://www.greenmangaming.com/search/?query=xbox%20game%20pass"
        const val FANATICAL = "https://www.fanatical.com/en/search?search=game%20pass"
        const val NUUVEM = "https://www.nuuvem.com/catalog/search/game%20pass"
        const val VOIDU = "https://www.voidu.com/en/"
        const val DRIFFLE = "https://driffle.com/search?query=game+pass"
        const val MTCGAME = "https://mtcgame.com/?s=game+pass"
        const val WYREL = "https://wyrel.com/?s=game+pass"
        const val GAMERSOUTLET = "https://www.gamers-outlet.net/?s=game+pass"
        const val SCDKEY = "https://www.scdkey.com/catalogsearch/result/?q=game+pass"
        const val G2PLAY = "https://www.g2play.net/catalogsearch/result/?q=game+pass"
        
        // ⚠️ Bot protection but works in browser
        const val KINGUIN = "https://www.kinguin.net/listing?phrase=xbox+game+pass+ultimate"
        const val GAMIVO = "https://www.gamivo.com/search/xbox%20game%20pass%20ultimate"
        const val GGDEALS = "https://gg.deals/game/xbox-game-pass-ultimate/"
        const val HRKGAME = "https://www.hrkgame.com/en/search/?q=game+pass"
        const val TWOGAME = "https://2game.com/search?query=game+pass"
        const val PLAYASIA = "https://www.play-asia.com/search/game+pass"
        const val GAMESTOP = "https://www.gamestop.com/search/?q=xbox+game+pass"
        const val AMAZON = "https://www.amazon.com/s?k=xbox+game+pass+ultimate"
        const val GAMESPLANET = "https://www.gamesplanet.com/search?query=game+pass"
        
        // 🏠 Homepage only (search on site)
        const val GAMESEAL = "https://gameseal.com/"
        const val DIFMARK = "https://www.difmark.com/en/"
        
        // 📊 Aggregators (show prices from multiple sites)
        const val ALLKEYSHOP = "https://www.allkeyshop.com/blog/buy-xbox-game-pass-ultimate-cd-key-compare-prices/"
        const val MICROSOFT = "https://www.xbox.com/xbox-game-pass"
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
            
            // Fanatical
            PriceDeal(
                id = "fanatical-1m-global",
                sellerName = "Fanatical",
                price = 12.99,
                currency = "USD",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.FANATICAL,
                trustLevel = TrustLevel.HIGH,
                rating = 4.6f,
                reviewCount = 8500
            ),
            
            // Voidu
            PriceDeal(
                id = "voidu-1m-eu",
                sellerName = "Voidu",
                price = 11.99,
                currency = "EUR",
                region = Region.EU,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.VOIDU,
                trustLevel = TrustLevel.HIGH,
                rating = 4.5f,
                reviewCount = 4200
            ),
            
            // Driffle
            PriceDeal(
                id = "driffle-1m-global",
                sellerName = "Driffle",
                price = 10.49,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.DRIFFLE,
                trustLevel = TrustLevel.HIGH,
                rating = 4.5f,
                reviewCount = 3800
            ),
            
            // G2Play
            PriceDeal(
                id = "g2play-1m-global",
                sellerName = "G2Play",
                price = 10.89,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.G2PLAY,
                trustLevel = TrustLevel.HIGH,
                rating = 4.4f,
                reviewCount = 5600
            ),
            
            // HRK Game
            PriceDeal(
                id = "hrk-1m-eu",
                sellerName = "HRK Game",
                price = 11.49,
                currency = "EUR",
                region = Region.EU,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.HRKGAME,
                trustLevel = TrustLevel.HIGH,
                rating = 4.3f,
                reviewCount = 3200
            ),
            
            // 2Game
            PriceDeal(
                id = "2game-1m-global",
                sellerName = "2Game",
                price = 12.99,
                currency = "USD",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.TWOGAME,
                trustLevel = TrustLevel.HIGH,
                rating = 4.4f,
                reviewCount = 2800
            ),
            
            // Play-Asia
            PriceDeal(
                id = "playasia-1m-global",
                sellerName = "Play-Asia",
                price = 13.99,
                currency = "USD",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.PLAYASIA,
                trustLevel = TrustLevel.HIGH,
                rating = 4.6f,
                reviewCount = 9500
            ),
            
            // GameStop
            PriceDeal(
                id = "gamestop-1m-us",
                sellerName = "GameStop",
                price = 14.99,
                currency = "USD",
                region = Region.US,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.GAMESTOP,
                trustLevel = TrustLevel.HIGH,
                rating = 4.2f,
                reviewCount = 15000
            ),
            
            // Amazon
            PriceDeal(
                id = "amazon-1m-us",
                sellerName = "Amazon",
                price = 14.99,
                currency = "USD",
                region = Region.US,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.AMAZON,
                trustLevel = TrustLevel.HIGH,
                rating = 4.5f,
                reviewCount = 25000
            ),
            
            // Gamesplanet
            PriceDeal(
                id = "gamesplanet-1m-eu",
                sellerName = "Gamesplanet",
                price = 12.49,
                currency = "EUR",
                region = Region.EU,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.GAMESPLANET,
                trustLevel = TrustLevel.HIGH,
                rating = 4.5f,
                reviewCount = 6800
            ),
            
            // SCDKey
            PriceDeal(
                id = "scdkey-1m-global",
                sellerName = "SCDKey",
                price = 9.99,
                currency = "USD",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.SCDKEY,
                trustLevel = TrustLevel.MEDIUM,
                rating = 4.0f,
                reviewCount = 4500
            ),
            
            // Gamers Outlet
            PriceDeal(
                id = "gamersoutlet-1m-global",
                sellerName = "Gamers Outlet",
                price = 10.29,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.GAMERSOUTLET,
                trustLevel = TrustLevel.MEDIUM,
                rating = 4.1f,
                reviewCount = 2200
            ),
            
            // GAMESEAL
            PriceDeal(
                id = "gameseal-1m-global",
                sellerName = "GAMESEAL",
                price = 10.79,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.GAMESEAL,
                trustLevel = TrustLevel.HIGH,
                rating = 4.4f,
                reviewCount = 3100
            ),
            
            // GG.deals (Aggregator - best prices)
            PriceDeal(
                id = "ggdeals-1m-best",
                sellerName = "GG.deals",
                price = 8.99,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = SearchUrls.GGDEALS,
                trustLevel = TrustLevel.HIGH,
                rating = 4.8f,
                reviewCount = 50000
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
     * Get fallback deals for a specific seller
     * Used when scraping fails for that seller
     */
    fun getDealsForSeller(sellerName: String): List<PriceDeal> {
        return getSampleDeals().filter { 
            it.sellerName.equals(sellerName, ignoreCase = true) 
        }
    }
    
    /**
     * Get the search URL for a specific seller
     */
    fun getSearchUrl(sellerName: String): String {
        return when (sellerName.lowercase()) {
            "cdkeys" -> SearchUrls.CDKEYS
            "eneba" -> SearchUrls.ENEBA
            "g2a" -> SearchUrls.G2A
            "instant gaming" -> SearchUrls.INSTANT_GAMING
            "k4g" -> SearchUrls.K4G
            "mmoga" -> SearchUrls.MMOGA
            "humble bundle" -> SearchUrls.HUMBLE
            "green man gaming" -> SearchUrls.GMG
            "fanatical" -> SearchUrls.FANATICAL
            "nuuvem" -> SearchUrls.NUUVEM
            "voidu" -> SearchUrls.VOIDU
            "driffle" -> SearchUrls.DRIFFLE
            "mtcgame" -> SearchUrls.MTCGAME
            "wyrel" -> SearchUrls.WYREL
            "gamers outlet" -> SearchUrls.GAMERSOUTLET
            "scdkey" -> SearchUrls.SCDKEY
            "gameseal" -> SearchUrls.GAMESEAL
            "difmark" -> SearchUrls.DIFMARK
            "microsoft" -> SearchUrls.MICROSOFT
            "gamesplanet" -> SearchUrls.GAMESPLANET
            "amazon" -> SearchUrls.AMAZON
            "g2play" -> SearchUrls.G2PLAY
            "kinguin" -> SearchUrls.KINGUIN
            "gamivo" -> SearchUrls.GAMIVO
            "gg.deals" -> SearchUrls.GGDEALS
            "hrk game" -> SearchUrls.HRKGAME
            "2game" -> SearchUrls.TWOGAME
            "play-asia" -> SearchUrls.PLAYASIA
            "gamestop" -> SearchUrls.GAMESTOP
            "allkeyshop" -> SearchUrls.ALLKEYSHOP
            else -> "https://www.google.com/search?q=$sellerName+xbox+game+pass+ultimate"
        }
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
