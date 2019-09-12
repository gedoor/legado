package io.legado.app.ui.book.info

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookChapter
import io.legado.app.lib.theme.accentColor
import kotlinx.android.synthetic.main.item_chapter_list.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.textColorResource

class ChapterListAdapter(context: Context, var callBack: CallBack) :
    SimpleRecyclerAdapter<BookChapter>(context, R.layout.item_chapter_list) {

    var reorder: Boolean = false; // 是否倒序

    override fun convert(holder: ItemViewHolder, item: BookChapter, payloads: MutableList<Any>) {
        holder.itemView.apply {
            var _item: BookChapter = item;
            if (reorder) {
                _item = getItems().get(getItems().size - item.index - 1);
            }
            tv_chapter_name.text = _item.title
            if (_item.index == callBack.durChapterIndex()) {
                tv_chapter_name.setTextColor(context.accentColor)
            } else {
                tv_chapter_name.textColorResource = R.color.tv_text_secondary
            }
            this.onClick {
                callBack.openChapter(_item)
            }
        }
    }

    interface CallBack {
        fun openChapter(chapter: BookChapter)
        fun durChapterIndex(): Int
    }
}