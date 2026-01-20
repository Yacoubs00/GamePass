package com.gamepass.pricechecker.adapters

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gamepass.pricechecker.R
import com.gamepass.pricechecker.models.PriceDeal
import com.gamepass.pricechecker.models.TrustLevel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Adapter for displaying price deals in RecyclerView
 */
class DealsAdapter : ListAdapter<PriceDeal, DealsAdapter.DealViewHolder>(DealDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deal, parent, false)
        return DealViewHolder(view)
    }

    override fun onBindViewHolder(holder: DealViewHolder, position: Int) {
        holder.bind(getItem(position), position == 0)
    }

    class DealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView as MaterialCardView
        private val tvSellerName: TextView = itemView.findViewById(R.id.tvSellerName)
        private val tvTrustBadge: TextView = itemView.findViewById(R.id.tvTrustBadge)
        private val tvRegion: TextView = itemView.findViewById(R.id.tvRegion)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val layoutRating: LinearLayout = itemView.findViewById(R.id.layoutRating)
        private val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        private val tvReviewCount: TextView = itemView.findViewById(R.id.tvReviewCount)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvOriginalPrice: TextView = itemView.findViewById(R.id.tvOriginalPrice)
        private val btnVisit: MaterialButton = itemView.findViewById(R.id.btnVisit)

        fun bind(deal: PriceDeal, isBestDeal: Boolean) {
            val context = itemView.context

            // Seller name
            tvSellerName.text = deal.sellerName

            // Trust badge with appropriate color and icon
            val (trustText, trustColor) = when (deal.trustLevel) {
                TrustLevel.HIGH -> "✅ Highly Trusted" to R.color.trust_high
                TrustLevel.MEDIUM -> "👍 Trusted" to R.color.trust_medium
                TrustLevel.CAUTION -> "⚠️ Use Caution" to R.color.trust_caution
            }
            tvTrustBadge.text = trustText
            tvTrustBadge.setTextColor(ContextCompat.getColor(context, trustColor))

            // Region with flag emoji
            val regionEmoji = when (deal.region.code) {
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
            tvRegion.text = "$regionEmoji ${deal.region.displayName}"

            // Duration
            tvDuration.text = "⏱️ ${deal.duration.displayName}"

            // Type
            val typeEmoji = if (deal.type.displayName == "Key") "🔑" else "👤"
            tvType.text = "$typeEmoji ${deal.type.displayName}"

            // Rating (if available)
            if (deal.rating != null && deal.rating > 0) {
                layoutRating.visibility = View.VISIBLE
                tvRating.text = "⭐ ${String.format("%.1f", deal.rating)}"
                tvReviewCount.text = deal.reviewCount?.let { "(${formatNumber(it)} reviews)" } ?: ""
            } else {
                layoutRating.visibility = View.GONE
            }

            // Price
            tvPrice.text = deal.getFormattedPrice()

            // Original price and discount (if available)
            if (deal.originalPrice != null && deal.discount != null && deal.discount > 0) {
                tvOriginalPrice.visibility = View.VISIBLE
                tvOriginalPrice.text = "Was ${deal.getFormattedOriginalPrice()} (-${deal.discount}%)"
                tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvOriginalPrice.visibility = View.GONE
            }

            // Highlight best deal
            if (isBestDeal) {
                card.strokeWidth = 2
                card.strokeColor = ContextCompat.getColor(context, R.color.xbox_green)
            } else {
                card.strokeWidth = 0
            }

            // Visit button - open in browser
            btnVisit.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deal.url))
                context.startActivity(intent)
            }

            // Card click also opens URL
            card.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deal.url))
                context.startActivity(intent)
            }
        }

        private fun formatNumber(number: Int): String {
            return when {
                number >= 1000000 -> "${number / 1000000}M"
                number >= 1000 -> "${number / 1000}K"
                else -> number.toString()
            }
        }
    }

    class DealDiffCallback : DiffUtil.ItemCallback<PriceDeal>() {
        override fun areItemsTheSame(oldItem: PriceDeal, newItem: PriceDeal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PriceDeal, newItem: PriceDeal): Boolean {
            return oldItem == newItem
        }
    }
}
