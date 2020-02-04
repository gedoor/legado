package io.legado.app.ui.main.bookshelf.books

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.invisible
import kotlinx.android.synthetic.main.item_bookshelf_list.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick

class BooksAdapterList(context: Context, private val callBack: CallBack) :
    BaseBooksAdapter(context, R.layout.item_bookshelf_list) {

    override fun convert(holder: ItemViewHolder, item: Book, payloads: MutableList<Any>) {
        with(holder.itemView) {
            if (payloads.isEmpty()) {
                ATH.applyBackgroundTint(this)
                tv_name.text = item.name
                tv_author.text = item.author
                tv_read.text = item.durChapterTitle
                tv_last.text = item.latestChapterTitle
                iv_cover.load(item.getDisplayCover(), item.name, item.author)
                onClick { callBack.open(item) }
                onLongClick {
                    callBack.openBookInfo(item)
                    true
                }
                if (item.origin != BookType.local && callBack.isUpdate(item.bookUrl)) {
                    bv_unread.invisible()
                    rl_loading.show()
                } else {
                    rl_loading.hide()
                    bv_unread.setBadgeCount(item.getUnreadChapterNum())
                    bv_unread.setHighlight(item.lastCheckCount > 0)
                }
            } else {
                when (payloads[0]) {
                    5 -> {
                        if (item.origin != BookType.local && callBack.isUpdate(item.bookUrl)) {
                            bv_unread.invisible()
                            rl_loading.show()
                        } else {
                            rl_loading.hide()
                            bv_unread.setBadgeCount(item.getUnreadChapterNum())
                            bv_unread.setHighlight(item.lastCheckCount > 0)
                        }
                    }
                }
            }
        }
    }

}