package com.Meditation.Sounds.frequencies.lemeor.tools.downloader

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.data.model.Track
import kotlinx.android.synthetic.main.item_download.view.*

class DownloaderAdapter : RecyclerView.Adapter<DownloaderAdapter.ViewHolder>() {
    var data: List<Track> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var fileProgressMap: HashMap<Int, Int> = HashMap()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var downloadErrorTracks:List<Int> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_download, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = data[position]
        holder.itemView.item_track_name.text = track.name
        holder.itemView.item_track_progress.progress = fileProgressMap[track.id] ?: 0
        if (downloadErrorTracks.contains(track.id)) {
            holder.itemView.item_track_progress.progressDrawable =
                ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_progress_bar_error)
        } else {
            holder.itemView.item_track_progress.progressDrawable =
                ContextCompat.getDrawable(holder.itemView.context, R.drawable.bg_progress_bar)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    @SuppressLint("NotifyDataSetChanged")
    fun updateProgress(parent: RecyclerView, position: Int, progress: Int) {
//        data[position].progress = progress
        (parent.layoutManager as LinearLayoutManager).findViewByPosition(position)?.let {
            val progressBar = it.findViewById<ProgressBar>(R.id.item_track_progress)
            progressBar.progress = progress
        }
    }
}