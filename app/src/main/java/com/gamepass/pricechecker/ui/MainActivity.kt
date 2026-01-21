package com.gamepass.pricechecker.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gamepass.pricechecker.R
import com.gamepass.pricechecker.adapters.DealsAdapter
import com.gamepass.pricechecker.adapters.FilterOptionsAdapter
import com.gamepass.pricechecker.models.*
import com.gamepass.pricechecker.network.FallbackDataProvider
import com.gamepass.pricechecker.network.PriceScraper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var chipTrustedOnly: Chip
    
    // Theme toggle
    private lateinit var btnThemeToggle: ImageButton

    // Adapter and data
    private lateinit var dealsAdapter: DealsAdapter
    private val priceScraper = PriceScraper()
    
    // Theme preferences
    private val PREFS_NAME = "GamePassPrefs"
    private val KEY_DARK_MODE = "dark_mode"
    
    // Current filters
    private var currentFilters = SearchFilters(
        region = Region.ALL,  // Default to all, but UAE is prioritized in display
        type = DealType.ALL,
        duration = Duration.ALL,
        trustedOnly = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before setContentView
        applySavedTheme()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        chipTrustedOnly = findViewById(R.id.chipTrustedOnly)
        
        // Theme toggle button
        btnThemeToggle = findViewById(R.id.btnThemeToggle)
        
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

        // Trusted only toggle
        chipTrustedOnly.setOnCheckedChangeListener { _, isChecked ->
            currentFilters = currentFilters.copy(trustedOnly = isChecked)
            if (dealsAdapter.currentList.isNotEmpty()) {
                applyFiltersToCurrentResults()
            }
        }
    }

    private fun searchDeals() {
        showLoadingState()

        lifecycleScope.launch {
            try {
                // Try to scrape live data
                val result = priceScraper.searchAll(currentFilters)
                
                when (result) {
                    is SearchResult.Success -> {
                        showResults(result.deals, result.searchTimeMs)
                    }
                    is SearchResult.Empty -> {
                        // Fall back to sample data if scraping returns nothing
                        val fallbackDeals = FallbackDataProvider.getSampleDeals()
                            .filter { currentFilters.matches(it) }
                            .sortedBy { it.price }
                        
                        if (fallbackDeals.isNotEmpty()) {
                            showResults(fallbackDeals, 0, isFallback = true)
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
                            showResults(fallbackDeals, 0, isFallback = true)
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
                    showResults(fallbackDeals, 0, isFallback = true)
                } else {
                    showErrorState(e.message ?: "Unknown error occurred")
                }
            }
        }
    }

    private fun showResults(deals: List<PriceDeal>, searchTimeMs: Long, isFallback: Boolean = false) {
        swipeRefresh.isRefreshing = false
        
        // Sort deals - prioritize UAE and Global first if user selected those
        val sortedDeals = deals.sortedWith(compareBy(
            { it.price },
            { if (it.region == Region.UAE || it.region == Region.GLOBAL) 0 else 1 }
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
    }
}
