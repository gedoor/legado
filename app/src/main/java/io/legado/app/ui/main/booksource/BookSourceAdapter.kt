package io.legado.app.ui.main.booksource

import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.PopupMenu
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.BookSource
import io.legado.app.help.ItemTouchCallback
import io.legado.app.help.ItemTouchCallback.OnItemTouchCallbackListener
import kotlinx.android.synthetic.main.item_book_source.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*
import kotlin.collections.HashSet

class BookSourceAdapter : PagedListAdapter<BookSource, BookSourceAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {

        @JvmField
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BookSource>() {
            override fun areItemsTheSame(oldItem: BookSource, newItem: BookSource): Boolean =
                oldItem.origin == newItem.origin

            override fun areContentsTheSame(oldItem: BookSource, newItem: BookSource): Boolean =
                oldItem.origin == newItem.origin
                        && oldItem.name == newItem.name
                        && oldItem.group == newItem.group
                        && oldItem.isEnabled == newItem.isEnabled
        }
    }

    var callBack: CallBack? = null
    val checkedList = HashSet<String>()

    val itemTouchCallbackListener = object : OnItemTouchCallbackListener {
        override fun onSwiped(adapterPosition: Int) {

        }

        override fun onMove(srcPosition: Int, targetPosition: Int): Boolean {
            currentList?.let {
                val srcSource = it[srcPosition]
                val targetSource = it[targetPosition]
                srcSource?.let { a->
                    targetSource?.let { b->
                        a.customOrder = targetPosition
                        b.customOrder = srcPosition
                        callBack?.update(a, b)
                    }
                }
            }
            return true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_book_source, parent, false))
    }


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, checkedList, callBack) }
    }


    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(bookSource: BookSource, checkedList: HashSet<String>, callBack: CallBack?) = with(itemView) {
            cb_book_source.text = String.format("%s (%s)", bookSource.name, bookSource.group)
            cb_book_source.onClick {
                if (cb_book_source.isChecked) {
                    checkedList.add(bookSource.origin)
                } else {
                    checkedList.remove(bookSource.origin)
                }
            }
            sw_enabled.isChecked = bookSource.isEnabled
            sw_enabled.setOnClickListener{
                bookSource.isEnabled = sw_enabled.isChecked
                callBack?.update(bookSource)
            }
            iv_more.setOnClickListener{
                val popupMenu = PopupMenu(context, iv_more)
                popupMenu.menu.add(Menu.NONE, R.id.menu_edit, Menu.NONE, R.string.edit)
                popupMenu.menu.add(Menu.NONE, R.id.menu_del, Menu.NONE, R.string.delete)
                popupMenu.menu.add(Menu.NONE, R.id.menu_top, Menu.NONE, R.string.to_top)
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.menu_edit ->{
                            callBack?.edit(bookSource)
                            true}
                        R.id.menu_del ->{
                            callBack?.del(bookSource)
                            true}
                        R.id.menu_top ->{ true}
                        else -> {false}
                    }
                }
                popupMenu.show()
            }
        }
    }

    interface CallBack {
        fun del(bookSource: BookSource)
        fun edit(bookSource: BookSource)
        fun update(bookSource: BookSource)
        fun update(vararg bookSource: BookSource)
    }
}