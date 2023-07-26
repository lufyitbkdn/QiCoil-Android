package com.Meditation.Sounds.frequencies.lemeor.ui.albums.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.data.model.Program
import kotlinx.android.synthetic.main.item_search.view.*

class ProgramsSearchAdapter(
    private var mData: List<Program> = arrayListOf(),
) : RecyclerView.Adapter<ProgramsSearchAdapter.ViewHolder>() {

    interface Listener {
        fun onProgramSearchClick(program: Program, i: Int)
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
        val program = mData[position]
        holder.itemView.tvSearch.text = program.name
        holder.itemView.setOnClickListener { mListener?.onProgramSearchClick(program, position) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    fun setData(programList: List<Program>?) {
        mData = programList as ArrayList<Program>
        notifyDataSetChanged()
    }
}