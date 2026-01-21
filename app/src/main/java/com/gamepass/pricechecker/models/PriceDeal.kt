package com.gamepass.pricechecker.models

/**
 * Represents a single Game Pass deal from a seller
 */
data class PriceDeal(
    val id: String,
    val sellerName: String,
    val sellerLogo: String? = null,
    val price: Double,
    val currency: String,
    val originalPrice: Double? = null,
    val discount: Int? = null,
    val region: Region,
    val type: DealType,
    val duration: Duration,
    val url: String,
    val trustLevel: TrustLevel,
    val rating: Float? = null,
    val reviewCount: Int? = null,
    val inStock: Boolean = true,
    val isTrial: Boolean = false,  // Flag for trial offers
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * Get formatted price string
     */
    fun getFormattedPrice(): String {
        return when (currency) {
            "USD" -> "$${String.format("%.2f", price)}"
            "EUR" -> "€${String.format("%.2f", price)}"
            "GBP" -> "£${String.format("%.2f", price)}"
            "AED" -> "AED ${String.format("%.2f", price)}"
            "TRY" -> "₺${String.format("%.2f", price)}"
            "BRL" -> "R$${String.format("%.2f", price)}"
            "INR" -> "₹${String.format("%.2f", price)}"
            else -> "$currency ${String.format("%.2f", price)}"
        }
    }
    
    /**
     * Get formatted original price string (for showing discounts)
     */
    fun getFormattedOriginalPrice(): String? {
        return originalPrice?.let {
            when (currency) {
                "USD" -> "$${String.format("%.2f", it)}"
                "EUR" -> "€${String.format("%.2f", it)}"
                "GBP" -> "£${String.format("%.2f", it)}"
                "AED" -> "AED ${String.format("%.2f", it)}"
                else -> "$currency ${String.format("%.2f", it)}"
            }
        }
    }
}

/**
 * Available regions for Game Pass keys
 */
enum class Region(val displayName: String, val code: String) {
    ALL("All Regions", "all"),
    GLOBAL("Global", "global"),
    UAE("UAE", "ae"),
    US("United States", "us"),
    UK("United Kingdom", "uk"),
    EU("Europe", "eu"),
    TURKEY("Turkey", "tr"),
    BRAZIL("Brazil", "br"),
    ARGENTINA("Argentina", "ar"),
    INDIA("India", "in")
}

/**
 * Type of deal - key or account
 */
enum class DealType(val displayName: String) {
    ALL("All Types"),
    KEY("Key"),
    ACCOUNT("Account")
}

/**
 * Subscription duration
 */
enum class Duration(val displayName: String, val months: Int) {
    ALL("All Durations", 0),
    ONE_MONTH("1 Month", 1),
    THREE_MONTHS("3 Months", 3),
    SIX_MONTHS("6 Months", 6),
    TWELVE_MONTHS("12 Months", 12)
}

/**
 * Trust level of the seller
 */
enum class TrustLevel(val displayName: String, val colorRes: String) {
    HIGH("Highly Trusted", "trust_high"),
    MEDIUM("Trusted", "trust_medium"),
    CAUTION("Use Caution", "trust_caution")
}
