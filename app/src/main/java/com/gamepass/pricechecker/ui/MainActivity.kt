package com.gamepass.pricechecker.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gamepass.pricechecker.R
import com.gamepass.pricechecker.adapters.DealsAdapter
import com.gamepass.pricechecker.adapters.FilterOptionsAdapter
import com.gamepass.pricechecker.models.*
import com.gamepass.pricechecker.models.TrustFilter
import com.gamepass.pricechecker.network.FallbackDataProvider
import com.gamepass.pricechecker.network.PriceScraper
import com.gamepass.pricechecker.service.PriceFetchService
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    // Views
    private lateinit var recyclerDeals: RecyclerView
    private lateinit var layoutLoading: LinearLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutError: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var tvResultCount: TextView
    private lateinit var btnSearch: MaterialButton
    private lateinit var btnRetry: MaterialButton
    private lateinit var swipeRefresh: SwipeRefreshLayout
    
    // Filter chips
    private lateinit var chipRegion: Chip
    private lateinit var chipDuration: Chip
    private lateinit var chipType: Chip
    private lateinit var chipTrustLevel: Chip
    private lateinit var chipExcludeTrials: Chip
    
    // Theme toggle and info button
    private lateinit var btnThemeToggle: ImageButton
    private lateinit var btnInfo: ImageButton
    
    // Progress views for streaming results
    private lateinit var layoutProgress: LinearLayout
    private lateinit var tvCurrentSite: TextView
    private lateinit var tvFoundCount: TextView
    private lateinit var progressBar: com.google.android.material.progressindicator.LinearProgressIndicator
    private lateinit var tvProgressStatus: TextView

    // Adapter and data
    private lateinit var dealsAdapter: DealsAdapter
    private lateinit var priceScraper: PriceScraper
    private val gson = Gson()
    
    // Broadcast receiver for WebView-based scraping results
    private val priceResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                PriceFetchService.ACTION_PRICE_RESULT -> {
                    val siteName = intent.getStringExtra(PriceFetchService.EXTRA_SITE_COMPLETED) ?: ""
                    val dealsJson = intent.getStringExtra(PriceFetchService.EXTRA_DEALS) ?: "[]"
                    
                    try {
                        val type = object : TypeToken<List<PriceDeal>>() {}.type
                        val newDeals: List<PriceDeal> = gson.fromJson(dealsJson, type)
                        
                        if (newDeals.isNotEmpty()) {
                            runOnUiThread {
                                // Add new deals to existing list and sort
                                val currentDeals = dealsAdapter.currentList.toMutableList()
                                currentDeals.addAll(newDeals.filter { newDeal -> 
                                    currentDeals.none { it.sellerName == newDeal.sellerName && it.price == newDeal.price }
                                })
                                val sortedDeals = currentDeals.sortedBy { it.price }
                                dealsAdapter.submitList(sortedDeals)
                                
                                tvResultCount.text = "${sortedDeals.size} deals"
                                tvCurrentSite.text = "Got results from $siteName"
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                PriceFetchService.ACTION_FETCH_COMPLETE -> {
                    runOnUiThread {
                        tvCurrentSite.text = "WebView scraping complete"
                    }
                }
            }
        }
    }
    
    // Theme preferences
    private val PREFS_NAME = "GamePassPrefs"
    private val KEY_DARK_MODE = "dark_mode"
    
    // Current filters
    private var currentFilters = SearchFilters(
        region = Region.UAE,  // Default to UAE
        type = DealType.KEY,  // Default to Key type
        duration = Duration.ONE_MONTH,  // Default to 1 month
        trustFilter = TrustFilter.ALL,
        excludeTrials = true  // No trials by default
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before setContentView
        applySavedTheme()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Register broadcast receiver for WebView scraping results
        val filter = IntentFilter().apply {
            addAction(PriceFetchService.ACTION_PRICE_RESULT)
            addAction(PriceFetchService.ACTION_FETCH_COMPLETE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(priceResultReceiver, filter)

        // Initialize price scraper with context for WebView support
        priceScraper = PriceScraper(this)

        initViews()
        setupRecyclerView()
        setupListeners()
        updateThemeIcon()
        
        // Show initial empty state
        showEmptyState()
    }
    
    private fun applySavedTheme() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true) // Default to dark
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES 
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
    
    private fun toggleTheme() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true)
        val newMode = !isDarkMode
        
        prefs.edit().putBoolean(KEY_DARK_MODE, newMode).apply()
        
        AppCompatDelegate.setDefaultNightMode(
            if (newMode) AppCompatDelegate.MODE_NIGHT_YES 
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
    
    private fun updateThemeIcon() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true)
        btnThemeToggle.setImageResource(
            if (isDarkMode) android.R.drawable.ic_menu_day  // Sun icon for switching to light
            else android.R.drawable.ic_menu_compass         // Compass icon for switching to dark
        )
    }

    private fun initViews() {
        recyclerDeals = findViewById(R.id.recyclerDeals)
        layoutLoading = findViewById(R.id.layoutLoading)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        layoutError = findViewById(R.id.layoutError)
        tvError = findViewById(R.id.tvError)
        tvResultCount = findViewById(R.id.tvResultCount)
        btnSearch = findViewById(R.id.btnSearch)
        btnRetry = findViewById(R.id.btnRetry)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        
        chipRegion = findViewById(R.id.chipRegion)
        chipDuration = findViewById(R.id.chipDuration)
        chipType = findViewById(R.id.chipType)
        chipTrustLevel = findViewById(R.id.chipTrustLevel)
        chipExcludeTrials = findViewById(R.id.chipExcludeTrials)
        
        // Theme toggle button
        btnThemeToggle = findViewById(R.id.btnThemeToggle)
        
        // Info button
        btnInfo = findViewById(R.id.btnInfo)
        
        // Progress views for streaming results
        layoutProgress = findViewById(R.id.layoutProgress)
        tvCurrentSite = findViewById(R.id.tvCurrentSite)
        tvFoundCount = findViewById(R.id.tvFoundCount)
        progressBar = findViewById(R.id.progressBar)
        tvProgressStatus = findViewById(R.id.tvProgressStatus)
        
        // Style the swipe refresh
        swipeRefresh.setColorSchemeResources(R.color.xbox_green)
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.background_secondary)
    }

    private fun setupRecyclerView() {
        dealsAdapter = DealsAdapter()
        recyclerDeals.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = dealsAdapter
        }
    }

    private fun setupListeners() {
        // Search button
        btnSearch.setOnClickListener {
            searchDeals()
        }

        // Retry button
        btnRetry.setOnClickListener {
            searchDeals()
        }

        // Swipe to refresh
        swipeRefresh.setOnRefreshListener {
            searchDeals()
        }
        
        // Theme toggle
        btnThemeToggle.setOnClickListener {
            toggleTheme()
        }
        
        // Info button - open Trust Level info page
        btnInfo.setOnClickListener {
            startActivity(Intent(this, TrustInfoActivity::class.java))
        }

        // Region filter chip
        chipRegion.setOnClickListener {
            showRegionFilterDialog()
        }

        // Duration filter chip
        chipDuration.setOnClickListener {
            showDurationFilterDialog()
        }

        // Type filter chip
        chipType.setOnClickListener {
            showTypeFilterDialog()
        }

        // Trust level filter
        chipTrustLevel.setOnClickListener {
            showTrustLevelFilterDialog()
        }
        
        // Exclude trials toggle
        chipExcludeTrials.setOnCheckedChangeListener { _, isChecked ->
            currentFilters = currentFilters.copy(excludeTrials = isChecked)
            if (dealsAdapter.currentList.isNotEmpty()) {
                applyFiltersToCurrentResults()
            }
        }
    }

    private fun searchDeals() {
        showStreamingState()
        
        // Start WebView-based scraping for Cloudflare-protected sites in parallel
        // These will send results via broadcast as they complete
        val cloudflareSites = listOf("G2A", "Kinguin", "Gamivo", "GG.deals", "HRK Game", "2Game", "Play-Asia", "GameStop", "Amazon")
        PriceFetchService.startFetch(this, cloudflareSites)

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            
            try {
                // Use streaming search with progress callbacks (for non-Cloudflare sites)
                val result = priceScraper.searchAllStreaming(
                    filters = currentFilters,
                    onProgress = { progress ->
                        // Update progress UI - must run on UI thread
                        runOnUiThread {
                            tvCurrentSite.text = "Searching ${progress.currentSite}..."
                            tvFoundCount.text = "${progress.dealsFound} found"
                            tvProgressStatus.text = "${progress.sitesSearched} of ${progress.totalSites} sites searched"
                            
                            // Update progress bar
                            val progressPercent = if (progress.totalSites > 0) {
                                (progress.sitesSearched * 100) / progress.totalSites
                            } else 0
                            progressBar.setProgressCompat(progressPercent, true)
                        }
                    },
                    onDealsFound = { deals ->
                        // Stream results to UI as they arrive (already sorted by PriceScraper)
                        // Must run on UI thread for RecyclerView updates
                        runOnUiThread {
                            if (deals.isNotEmpty()) {
                                dealsAdapter.submitList(deals.toList())
                                recyclerDeals.visibility = View.VISIBLE
                                
                                // Update result count
                                tvResultCount.text = "${deals.size} deals"
                                tvResultCount.visibility = View.VISIBLE
                            }
                        }
                    }
                )
                
                val searchTime = System.currentTimeMillis() - startTime
                
                when (result) {
                    is SearchResult.Success -> {
                        hideProgressShowResults(result.deals, searchTime)
                    }
                    is SearchResult.Empty -> {
                        // Fall back to sample data if scraping returns nothing
                        val fallbackDeals = FallbackDataProvider.getSampleDeals()
                            .filter { currentFilters.matches(it) }
                            .sortedBy { it.price }
                        
                        if (fallbackDeals.isNotEmpty()) {
                            hideProgressShowResults(fallbackDeals, 0, isFallback = true)
                        } else {
                            showEmptyState()
                        }
                    }
                    is SearchResult.Error -> {
                        // On error, show fallback data with a notice
                        val fallbackDeals = FallbackDataProvider.getSampleDeals()
                            .filter { currentFilters.matches(it) }
                            .sortedBy { it.price }
                        
                        if (fallbackDeals.isNotEmpty()) {
                            hideProgressShowResults(fallbackDeals, 0, isFallback = true)
                            Snackbar.make(
                                recyclerDeals,
                                "Showing cached prices. Pull to refresh.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        } else {
                            showErrorState(result.message)
                        }
                    }
                    SearchResult.Loading -> {
                        // Already showing loading
                    }
                }
            } catch (e: Exception) {
                // Final fallback
                val fallbackDeals = FallbackDataProvider.getSampleDeals()
                    .filter { currentFilters.matches(it) }
                    .sortedBy { it.price }
                
                if (fallbackDeals.isNotEmpty()) {
                    hideProgressShowResults(fallbackDeals, 0, isFallback = true)
                } else {
                    showErrorState(e.message ?: "Unknown error occurred")
                }
            }
        }
    }
    
    private fun showStreamingState() {
        swipeRefresh.isRefreshing = false
        
        // Show progress bar at top with results below
        layoutProgress.visibility = View.VISIBLE
        layoutLoading.visibility = View.GONE
        layoutEmpty.visibility = View.GONE
        layoutError.visibility = View.GONE
        recyclerDeals.visibility = View.VISIBLE  // Show results area (will populate as results come in)
        
        // Reset progress UI
        tvCurrentSite.text = "Starting search..."
        tvFoundCount.text = "0 found"
        tvProgressStatus.text = "0 of 8 sites"
        progressBar.setProgressCompat(0, false)
        
        // Clear previous results
        dealsAdapter.submitList(emptyList())
        tvResultCount.visibility = View.GONE
    }
    
    private fun hideProgressShowResults(deals: List<PriceDeal>, searchTimeMs: Long, isFallback: Boolean = false) {
        swipeRefresh.isRefreshing = false
        
        // Hide progress, show final results
        layoutProgress.visibility = View.GONE
        layoutLoading.visibility = View.GONE
        layoutEmpty.visibility = View.GONE
        layoutError.visibility = View.GONE
        recyclerDeals.visibility = View.VISIBLE
        
        // Sort deals - prioritize UAE and Global together, then by price
        val sortedDeals = deals.sortedWith(compareBy(
            { if (it.region == Region.UAE || it.region == Region.GLOBAL) 0 else 1 },
            { it.price }
        ))

        dealsAdapter.submitList(sortedDeals)

        // Update result count
        val countText = if (isFallback) {
            "${sortedDeals.size} deals (sample data)"
        } else {
            "${sortedDeals.size} deals found in ${searchTimeMs}ms"
        }
        tvResultCount.text = countText
        tvResultCount.visibility = View.VISIBLE

        // Show best deal notification
        sortedDeals.firstOrNull()?.let { bestDeal ->
            val message = "Best price: ${bestDeal.getFormattedPrice()} at ${bestDeal.sellerName}"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResults(deals: List<PriceDeal>, searchTimeMs: Long, isFallback: Boolean = false) {
        swipeRefresh.isRefreshing = false
        
        // Sort deals - prioritize UAE and Global together, then by price
        val sortedDeals = deals.sortedWith(compareBy(
            // First: prioritize UAE and Global regions (0 = top priority)
            { if (it.region == Region.UAE || it.region == Region.GLOBAL) 0 else 1 },
            // Then: sort by price within each priority group
            { it.price }
        ))

        dealsAdapter.submitList(sortedDeals)

        // Update result count
        val countText = if (isFallback) {
            "${sortedDeals.size} deals (sample data)"
        } else {
            "${sortedDeals.size} deals found in ${searchTimeMs}ms"
        }
        tvResultCount.text = countText
        tvResultCount.visibility = View.VISIBLE

        // Show the results
        layoutLoading.visibility = View.GONE
        layoutEmpty.visibility = View.GONE
        layoutError.visibility = View.GONE
        recyclerDeals.visibility = View.VISIBLE

        // Show best deal notification
        sortedDeals.firstOrNull()?.let { bestDeal ->
            val message = "Best price: ${bestDeal.getFormattedPrice()} at ${bestDeal.sellerName}"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyFiltersToCurrentResults() {
        val currentDeals = FallbackDataProvider.getSampleDeals()
            .filter { currentFilters.matches(it) }
            .sortedBy { it.price }
        
        if (currentDeals.isNotEmpty()) {
            dealsAdapter.submitList(currentDeals)
            tvResultCount.text = "${currentDeals.size} deals"
        } else {
            showEmptyState()
        }
    }

    private fun showLoadingState() {
        swipeRefresh.isRefreshing = false
        layoutLoading.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE
        layoutError.visibility = View.GONE
        recyclerDeals.visibility = View.GONE
        tvResultCount.visibility = View.GONE
    }

    private fun showEmptyState() {
        swipeRefresh.isRefreshing = false
        layoutLoading.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
        layoutError.visibility = View.GONE
        recyclerDeals.visibility = View.GONE
        tvResultCount.visibility = View.GONE
    }

    private fun showErrorState(message: String) {
        swipeRefresh.isRefreshing = false
        layoutLoading.visibility = View.GONE
        layoutEmpty.visibility = View.GONE
        layoutError.visibility = View.VISIBLE
        recyclerDeals.visibility = View.GONE
        tvResultCount.visibility = View.GONE
        tvError.text = message
    }

    // Filter dialogs
    private fun showRegionFilterDialog() {
        val dialog = BottomSheetDialog(this, R.style.Theme_GamePassPriceChecker)
        val view = layoutInflater.inflate(R.layout.dialog_filter, null)
        
        view.findViewById<TextView>(R.id.tvDialogTitle).text = "Select Region"
        
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerOptions)
        recycler.layoutManager = LinearLayoutManager(this)
        
        // Prioritize UAE and Global at the top
        val regions = listOf(
            Region.ALL, Region.UAE, Region.GLOBAL, Region.US, Region.UK,
            Region.EU, Region.TURKEY, Region.BRAZIL, Region.ARGENTINA, Region.INDIA
        )
        
        recycler.adapter = FilterOptionsAdapter(
            options = regions,
            selectedOption = currentFilters.region,
            displayText = { region ->
                val emoji = when (region.code) {
                    "all" -> "🌐"
                    "global" -> "🌍"
                    "ae" -> "🇦🇪"
                    "us" -> "🇺🇸"
                    "uk" -> "🇬🇧"
                    "eu" -> "🇪🇺"
                    "tr" -> "🇹🇷"
                    "br" -> "🇧🇷"
                    "ar" -> "🇦🇷"
                    "in" -> "🇮🇳"
                    else -> "🌍"
                }
                "$emoji ${region.displayName}"
            },
            onOptionSelected = { region ->
                currentFilters = currentFilters.copy(region = region)
                updateFilterChips()
                dialog.dismiss()
                if (dealsAdapter.currentList.isNotEmpty()) {
                    applyFiltersToCurrentResults()
                }
            }
        )
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showDurationFilterDialog() {
        val dialog = BottomSheetDialog(this, R.style.Theme_GamePassPriceChecker)
        val view = layoutInflater.inflate(R.layout.dialog_filter, null)
        
        view.findViewById<TextView>(R.id.tvDialogTitle).text = "Select Duration"
        
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerOptions)
        recycler.layoutManager = LinearLayoutManager(this)
        
        recycler.adapter = FilterOptionsAdapter(
            options = Duration.values().toList(),
            selectedOption = currentFilters.duration,
            displayText = { "⏱️ ${it.displayName}" },
            onOptionSelected = { duration ->
                currentFilters = currentFilters.copy(duration = duration)
                updateFilterChips()
                dialog.dismiss()
                if (dealsAdapter.currentList.isNotEmpty()) {
                    applyFiltersToCurrentResults()
                }
            }
        )
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showTypeFilterDialog() {
        val dialog = BottomSheetDialog(this, R.style.Theme_GamePassPriceChecker)
        val view = layoutInflater.inflate(R.layout.dialog_filter, null)
        
        view.findViewById<TextView>(R.id.tvDialogTitle).text = "Select Type"
        
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerOptions)
        recycler.layoutManager = LinearLayoutManager(this)
        
        recycler.adapter = FilterOptionsAdapter(
            options = DealType.values().toList(),
            selectedOption = currentFilters.type,
            displayText = { type ->
                val emoji = when (type) {
                    DealType.ALL -> "📦"
                    DealType.KEY -> "🔑"
                    DealType.ACCOUNT -> "👤"
                }
                "$emoji ${type.displayName}"
            },
            onOptionSelected = { type ->
                currentFilters = currentFilters.copy(type = type)
                updateFilterChips()
                dialog.dismiss()
                if (dealsAdapter.currentList.isNotEmpty()) {
                    applyFiltersToCurrentResults()
                }
            }
        )
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showTrustLevelFilterDialog() {
        val dialog = BottomSheetDialog(this, R.style.Theme_GamePassPriceChecker)
        val view = layoutInflater.inflate(R.layout.dialog_filter, null)
        
        view.findViewById<TextView>(R.id.tvDialogTitle).text = "Select Trust Level"
        
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerOptions)
        recycler.layoutManager = LinearLayoutManager(this)
        
        recycler.adapter = FilterOptionsAdapter(
            options = TrustFilter.values().toList(),
            selectedOption = currentFilters.trustFilter,
            displayText = { filter ->
                val emoji = when (filter) {
                    TrustFilter.ALL -> "👥"
                    TrustFilter.HIGH_ONLY -> "✅"
                    TrustFilter.HIGH_MEDIUM -> "👍"
                    TrustFilter.CAUTION -> "⚠️"
                }
                "$emoji ${filter.displayName}"
            },
            onOptionSelected = { filter ->
                currentFilters = currentFilters.copy(trustFilter = filter)
                updateFilterChips()
                dialog.dismiss()
                if (dealsAdapter.currentList.isNotEmpty()) {
                    applyFiltersToCurrentResults()
                }
            }
        )
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun updateFilterChips() {
        // Update region chip
        val regionEmoji = when (currentFilters.region.code) {
            "all" -> "🌐"
            "global" -> "🌍"
            "ae" -> "🇦🇪"
            "us" -> "🇺🇸"
            "uk" -> "🇬🇧"
            "eu" -> "🇪🇺"
            "tr" -> "🇹🇷"
            "br" -> "🇧🇷"
            "ar" -> "🇦🇷"
            "in" -> "🇮🇳"
            else -> "🌍"
        }
        chipRegion.text = "$regionEmoji ${currentFilters.region.displayName}"

        // Update duration chip
        chipDuration.text = "⏱️ ${currentFilters.duration.displayName}"

        // Update type chip
        val typeEmoji = when (currentFilters.type) {
            DealType.ALL -> "📦"
            DealType.KEY -> "🔑"
            DealType.ACCOUNT -> "👤"
        }
        chipType.text = "$typeEmoji ${currentFilters.type.displayName}"
        
        // Update trust level chip
        val trustEmoji = when (currentFilters.trustFilter) {
            TrustFilter.ALL -> "👥"
            TrustFilter.HIGH_ONLY -> "✅"
            TrustFilter.HIGH_MEDIUM -> "👍"
            TrustFilter.CAUTION -> "⚠️"
        }
        chipTrustLevel.text = "$trustEmoji ${currentFilters.trustFilter.displayName}"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(priceResultReceiver)
    }
}
