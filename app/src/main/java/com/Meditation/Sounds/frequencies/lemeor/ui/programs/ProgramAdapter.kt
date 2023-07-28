package com.Meditation.Sounds.frequencies.lemeor.ui.programs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.Meditation.Sounds.frequencies.R
import com.Meditation.Sounds.frequencies.lemeor.FAVORITES
import com.Meditation.Sounds.frequencies.lemeor.data.model.Program
import com.Meditation.Sounds.frequencies.lemeor.getConvertedTime
import kotlinx.android.synthetic.main.item_program.view.*
import java.util.*

class ProgramAdapter(
    private var mData: List<Program> = listOf()
) : RecyclerView.Adapter<ProgramAdapter.ViewHolder>() {

    interface Listener {
        fun onClickItem(program: Program, i: Int)
        fun onDeleteItem(program: Program, i: Int)
    }

    private var mListener: Listener? = null

    fun setOnClickListener(listener: Listener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_program, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val program = mData[position]

        holder.itemView.item_program_name.text = program.name
        holder.itemView.item_program_duration.text = holder.itemView.context.getString(
            R.string.total_time,
            getConvertedTime((program.records.size * 300000).toLong())
        )

        if (program.isMy) {
            if (program.name == FAVORITES) {
                holder.itemView.item_program_delete.visibility = View.INVISIBLE
                holder.itemView.item_program_lock.visibility = View.INVISIBLE
            } else {
                holder.itemView.item_program_delete.visibility = View.VISIBLE
                holder.itemView.item_program_lock.visibility = View.INVISIBLE
            }
        } else {
            holder.itemView.item_program_delete.visibility = View.INVISIBLE
            if (program.isUnlocked) {
                holder.itemView.item_program_lock.visibility = View.INVISIBLE
            } else {
                holder.itemView.item_program_lock.visibility = View.VISIBLE
            }
        }

        holder.itemView.item_program_delete.setOnClickListener {
            mListener?.onDeleteItem(
                program,
                position
            )
        }
        holder.itemView.setOnClickListener { mListener?.onClickItem(program, position) }

        if (position == mData.size - 1) {
            holder.itemView.program_divider_favorites.visibility = View.INVISIBLE
        } else {
            holder.itemView.program_divider_favorites.visibility = View.VISIBLE
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    fun setData(programList: List<Program>?) {
        mData = ArrayList(programList as MutableList).sortedWith(compareByDescending { it.isMy })
        notifyDataSetChanged()
    }
}