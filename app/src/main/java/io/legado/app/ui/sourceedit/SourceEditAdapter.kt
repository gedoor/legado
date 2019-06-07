package io.legado.app.ui.sourceedit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R

class SourceEditAdapter : RecyclerView.Adapter<SourceEditAdapter.MyViewHolder>() {

    var sourceEditEntities:ArrayList<SourceEditEntity> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_source_edit, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getItemCount(): Int {
        return sourceEditEntities.size
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    }

    class SourceEditEntity {
        var key:String?=null
        var value:String?=null
        var hint:String?=null
    }
}