package com.Meditation.Sounds.frequencies.lemeor.ui.albums.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track
import kotlinx.android.synthetic.main.item_search.view.*

class TracksSearchAdapter(
    private var mData: List<Track> = arrayListOf(),
) : RecyclerView.Adapter<TracksSearchAdapter.ViewHolder>() {

    interface Listener {
        fun onTrackSearchClick(track: Track, i: Int)
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
        val track = mData[position]
        holder.itemView.tvSearch.text = track.name
        holder.itemView.setOnClickListener { mListener?.onTrackSearchClick(track, position) }

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    fun setData(trackList: List<Track>?) {
        mData = trackList as ArrayList<Track>
        notifyDataSetChanged()
    }
}