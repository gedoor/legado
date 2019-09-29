package io.legado.app.ui.book.search

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchKeyword
import kotlinx.android.synthetic.main.item_text.view.*


class HistoryKeyAdapter(context: Context) :
    SimpleRecyclerAdapter<SearchKeyword>(context, R.layout.item_text) {

    override fun convert(holder: ItemViewHolder, item: SearchKeyword, payloads: MutableList<Any>) {
        with(holder.itemView) {
            text_view.text = item.word
        }
    }

}