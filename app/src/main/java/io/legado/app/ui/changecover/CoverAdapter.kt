package io.legado.app.ui.changecover

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book


class CoverAdapter(context: Context) : SimpleRecyclerAdapter<Book>(context, R.layout.item_cover) {

    override fun convert(holder: ItemViewHolder, item: Book, payloads: MutableList<Any>) {

    }

}