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
    val groupRequestCode = 12
    val selectedBooks: HashSet<Book> = hashSetOf()
    var actionItem: Book? = null

    fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            getItems().forEach {
                selectedBooks.add(it)
            }
        } else {
            selectedBooks.clear()
        }
        notifyDataSetChanged()
        callBack.upSelectCount()
    }

    fun revertSelection() {
        getItems().forEach {
            if (selectedBooks.contains(it)) {
                selectedBooks.remove(it)
            } else {
                selectedBooks.add(it)
            }
        }
        notifyDataSetChanged()
        callBack.upSelectCount()
    }

    override fun convert(holder: ItemViewHolder, item: Book, payloads: MutableList<Any>) {
        with(holder.itemView) {
            tv_name.text = item.name
            tv_author.text = context.getString(R.string.author_show, item.author)
            tv_group.text = getGroupName(item.group)
            checkbox.isChecked = selectedBooks.contains(item)
            checkbox.onClick {
                if (checkbox.isChecked) {
                    selectedBooks.add(item)
                } else {
                    selectedBooks.remove(item)
                }
                callBack.upSelectCount()
            }
            onClick {
                checkbox.isChecked = !checkbox.isChecked
                if (checkbox.isChecked) {
                    selectedBooks.add(item)
                } else {
                    selectedBooks.remove(item)
                }
                callBack.upSelectCount()
            }
            tv_delete.onClick {
                callBack.deleteBook(item)
            }
            tv_group.onClick {
                actionItem = item
                callBack.selectGroup(item.group, groupRequestCode)
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
        fun deleteBook(book: Book)
        fun selectGroup(groupId: Int, requestCode: Int)
    }
}