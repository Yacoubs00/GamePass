package com.gamepass.pricechecker.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gamepass.pricechecker.R
import com.gamepass.pricechecker.models.Sellers
import com.gamepass.pricechecker.models.TrustLevel
import com.google.android.material.card.MaterialCardView

class TrustInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trust_info)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerSellers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SellerTrustAdapter(getTrustData())
    }

    private fun getTrustData(): List<SellerTrustInfo> {
        return listOf(
            // Highly Trusted - Official/Authorized
            SellerTrustInfo(
                name = "CDKeys",
                website = "cdkeys.com",
                trustLevel = TrustLevel.HIGH,
                description = "Well-established digital key retailer since 2012",
                trustReason = "Trustpilot: 4.7/5 (100K+ reviews) • 12+ years in business • Money-back guarantee"
            ),
            SellerTrustInfo(
                name = "Eneba",
                website = "eneba.com",
                trustLevel = TrustLevel.HIGH,
                description = "Large marketplace with comprehensive buyer protection",
                trustReason = "Trustpilot: 4.6/5 (200K+ reviews) • Eneba Protect program • Fast refunds"
            ),
            SellerTrustInfo(
                name = "G2A",
                website = "g2a.com",
                trustLevel = TrustLevel.HIGH,
                description = "World's largest gaming marketplace",
                trustReason = "Trustpilot: 4.5/5 (200K+ reviews) • G2A Shield protection • 15M+ customers"
            ),
            SellerTrustInfo(
                name = "Green Man Gaming",
                website = "greenmangaming.com",
                trustLevel = TrustLevel.HIGH,
                description = "Official authorized reseller",
                trustReason = "IsThereAnyDeal verified • Official Xbox partner • 13+ years trusted"
            ),
            SellerTrustInfo(
                name = "Humble Bundle",
                website = "humblebundle.com",
                trustLevel = TrustLevel.HIGH,
                description = "Official Microsoft partner, supports charity",
                trustReason = "Official keys only • $200M+ donated to charity • Owned by IGN"
            ),
            SellerTrustInfo(
                name = "Instant Gaming",
                website = "instant-gaming.com",
                trustLevel = TrustLevel.HIGH,
                description = "European-based trusted retailer",
                trustReason = "Trustpilot: 4.5/5 (150K+ reviews) • Fast delivery • EU-based company"
            ),
            SellerTrustInfo(
                name = "Kinguin",
                website = "kinguin.net",
                trustLevel = TrustLevel.HIGH,
                description = "Large gaming marketplace with buyer protection",
                trustReason = "Trustpilot: 4.4/5 (80K+ reviews) • Buyer Protection available • 10+ years"
            ),
            SellerTrustInfo(
                name = "Gamivo",
                website = "gamivo.com",
                trustLevel = TrustLevel.HIGH,
                description = "Marketplace with Smart subscription benefits",
                trustReason = "Trustpilot: 4.6/5 (50K+ reviews) • Smart program discounts • Good support"
            ),
            SellerTrustInfo(
                name = "Fanatical",
                website = "fanatical.com",
                trustLevel = TrustLevel.HIGH,
                description = "Authorized reseller with great bundles",
                trustReason = "IsThereAnyDeal verified • Official partner • UK-based company"
            ),
            SellerTrustInfo(
                name = "K4G",
                website = "k4g.com",
                trustLevel = TrustLevel.HIGH,
                description = "Key marketplace with competitive prices",
                trustReason = "Trustpilot: 4.6/5 (20K+ reviews) • Buyer protection • Fast delivery"
            ),
            SellerTrustInfo(
                name = "GAMESEAL",
                website = "gameseal.com",
                trustLevel = TrustLevel.HIGH,
                description = "Trusted key marketplace",
                trustReason = "GG.deals verified • Positive Reddit feedback • Buyer protection"
            ),
            SellerTrustInfo(
                name = "MMOGA",
                website = "mmoga.com",
                trustLevel = TrustLevel.HIGH,
                description = "German marketplace, established 2002",
                trustReason = "Trustpilot: 4.5/5 (50K+ reviews) • 20+ years in business • German company"
            ),
            SellerTrustInfo(
                name = "Voidu",
                website = "voidu.com",
                trustLevel = TrustLevel.HIGH,
                description = "Netherlands-based authorized reseller",
                trustReason = "IsThereAnyDeal verified • Official keys • EU consumer protection"
            ),
            SellerTrustInfo(
                name = "Nuuvem",
                website = "nuuvem.com",
                trustLevel = TrustLevel.HIGH,
                description = "Brazil-based, great for SA region",
                trustReason = "IsThereAnyDeal verified • Official regional keys • Good prices"
            ),
            SellerTrustInfo(
                name = "MTCGame",
                website = "mtcgame.com",
                trustLevel = TrustLevel.HIGH,
                description = "Turkish marketplace, great regional prices",
                trustReason = "Trustpilot: 4.5/5 • Turkey key specialist • Fast delivery"
            ),
            SellerTrustInfo(
                name = "Wyrel",
                website = "wyrel.com",
                trustLevel = TrustLevel.HIGH,
                description = "Specializes in Turkey region keys",
                trustReason = "GG.deals listed • Positive community feedback • Regional specialist"
            ),
            SellerTrustInfo(
                name = "2Game",
                website = "2game.com",
                trustLevel = TrustLevel.HIGH,
                description = "Authorized reseller",
                trustReason = "IsThereAnyDeal verified • Official keys • Established retailer"
            ),
            SellerTrustInfo(
                name = "Driffle",
                website = "driffle.com",
                trustLevel = TrustLevel.HIGH,
                description = "Key marketplace with buyer protection",
                trustReason = "GG.deals verified • Buyer protection • Good prices"
            ),
            SellerTrustInfo(
                name = "G2Play",
                website = "g2play.net",
                trustLevel = TrustLevel.HIGH,
                description = "Trusted key marketplace",
                trustReason = "Trustpilot: 4.4/5 (10K+ reviews) • Established • Buyer protection"
            ),
            SellerTrustInfo(
                name = "Play-Asia",
                website = "play-asia.com",
                trustLevel = TrustLevel.HIGH,
                description = "Asian game retailer, global shipping",
                trustReason = "20+ years in business • Official retailer • Asia specialist"
            ),
            SellerTrustInfo(
                name = "GameStop",
                website = "gamestop.com",
                trustLevel = TrustLevel.HIGH,
                description = "Official US retailer",
                trustReason = "Official retailer • Public company • Physical stores"
            ),
            SellerTrustInfo(
                name = "Amazon",
                website = "amazon.com",
                trustLevel = TrustLevel.HIGH,
                description = "Official retailer",
                trustReason = "Official keys • A-to-z guarantee • World's largest retailer"
            ),
            SellerTrustInfo(
                name = "Difmark",
                website = "difmark.com",
                trustLevel = TrustLevel.HIGH,
                description = "Great for regional keys (Turkey, Brazil)",
                trustReason = "GG.deals verified • Regional specialist • Good prices"
            ),
            SellerTrustInfo(
                name = "HRK Game",
                website = "hrkgame.com",
                trustLevel = TrustLevel.HIGH,
                description = "European retailer",
                trustReason = "Trustpilot: 4.3/5 • EU-based • Competitive prices"
            ),
            SellerTrustInfo(
                name = "Gamesplanet",
                website = "gamesplanet.com",
                trustLevel = TrustLevel.HIGH,
                description = "UK/EU authorized retailer",
                trustReason = "IsThereAnyDeal verified • Official partner • Star deals program"
            ),
            
            // Medium Trust
            SellerTrustInfo(
                name = "Gamers Outlet",
                website = "gamers-outlet.net",
                trustLevel = TrustLevel.MEDIUM,
                description = "Budget key seller",
                trustReason = "Lower prices but fewer reviews • Check seller ratings • Use PayPal for protection"
            ),
            SellerTrustInfo(
                name = "SCDKey",
                website = "scdkey.com",
                trustLevel = TrustLevel.MEDIUM,
                description = "Budget-friendly key seller",
                trustReason = "Mixed reviews • Very low prices • Buyer caution advised"
            )
        ).sortedWith(compareBy({ it.trustLevel.ordinal }, { it.name }))
    }
}

