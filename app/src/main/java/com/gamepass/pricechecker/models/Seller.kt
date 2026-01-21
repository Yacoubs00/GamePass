package com.gamepass.pricechecker.models

/**
 * Represents a key seller/retailer
 */
data class Seller(
    val id: String,
    val name: String,
    val website: String,
    val logoUrl: String? = null,
    val trustLevel: TrustLevel,
    val description: String,
    val features: List<String> = emptyList()
)

/**
 * Pre-defined list of reputable sellers
 */
object Sellers {
    
    val CDKeys = Seller(
        id = "cdkeys",
        name = "CDKeys",
        website = "https://www.cdkeys.com",
        trustLevel = TrustLevel.HIGH,
        description = "Well-established digital key retailer with excellent reputation",
        features = listOf("Instant delivery", "24/7 support", "Money-back guarantee")
    )
    
    val Eneba = Seller(
        id = "eneba",
        name = "Eneba",
        website = "https://www.eneba.com",
        trustLevel = TrustLevel.HIGH,
        description = "Large marketplace with buyer protection",
        features = listOf("Buyer protection", "Multiple sellers", "Competitive prices")
    )
    
    val InstantGaming = Seller(
        id = "instant_gaming",
        name = "Instant Gaming",
        website = "https://www.instant-gaming.com",
        trustLevel = TrustLevel.HIGH,
        description = "European-based trusted retailer",
        features = listOf("Fast delivery", "Good prices", "Reliable")
    )
    
    val GreenManGaming = Seller(
        id = "gmg",
        name = "Green Man Gaming",
        website = "https://www.greenmangaming.com",
        trustLevel = TrustLevel.HIGH,
        description = "Authorized official reseller",
        features = listOf("Official partner", "XP rewards", "Trustworthy")
    )
    
    val Kinguin = Seller(
        id = "kinguin",
        name = "Kinguin",
        website = "https://www.kinguin.net",
        trustLevel = TrustLevel.MEDIUM,
        description = "Marketplace with buyer protection available",
        features = listOf("Buyer protection available", "Large selection", "Competitive")
    )
    
    val G2A = Seller(
        id = "g2a",
        name = "G2A",
        website = "https://www.g2a.com",
        trustLevel = TrustLevel.HIGH,
        description = "Large trusted marketplace with buyer protection",
        features = listOf("Huge selection", "G2A Shield protection", "Great prices")
    )
    
    val HumbleBundle = Seller(
        id = "humble",
        name = "Humble Bundle",
        website = "https://www.humblebundle.com",
        trustLevel = TrustLevel.HIGH,
        description = "Official partner, supports charity",
        features = listOf("Official keys", "Charity support", "Humble Choice")
    )
    
    val Gamivo = Seller(
        id = "gamivo",
        name = "Gamivo",
        website = "https://www.gamivo.com",
        trustLevel = TrustLevel.HIGH,
        description = "Marketplace with Smart subscription benefits",
        features = listOf("Smart subscription", "Buyer protection", "Good prices")
    )
    
    val GGDeals = Seller(
        id = "ggdeals",
        name = "GG.deals",
        website = "https://gg.deals",
        trustLevel = TrustLevel.HIGH,
        description = "Price aggregator with historical data",
        features = listOf("Price tracking", "Deal alerts", "Price history")
    )
    
    val Difmark = Seller(
        id = "difmark",
        name = "Difmark",
        website = "https://www.difmark.com",
        trustLevel = TrustLevel.HIGH,
        description = "Great for regional keys (Turkey, Brazil, Argentina)",
        features = listOf("Regional keys", "Good prices", "Fast delivery")
    )
    
    val HRKGame = Seller(
        id = "hrkgame",
        name = "HRK Game",
        website = "https://www.hrkgame.com",
        trustLevel = TrustLevel.HIGH,
        description = "European retailer with competitive prices",
        features = listOf("EU-based", "Good prices", "Reliable")
    )
    
    val Gamesplanet = Seller(
        id = "gamesplanet",
        name = "Gamesplanet",
        website = "https://www.gamesplanet.com",
        trustLevel = TrustLevel.HIGH,
        description = "UK/EU authorized retailer",
        features = listOf("Authorized reseller", "Star deals", "Trustworthy")
    )
    
    val SCDKey = Seller(
        id = "scdkey",
        name = "SCDKey",
        website = "https://www.scdkey.com",
        trustLevel = TrustLevel.MEDIUM,
        description = "Budget-friendly key seller",
        features = listOf("Low prices", "Various regions", "Quick delivery")
    )
    
    val Cdkeys_com = Seller(
        id = "cdkeyscom",
        name = "CDKeys.com",
        website = "https://www.cdkeys.com",
        trustLevel = TrustLevel.HIGH,
        description = "Popular trusted retailer",
        features = listOf("Instant delivery", "Great prices", "Reliable")
    )
    
    /**
     * Get all sellers
     */
    fun getAll(): List<Seller> = listOf(
        CDKeys, Eneba, InstantGaming, GreenManGaming, 
        Kinguin, G2A, HumbleBundle, Gamivo,
        GGDeals, Difmark, HRKGame, Gamesplanet, SCDKey
    )
    
    /**
     * Get seller by ID
     */
    fun getById(id: String): Seller? = getAll().find { it.id == id }
    
    /**
     * Get only highly trusted sellers
     */
    fun getTrusted(): List<Seller> = getAll().filter { 
        it.trustLevel == TrustLevel.HIGH 
    }
}
