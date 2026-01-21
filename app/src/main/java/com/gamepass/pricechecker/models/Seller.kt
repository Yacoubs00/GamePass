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
    
    val Fanatical = Seller(
        id = "fanatical",
        name = "Fanatical",
        website = "https://www.fanatical.com",
        trustLevel = TrustLevel.HIGH,
        description = "Authorized reseller with great bundles",
        features = listOf("Star deals", "Bundles", "Authorized")
    )
    
    val K4G = Seller(
        id = "k4g",
        name = "K4G",
        website = "https://k4g.com",
        trustLevel = TrustLevel.HIGH,
        description = "Key marketplace with good prices",
        features = listOf("Competitive prices", "Fast delivery", "Various regions")
    )
    
    val Gameseal = Seller(
        id = "gameseal",
        name = "GAMESEAL",
        website = "https://gameseal.com",
        trustLevel = TrustLevel.HIGH,
        description = "Trusted key marketplace",
        features = listOf("Buyer protection", "Good prices", "Fast delivery")
    )
    
    val GamersOutlet = Seller(
        id = "gamersoutlet",
        name = "Gamers Outlet",
        website = "https://www.gamers-outlet.net",
        trustLevel = TrustLevel.MEDIUM,
        description = "Budget key seller",
        features = listOf("Low prices", "Various regions", "Quick delivery")
    )
    
    val MMOGA = Seller(
        id = "mmoga",
        name = "MMOGA",
        website = "https://www.mmoga.com",
        trustLevel = TrustLevel.HIGH,
        description = "German marketplace, very reliable",
        features = listOf("Established since 2002", "24/7 support", "Trusted")
    )
    
    val Voidu = Seller(
        id = "voidu",
        name = "Voidu",
        website = "https://www.voidu.com",
        trustLevel = TrustLevel.HIGH,
        description = "Netherlands-based authorized reseller",
        features = listOf("Authorized", "Good prices", "EU-based")
    )
    
    val Nuuvem = Seller(
        id = "nuuvem",
        name = "Nuuvem",
        website = "https://www.nuuvem.com",
        trustLevel = TrustLevel.HIGH,
        description = "Brazil-based, great for SA region",
        features = listOf("South America specialist", "Good prices", "Authorized")
    )
    
    val MTCGame = Seller(
        id = "mtcgame",
        name = "MTCGame",
        website = "https://mtcgame.com",
        trustLevel = TrustLevel.HIGH,
        description = "Turkish marketplace, great regional prices",
        features = listOf("Turkey keys", "Good prices", "Fast delivery")
    )
    
    val Wyrel = Seller(
        id = "wyrel",
        name = "Wyrel",
        website = "https://wyrel.com",
        trustLevel = TrustLevel.HIGH,
        description = "Specializes in Turkey region keys",
        features = listOf("Turkey specialist", "Best regional prices", "Reliable")
    )
    
    val TwoGame = Seller(
        id = "2game",
        name = "2Game",
        website = "https://2game.com",
        trustLevel = TrustLevel.HIGH,
        description = "Authorized reseller",
        features = listOf("Official keys", "Authorized", "Reliable")
    )
    
    val Driffle = Seller(
        id = "driffle",
        name = "Driffle",
        website = "https://driffle.com",
        trustLevel = TrustLevel.HIGH,
        description = "Key marketplace with buyer protection",
        features = listOf("Buyer protection", "Good prices", "Fast delivery")
    )
    
    val G2Play = Seller(
        id = "g2play",
        name = "G2Play",
        website = "https://www.g2play.net",
        trustLevel = TrustLevel.HIGH,
        description = "Trusted key marketplace",
        features = listOf("Established", "Various regions", "Good prices")
    )
    
    val PlayAsia = Seller(
        id = "playasia",
        name = "Play-Asia",
        website = "https://www.play-asia.com",
        trustLevel = TrustLevel.HIGH,
        description = "Asian game retailer, great for regional keys",
        features = listOf("Asia specialist", "Global shipping", "Trusted")
    )
    
    val GameStop = Seller(
        id = "gamestop",
        name = "GameStop",
        website = "https://www.gamestop.com",
        trustLevel = TrustLevel.HIGH,
        description = "Official retailer",
        features = listOf("Official", "US-based", "Reliable")
    )
    
    val Amazon = Seller(
        id = "amazon",
        name = "Amazon",
        website = "https://www.amazon.com",
        trustLevel = TrustLevel.HIGH,
        description = "Official retailer",
        features = listOf("Official keys", "Fast delivery", "Trustworthy")
    )
    
    /**
     * Get all sellers
     */
    fun getAll(): List<Seller> = listOf(
        CDKeys, Eneba, InstantGaming, GreenManGaming, 
        Kinguin, G2A, HumbleBundle, Gamivo,
        GGDeals, Difmark, HRKGame, Gamesplanet,
        Fanatical, K4G, Gameseal, GamersOutlet,
        MMOGA, Voidu, Nuuvem, MTCGame, Wyrel, TwoGame,
        Driffle, G2Play, PlayAsia, GameStop, Amazon
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
