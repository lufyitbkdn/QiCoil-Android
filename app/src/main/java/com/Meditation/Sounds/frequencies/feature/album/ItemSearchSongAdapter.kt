package com.Meditation.Sounds.frequencies.feature.album

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.models.Song
import kotlinx.android.synthetic.main.item_search.view.*

class ItemSearchSongAdapter(var context: Context, var data: List<Song>, var listener: IOnClickItemListener)
    : RecyclerView.Adapter<ItemSearchSongAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search, parent, false))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.itemView.tvSearch.text = item.title
        holder.itemView.tvSearch.setOnClickListener {
            listener?.let {
                it.onClickItem(position)
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface IOnClickItemListener {
        fun onClickItem(position: Int)
    }
}
