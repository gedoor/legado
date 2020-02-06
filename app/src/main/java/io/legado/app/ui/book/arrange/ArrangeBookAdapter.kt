package io.legado.app.ui.book.arrange

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import kotlinx.android.synthetic.main.item_arrange_book.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ArrangeBookAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<Book>(context, R.layout.item_arrange_book) {

    val selectedBooks: HashSet<String> = hashSetOf()

    fun isSelectAll(): Boolean {
        return if (selectedBooks.isEmpty()) {
            false
        } else {
            selectedBooks.size >= itemCount
        }
    }

    fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            getItems().forEach {
                selectedBooks.add(it.bookUrl)
            }
            notifyDataSetChanged()
            callBack.upSelectCount()
        } else {
            selectedBooks.clear()
            notifyDataSetChanged()
            callBack.upSelectCount()
        }
    }

    override fun convert(holder: ItemViewHolder, item: Book, payloads: MutableList<Any>) {
        with(holder.itemView) {
            tv_name.text = item.name
            tv_author.text = context.getString(R.string.author_show, item.author)
            tv_group.text = getGroupName(item.group)
            checkbox.isChecked = selectedBooks.contains(item.bookUrl)
            checkbox.onClick {
                if (checkbox.isChecked) {
                    selectedBooks.add(item.bookUrl)
                } else {
                    selectedBooks.remove(item.bookUrl)
                }
                callBack.upSelectCount()
            }
            onClick {
                checkbox.isChecked = !checkbox.isChecked
                if (checkbox.isChecked) {
                    selectedBooks.add(item.bookUrl)
                } else {
                    selectedBooks.remove(item.bookUrl)
                }
                callBack.upSelectCount()
            }
            tv_delete.onClick {
                callBack.deleteBook(item.bookUrl)
            }
            tv_group.onClick {
                callBack.selectGroup()
            }
        }
    }

    private fun getGroupName(groupId: Int): String {
        callBack.groupList.forEach {
            if (it.groupId == groupId) {
                return it.groupName
            }
        }
        return context.getString(R.string.group)
    }

    interface CallBack {
        val groupList: List<BookGroup>
        fun upSelectCount()
        fun deleteBook(bookUrl: String)
        fun selectGroup()
    }
}