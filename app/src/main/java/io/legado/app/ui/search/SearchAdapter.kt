package io.legado.app.ui.search

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewDelegate
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchBook

class SearchAdapter(context: Context) : SimpleRecyclerAdapter<SearchBook>(context, R.layout.item_search) {

    init {
        addItemViewDelegate(TestItemDelegate(context))
    }

    override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
    }

    internal class TestItemDelegate(context: Context) : ItemViewDelegate<SearchBook>(context, R.layout.item_search) {

        override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
        }

    }

}