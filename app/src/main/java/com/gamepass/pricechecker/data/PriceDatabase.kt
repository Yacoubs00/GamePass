package com.gamepass.pricechecker.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room Entity for cached price results
 */
@Entity(tableName = "cached_prices")
data class CachedPrice(
    @PrimaryKey
    val id: String,  // "{seller}_{region}_{duration}"
    
    val sellerName: String,
    val price: Double,
    val currency: String,
    val region: String,
    val duration: String,  // "1_MONTH", "3_MONTHS", etc.
    val productUrl: String,
    val productTitle: String,
    
    val fetchedAt: Long = System.currentTimeMillis(),
    val sourceType: String = "WEBVIEW",  // "WEBVIEW", "JSOUP", "API"
    val isStale: Boolean = false
)

/**
 * Room Entity for tracking fetch status per site
 */
@Entity(tableName = "fetch_status")
data class FetchStatus(
    @PrimaryKey
    val siteName: String,
    
    val lastFetchTime: Long = 0,
    val lastSuccessTime: Long = 0,
    val consecutiveFailures: Int = 0,
    val lastError: String? = null,
    val isBlocked: Boolean = false
)

/**
 * DAO for cached prices
 */
@Dao
interface PriceDao {
    
    @Query("SELECT * FROM cached_prices ORDER BY price ASC")
    fun getAllPrices(): Flow<List<CachedPrice>>
    
    @Query("SELECT * FROM cached_prices WHERE region = :region ORDER BY price ASC")
    fun getPricesByRegion(region: String): Flow<List<CachedPrice>>
    
    @Query("SELECT * FROM cached_prices WHERE sellerName = :seller")
    suspend fun getPricesForSeller(seller: String): List<CachedPrice>
    
    @Query("SELECT * FROM cached_prices WHERE fetchedAt > :since ORDER BY price ASC")
    suspend fun getRecentPrices(since: Long): List<CachedPrice>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrice(price: CachedPrice)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<CachedPrice>)
    
    @Query("UPDATE cached_prices SET isStale = 1 WHERE fetchedAt < :before")
    suspend fun markStale(before: Long)
    
    @Query("DELETE FROM cached_prices WHERE fetchedAt < :before")
    suspend fun deleteOldPrices(before: Long)
    
    @Query("DELETE FROM cached_prices")
    suspend fun clearAll()
}

/**
 * DAO for fetch status tracking
 */
@Dao
interface FetchStatusDao {
    
    @Query("SELECT * FROM fetch_status WHERE siteName = :site")
    suspend fun getStatus(site: String): FetchStatus?
    
    @Query("SELECT * FROM fetch_status")
    suspend fun getAllStatuses(): List<FetchStatus>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStatus(status: FetchStatus)
    
    @Query("UPDATE fetch_status SET consecutiveFailures = consecutiveFailures + 1, lastError = :error WHERE siteName = :site")
    suspend fun recordFailure(site: String, error: String)
    
    @Query("UPDATE fetch_status SET consecutiveFailures = 0, lastSuccessTime = :time, lastError = NULL WHERE siteName = :site")
    suspend fun recordSuccess(site: String, time: Long = System.currentTimeMillis())
    
    @Query("UPDATE fetch_status SET isBlocked = :blocked WHERE siteName = :site")
    suspend fun setBlocked(site: String, blocked: Boolean)
}

/**
 * Room Database
 */
@Database(
    entities = [CachedPrice::class, FetchStatus::class],
    version = 1,
    exportSchema = false
)
abstract class PriceDatabase : RoomDatabase() {
    
    abstract fun priceDao(): PriceDao
    abstract fun fetchStatusDao(): FetchStatusDao
    
    companion object {
        @Volatile
        private var INSTANCE: PriceDatabase? = null
        
        fun getInstance(context: Context): PriceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PriceDatabase::class.java,
                    "price_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
