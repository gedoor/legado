package io.legado.app.ui.book.local

import android.content.Context
import android.net.Uri
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.constant.AppConst
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.item_import_book.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ImportBookAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<DocItem>(context, R.layout.item_import_book) {
    var selectedUris = hashSetOf<String>()
    var checkableCount = 0
    private var bookFileNames = arrayListOf<String>()

    fun upBookHas(bookUrls: List<String>) {
        bookFileNames.clear()
        bookUrls.forEach {
            val path = Uri.decode(it)
            bookFileNames.add(FileUtils.getName(path))
        }
        notifyDataSetChanged()
        upCheckableCount()
    }

    fun setData(data: List<DocItem>) {
        setItems(data)
        upCheckableCount()
    }

    private fun upCheckableCount() {
        checkableCount = 0
        getItems().forEach {
            if (!it.isDir && !bookFileNames.contains(it.uri.toString())) {
                checkableCount++
            }
        }
        callBack.upCountView()
    }

    fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            getItems().forEach {
                if (!it.isDir && !bookFileNames.contains(it.uri.toString())) {
                    selectedUris.add(it.uri.toString())
                }
            }
        } else {
            selectedUris.clear()
        }
        notifyDataSetChanged()
        callBack.upCountView()
    }

    fun revertSelection() {
        getItems().forEach {
            if (!it.isDir) {
                if (selectedUris.contains(it.uri.toString())) {
                    selectedUris.remove(it.uri.toString())
                } else {
                    selectedUris.add(it.uri.toString())
                }
            }
        }
        callBack.upCountView()
    }

    fun removeSelection() {
        for (i in getItems().lastIndex downTo 0) {
            if (getItem(i)?.uri.toString() in selectedUris) {
                removeItem(i)
            }
        }
    }

    override fun convert(holder: ItemViewHolder, item: DocItem, payloads: MutableList<Any>) {
        holder.itemView.apply {
            if (payloads.isEmpty()) {
                if (item.isDir) {
                    iv_icon.setImageResource(R.drawable.ic_folder)
                    iv_icon.visible()
                    cb_select.invisible()
                    ll_brief.gone()
                    cb_select.isChecked = false
                } else {
                    if (bookFileNames.contains(item.name)) {
                        iv_icon.setImageResource(R.drawable.ic_book_has)
                        iv_icon.visible()
                        cb_select.invisible()
                    } else {
                        iv_icon.invisible()
                        cb_select.visible()
                    }
                    ll_brief.visible()
                    tv_tag.text = item.name.substringAfterLast(".")
                    tv_size.text = StringUtils.toSize(item.size)
                    tv_date.text = AppConst.dateFormat.format(item.date)
                    cb_select.isChecked = selectedUris.contains(item.uri.toString())
                }
                tv_name.text = item.name
            } else {
                cb_select.isChecked = selectedUris.contains(item.uri.toString())
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.onClick {
            getItem(holder.layoutPosition)?.let {
                if (it.isDir) {
                    callBack.nextDoc(it.uri)
                } else if (!bookFileNames.contains(it.uri.toString())) {
                    if (!selectedUris.contains(it.uri.toString())) {
                        selectedUris.add(it.uri.toString())
                    } else {
                        selectedUris.remove(it.uri.toString())
                    }
                    notifyItemChanged(holder.layoutPosition, true)
                    callBack.upCountView()
                }
            }
        }
    }

    interface CallBack {
        fun nextDoc(uri: Uri)
        fun upCountView()
    }

}