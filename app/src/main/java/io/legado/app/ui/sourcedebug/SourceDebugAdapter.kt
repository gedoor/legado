package io.legado.app.ui.sourcedebug

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import kotlinx.android.synthetic.main.item_source_debug.view.*

class SourceDebugAdapter : RecyclerView.Adapter<SourceDebugAdapter.MyViewHolder>() {

    val logList = arrayListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_source_debug, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(logList[position])
    }

    override fun getItemCount(): Int {
        return logList.size
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(log: String) = with(itemView) {
            text_view.text = log
        }
    }
}