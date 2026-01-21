package com.gamepass.pricechecker.network

import com.gamepass.pricechecker.models.*

/**
 * Provides fallback/demo data when scraping fails or for testing
 * Also serves as a reference for typical prices
 */
object FallbackDataProvider {
    
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
                url = "https://www.cdkeys.com/xbox-live/memberships/xbox-game-pass-ultimate-1-month",
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
                url = "https://www.cdkeys.com/xbox-live/memberships/xbox-game-pass-ultimate-3-months",
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
                url = "https://www.eneba.com/xbox-xbox-game-pass-ultimate-1-month",
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
                url = "https://www.eneba.com/xbox-xbox-game-pass-ultimate-1-month-turkey",
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
                url = "https://www.instant-gaming.com/en/xbox-game-pass-ultimate/",
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
                url = "https://www.kinguin.net/xbox-game-pass-ultimate",
                trustLevel = TrustLevel.MEDIUM,
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
                url = "https://www.kinguin.net/xbox-game-pass-ultimate-3-months",
                trustLevel = TrustLevel.MEDIUM,
                rating = 4.1f,
                reviewCount = 3420
            ),
            
            // G2A (with caution)
            PriceDeal(
                id = "g2a-1m-global",
                sellerName = "G2A",
                price = 9.89,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = "https://www.g2a.com/xbox-game-pass-ultimate",
                trustLevel = TrustLevel.CAUTION,
                rating = 4.0f,
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
                url = "https://www.g2a.com/xbox-game-pass-ultimate-brazil",
                trustLevel = TrustLevel.CAUTION,
                rating = 3.9f,
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
                url = "https://www.gamivo.com/product/xbox-game-pass-ultimate-1-month",
                trustLevel = TrustLevel.MEDIUM,
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
                url = "https://www.eneba.com/xbox-xbox-game-pass-ultimate-1-month-uae",
                trustLevel = TrustLevel.HIGH,
                rating = 4.5f,
                reviewCount = 890
            ),
            PriceDeal(
                id = "cdkeys-3m-uae",
                sellerName = "CDKeys",
                price = 125.00,
                currency = "AED",
                region = Region.UAE,
                type = DealType.KEY,
                duration = Duration.THREE_MONTHS,
                url = "https://www.cdkeys.com/xbox-game-pass-ultimate-3-months-uae",
                trustLevel = TrustLevel.HIGH,
                rating = 4.6f,
                reviewCount = 450
            ),
            
            // Account type deals (some sellers offer these)
            PriceDeal(
                id = "g2a-1m-account",
                sellerName = "G2A",
                price = 4.99,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.ACCOUNT,
                duration = Duration.ONE_MONTH,
                url = "https://www.g2a.com/xbox-game-pass-ultimate-account",
                trustLevel = TrustLevel.HIGH,
                rating = 3.5f,
                reviewCount = 1200
            ),
            
            // New providers
            PriceDeal(
                id = "k4g-1m-global",
                sellerName = "K4G",
                price = 10.99,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = "https://k4g.com/product/xbox-game-pass-ultimate",
                trustLevel = TrustLevel.HIGH,
                rating = 4.5f,
                reviewCount = 3200
            ),
            PriceDeal(
                id = "gameseal-1m-global",
                sellerName = "GAMESEAL",
                price = 11.29,
                currency = "EUR",
                region = Region.GLOBAL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = "https://gameseal.com/xbox-game-pass-ultimate",
                trustLevel = TrustLevel.HIGH,
                rating = 4.4f,
                reviewCount = 2100
            ),
            PriceDeal(
                id = "mmoga-1m-eu",
                sellerName = "MMOGA",
                price = 12.49,
                currency = "EUR",
                region = Region.EU,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = "https://www.mmoga.com/Xbox/Xbox-Game-Pass-Ultimate.html",
                trustLevel = TrustLevel.HIGH,
                rating = 4.6f,
                reviewCount = 8900
            ),
            PriceDeal(
                id = "mtcgame-1m-turkey",
                sellerName = "MTCGame",
                price = 6.99,
                currency = "EUR",
                region = Region.TURKEY,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = "https://mtcgame.com/xbox-game-pass-ultimate-turkey",
                trustLevel = TrustLevel.HIGH,
                rating = 4.3f,
                reviewCount = 1500
            ),
            PriceDeal(
                id = "wyrel-1m-turkey",
                sellerName = "Wyrel",
                price = 6.49,
                currency = "EUR",
                region = Region.TURKEY,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = "https://wyrel.com/xbox-game-pass-ultimate-turkey",
                trustLevel = TrustLevel.HIGH,
                rating = 4.4f,
                reviewCount = 980
            ),
            PriceDeal(
                id = "nuuvem-1m-brazil",
                sellerName = "Nuuvem",
                price = 5.99,
                currency = "EUR",
                region = Region.BRAZIL,
                type = DealType.KEY,
                duration = Duration.ONE_MONTH,
                url = "https://www.nuuvem.com/xbox-game-pass-ultimate-brazil",
                trustLevel = TrustLevel.HIGH,
                rating = 4.5f,
                reviewCount = 2300
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
                url = "https://www.cdkeys.com/xbox-game-pass-ultimate-14-day-trial",
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
                url = "https://www.eneba.com/xbox-game-pass-ultimate-7-day-trial",
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
                url = "https://www.g2a.com/xbox-game-pass-ultimate-14-day-trial",
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
                url = "https://www.kinguin.net/xbox-game-pass-ultimate-trial",
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
            url = "https://www.xbox.com/en-US/xbox-game-pass",
            trustLevel = TrustLevel.HIGH,
            rating = 5.0f,
            reviewCount = 0
        )
    }
}
