package Akari.LSPosed.namechanger.ui

import Akari.LSPosed.namechanger.R
import Akari.LSPosed.namechanger.databinding.ItemAppCardBinding
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val onItemClick: (AppItem) -> Unit
) : ListAdapter<AppItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isHeader) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 1) {
            val view = inflater.inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val binding = ItemAppCardBinding.inflate(inflater, parent, false)
            AppViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is HeaderViewHolder) {
            holder.bind(item)
        } else if (holder is AppViewHolder) {
            holder.bind(item)
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHeader: TextView = view.findViewById(R.id.tvHeader)
        fun bind(item: AppItem) {
            tvHeader.text = item.headerText
        }
    }

    inner class AppViewHolder(private val binding: ItemAppCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppItem) {
            val context = binding.root.context
            
            val appNameText = if (item.isSystemLauncher) {
                "${item.appName} (${context.getString(R.string.current_system_launcher)})"
            } else {
                item.appName
            }
            binding.tvAppName.text = appNameText
            
            binding.tvPackageName.text = item.packageName

            if (item.icon != null) {
                binding.ivAppIcon.setImageDrawable(item.icon)
            } else {
                binding.ivAppIcon.setImageResource(R.mipmap.ic_launcher)
            }

            if (item.isConfigured) {
                binding.tvStatus.text = item.customName
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_configured)
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.badge_text))
            } else {
                binding.tvStatus.text = context.getString(R.string.status_unconfigured)
                binding.tvStatus.background = null
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_unconfigured))
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
            if (oldItem.isHeader && newItem.isHeader) return oldItem.headerText == newItem.headerText
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem): Boolean {
            return oldItem == newItem
        }
    }
}
