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
        trustLevel = TrustLevel.CAUTION,
        description = "Large marketplace - use G2A Shield for protection",
        features = listOf("Huge selection", "Use G2A Shield", "Check seller ratings")
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
        trustLevel = TrustLevel.MEDIUM,
        description = "Marketplace with Smart subscription benefits",
        features = listOf("Smart subscription", "Buyer protection", "Good prices")
    )
    
    /**
     * Get all sellers
     */
    fun getAll(): List<Seller> = listOf(
        CDKeys, Eneba, InstantGaming, GreenManGaming, 
        Kinguin, G2A, HumbleBundle, Gamivo
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
