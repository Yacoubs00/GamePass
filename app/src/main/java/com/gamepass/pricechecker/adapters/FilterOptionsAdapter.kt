package com.gamepass.pricechecker.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gamepass.pricechecker.R

/**
 * Generic adapter for filter option dialogs
 */
class FilterOptionsAdapter<T>(
    private val options: List<T>,
    private var selectedOption: T,
    private val displayText: (T) -> String,
    private val onOptionSelected: (T) -> Unit
) : RecyclerView.Adapter<FilterOptionsAdapter<T>.OptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount(): Int = options.size

    inner class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOptionText: TextView = itemView.findViewById(R.id.tvOptionText)
        private val ivCheck: ImageView = itemView.findViewById(R.id.ivCheck)

        fun bind(option: T) {
            tvOptionText.text = displayText(option)
            ivCheck.visibility = if (option == selectedOption) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                val previousSelected = selectedOption
                selectedOption = option
                
                // Update UI
                val previousPosition = options.indexOf(previousSelected)
                val currentPosition = options.indexOf(option)
                notifyItemChanged(previousPosition)
                notifyItemChanged(currentPosition)
                
                // Notify callback
                onOptionSelected(option)
            }
        }
    }
}
