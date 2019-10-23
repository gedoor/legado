package io.legado.app.ui.book.search

import android.content.Context
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.ui.widget.anima.explosion_field.ExplosionField
import kotlinx.android.synthetic.main.item_text.view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick


class HistoryKeyAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<SearchKeyword>(context, R.layout.item_text) {

    override fun convert(holder: ItemViewHolder, item: SearchKeyword, payloads: MutableList<Any>) {
        with(holder.itemView) {
            text_view.text = item.word
            onClick {
                callBack.searchHistory(item.word)
            }
            onLongClick {
                it?.let {
                    ExplosionField(context).explode(it, true)
                }
                GlobalScope.launch(IO) {
                    App.db.searchKeywordDao().delete(item)
                }
                true
            }
        }
    }

    interface CallBack {
        fun searchHistory(key: String)
    }
}