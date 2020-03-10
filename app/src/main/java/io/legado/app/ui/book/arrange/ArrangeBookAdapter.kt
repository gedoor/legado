package io.legado.app.ui.book.arrange

import android.content.Context
import android.view.View
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_arrange_book.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ArrangeBookAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<Book>(context, R.layout.item_arrange_book) {
    val groupRequestCode = 12
    private val selectedBooks: HashSet<Book> = hashSetOf()
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

    fun selectedBooks(): Array<Book> {
        val books = arrayListOf<Book>()
        selectedBooks.forEach {
            if (getItems().contains(it)) {
                books.add(it)
            }
        }
        return books.toTypedArray()
    }

    override fun convert(holder: ItemViewHolder, item: Book, payloads: MutableList<Any>) {
        with(holder.itemView) {
            tv_name.text = item.name
//            tv_name.text = if (item.author.isEmpty()) {
//                item.name
//            } else {
//                "${item.name}(${item.author})"
//            }
            tv_author.text = item.author // resources.getString(R.string.author_show, item.author)
            tv_author.visibility = if (item.author.isEmpty()) View.GONE else View.VISIBLE
            tv_group_s.text = getGroupName(item.group)
            checkbox.isChecked = selectedBooks.contains(item)
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.apply {
            checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                getItem(holder.layoutPosition)?.let {
                    if (buttonView.isPressed) {
                        if (isChecked) {
                            selectedBooks.add(it)
                        } else {
                            selectedBooks.remove(it)
                        }
                        callBack.upSelectCount()
                    }

                }
            }
            onClick {
                getItem(holder.layoutPosition)?.let {
                    checkbox.isChecked = !checkbox.isChecked
                    if (checkbox.isChecked) {
                        selectedBooks.add(it)
                    } else {
                        selectedBooks.remove(it)
                    }
                    callBack.upSelectCount()
                }
            }
            tv_delete.onClick {
                getItem(holder.layoutPosition)?.let {
                    callBack.deleteBook(it)
                }
            }
            tv_group.onClick {
                getItem(holder.layoutPosition)?.let {
                    actionItem = it
                    callBack.selectGroup(it.group, groupRequestCode)
                }
            }
        }
    }

    private fun getGroupList(groupId: Int): List<String> {
        val groupNames = arrayListOf<String>()
        callBack.groupList.forEach {
            if (it.groupId and groupId > 0) {
                groupNames.add(it.groupName)
            }
        }
        return groupNames;
    }

    private fun getGroupName(groupId: Int): String {
        val groupNames = getGroupList(groupId)
        if (groupNames.isEmpty()) {
            return ""
        }
        return groupNames.joinToString(",")
    }

    interface CallBack {
        val groupList: List<BookGroup>
        fun upSelectCount()
        fun deleteBook(book: Book)
        fun selectGroup(groupId: Int, requestCode: Int)
    }
}