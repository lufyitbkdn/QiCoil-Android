package com.Meditation.Sounds.frequencies.lemeor.ui.albums.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.data.model.Album
import kotlinx.android.synthetic.main.item_search.view.*

class AlbumsSearchAdapter(
    private var mData: List<Album> = arrayListOf(),
) : RecyclerView.Adapter<AlbumsSearchAdapter.ViewHolder>() {

    interface Listener {
        fun onAlbumSearchClick(album: Album, i: Int)
    }

    private var mListener: Listener? = null

    fun setOnClickListener(listener: Listener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search, parent, false))
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = mData[position]
        holder.itemView.tvSearch.text = album.name
        holder.itemView.setOnClickListener { mListener?.onAlbumSearchClick(album, position) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    fun setData(albumList: List<Album>?) {
        mData = albumList as ArrayList<Album>
        notifyDataSetChanged()
    }
}