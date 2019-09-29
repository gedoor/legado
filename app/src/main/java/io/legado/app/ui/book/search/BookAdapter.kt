package io.legado.app.ui.book.search

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter

class BookAdapter(context: Context) :
    SimpleRecyclerAdapter<String>(context, R.layout.item_text) {

    override fun convert(holder: ItemViewHolder, item: String, payloads: MutableList<Any>) {

    }

}