package io.legado.app.ui.book.local

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.constant.AppConst
import io.legado.app.databinding.ItemImportBookBinding
import io.legado.app.utils.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ImportBookAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<DocItem, ItemImportBookBinding>(context) {
    var selectedUris = hashSetOf<String>()
    var checkableCount = 0
    private var bookFileNames = arrayListOf<String>()

    override fun getViewBinding(parent: ViewGroup): ItemImportBookBinding {
        return ItemImportBookBinding.inflate(inflater, parent, false)
    }

    override fun onCurrentListChanged() {
        upCheckableCount()
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemImportBookBinding,
        item: DocItem,
        payloads: MutableList<Any>
    ) {
        with(binding) {
            if (payloads.isEmpty()) {
                if (item.isDir) {
                    ivIcon.setImageResource(R.drawable.ic_folder)
                    ivIcon.visible()
                    cbSelect.invisible()
                    llBrief.gone()
                    cbSelect.isChecked = false
                } else {
                    if (bookFileNames.contains(item.name)) {
                        ivIcon.setImageResource(R.drawable.ic_book_has)
                        ivIcon.visible()
                        cbSelect.invisible()
                    } else {
                        ivIcon.invisible()
                        cbSelect.visible()
                    }
                    llBrief.visible()
                    tvTag.text = item.name.substringAfterLast(".")
                    tvSize.text = StringUtils.toSize(item.size)
                    tvDate.text = AppConst.dateFormat.format(item.date)
                    cbSelect.isChecked = selectedUris.contains(item.uri.toString())
                }
                tvName.text = item.name
            } else {
                cbSelect.isChecked = selectedUris.contains(item.uri.toString())
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemImportBookBinding) {
        holder.itemView.onClick {
            getItem(holder.layoutPosition)?.let {
                if (it.isDir) {
                    callBack.nextDoc(it.uri)
                } else if (!bookFileNames.contains(it.name)) {
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

    fun upBookHas(bookUrls: List<String>) {
        bookFileNames.clear()
        bookUrls.forEach {
            val path = Uri.decode(it)
            bookFileNames.add(FileUtils.getName(path))
        }
        notifyDataSetChanged()
        upCheckableCount()
    }

    private fun upCheckableCount() {
        checkableCount = 0
        getItems().forEach {
            if (!it.isDir && !bookFileNames.contains(it.name)) {
                checkableCount++
            }
        }
        callBack.upCountView()
    }

    fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            getItems().forEach {
                if (!it.isDir && !bookFileNames.contains(it.name)) {
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

    interface CallBack {
        fun nextDoc(uri: Uri)
        fun upCountView()
    }

}