package io.legado.app.ui.search

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewDelegate
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchBook
import kotlinx.android.synthetic.main.item_search.view.*

class SearchAdapter(context: Context) : SimpleRecyclerAdapter<SearchBook>(context, R.layout.item_search) {

    init {
        addItemViewDelegate(TestItemDelegate(context))
    }

    override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
        holder.itemView.bookName.text = "我欲封天"
    }

    internal class TestItemDelegate(context: Context) : ItemViewDelegate<SearchBook>(context, R.layout.item_search) {

        override fun convert(holder: ItemViewHolder, item: SearchBook, payloads: MutableList<Any>) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

}