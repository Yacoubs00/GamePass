package com.gamepass.pricechecker.models

/**
 * Wrapper for search results
 */
sealed class SearchResult {
    /**
     * Loading state
     */
    object Loading : SearchResult()
    
    /**
     * Success with list of deals
     */
    data class Success(
        val deals: List<PriceDeal>,
        val totalFound: Int,
        val searchTimeMs: Long,
        val lowestPrice: PriceDeal? = deals.minByOrNull { it.price },
        val sourcesSearched: Int
    ) : SearchResult()
    
    /**
     * Error state
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : SearchResult()
    
    /**
     * Empty state (no results found)
     */
    object Empty : SearchResult()
}

/**
 * Statistics about the search results
 */
data class SearchStats(
    val totalDeals: Int,
    val lowestPrice: Double?,
    val highestPrice: Double?,
    val averagePrice: Double?,
    val currency: String,
    val sellersCount: Int
) {
    companion object {
        fun fromDeals(deals: List<PriceDeal>): SearchStats? {
            if (deals.isEmpty()) return null
            
            val prices = deals.map { it.price }
            val primaryCurrency = deals.groupBy { it.currency }
                .maxByOrNull { it.value.size }?.key ?: "USD"
            
            return SearchStats(
                totalDeals = deals.size,
                lowestPrice = prices.minOrNull(),
                highestPrice = prices.maxOrNull(),
                averagePrice = prices.average(),
                currency = primaryCurrency,
                sellersCount = deals.map { it.sellerName }.distinct().size
            )
        }
    }
}