data class SellerTrustInfo(
    val name: String,
    val website: String,
    val trustLevel: TrustLevel,
    val description: String,
    val trustReason: String
)

class SellerTrustAdapter(
    private val sellers: List<SellerTrustInfo>
) : RecyclerView.Adapter<SellerTrustAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seller_trust, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sellers[position])
    }

    override fun getItemCount() = sellers.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView as MaterialCardView
        private val tvName: TextView = itemView.findViewById(R.id.tvSellerName)
        private val tvWebsite: TextView = itemView.findViewById(R.id.tvWebsite)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvTrustBadge)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvReason: TextView = itemView.findViewById(R.id.tvTrustReason)

        fun bind(seller: SellerTrustInfo) {
            tvName.text = seller.name
            tvWebsite.text = seller.website
            tvDescription.text = seller.description
            tvReason.text = "📋 ${seller.trustReason}"

            val (badge, color) = when (seller.trustLevel) {
                TrustLevel.HIGH -> "✅ Highly Trusted" to 0xFF4CAF50.toInt()
                TrustLevel.MEDIUM -> "👍 Trusted" to 0xFFFFC107.toInt()
                TrustLevel.CAUTION -> "⚠️ Use Caution" to 0xFFFF9800.toInt()
            }
            tvBadge.text = badge
            tvBadge.setTextColor(color)

            // Open website on click
            card.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://${seller.website}"))
                itemView.context.startActivity(intent)
            }

            tvWebsite.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://${seller.website}"))
                itemView.context.startActivity(intent)
            }
        }
    }
}
