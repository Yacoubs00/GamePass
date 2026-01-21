package com.gamepass.pricechecker.models

/**
 * Holds the current search filter state
 */
data class SearchFilters(
    val region: Region = Region.ALL,
    val type: DealType = DealType.ALL,
    val duration: Duration = Duration.ALL,
    val sortBy: SortOption = SortOption.PRICE_LOW,
    val trustedOnly: Boolean = false,
    val excludeTrials: Boolean = true  // Exclude trials by default
) {
    /**
     * Check if a deal matches these filters
     */
    fun matches(deal: PriceDeal): Boolean {
        // Region filter
        if (region != Region.ALL && deal.region != region && deal.region != Region.GLOBAL) {
            return false
        }
        
        // Type filter
        if (type != DealType.ALL && deal.type != type) {
            return false
        }
        
        // Duration filter
        if (duration != Duration.ALL && deal.duration != duration) {
            return false
        }
        
        // Trusted only filter
        if (trustedOnly && deal.trustLevel != TrustLevel.HIGH) {
            return false
        }
        
        // Exclude trials filter
        if (excludeTrials && deal.isTrial) {
            return false
        }
        
        return true
    }
    
    /**
     * Get a description of active filters
     */
    fun getActiveFiltersDescription(): String {
        val parts = mutableListOf<String>()
        
        if (region != Region.ALL) parts.add(region.displayName)
        if (type != DealType.ALL) parts.add(type.displayName)
        if (duration != Duration.ALL) parts.add(duration.displayName)
        if (trustedOnly) parts.add("Trusted Only")
        
        return if (parts.isEmpty()) "No filters" else parts.joinToString(" • ")
    }
}

/**
 * Sort options for results
 */
enum class SortOption(val displayName: String) {
    PRICE_LOW("Price: Low to High"),
    PRICE_HIGH("Price: High to Low"),
    RATING("Best Rating"),
    TRUST("Most Trusted")
}
