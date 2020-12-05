package io.legado.app.ui.rss.subscription

import android.content.Context
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SourceSub
import io.legado.app.databinding.ItemSourceSubBinding

class SourceSubAdapter(context: Context) :
    SimpleRecyclerAdapter<SourceSub, ItemSourceSubBinding>(context) {


    override fun convert(
        holder: ItemViewHolder,
        binding: ItemSourceSubBinding,
        item: SourceSub,
        payloads: MutableList<Any>
    ) {
        binding.tvName.text = item.name
        binding.tvUrl.text = item.url
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemSourceSubBinding) {

    }

    override fun getViewBinding(parent: ViewGroup): ItemSourceSubBinding {
        return ItemSourceSubBinding.inflate(inflater, parent, false)
    }

}