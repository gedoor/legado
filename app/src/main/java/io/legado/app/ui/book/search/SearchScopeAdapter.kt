package io.legado.app.ui.book.search

import android.content.Context
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.ItemFilletTextBinding

class SearchScopeAdapter(context: Context) :
    RecyclerAdapter<String, ItemFilletTextBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemFilletTextBinding {
        return ItemFilletTextBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemFilletTextBinding,
        item: String,
        payloads: MutableList<Any>
    ) {
        binding.run {
            textView.text = item
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemFilletTextBinding) {

    }

}