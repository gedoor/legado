package io.legado.app.ui.widget.font

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.ItemFontBinding
import io.legado.app.utils.DocItem
import io.legado.app.utils.RealPathUtil
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.toast
import java.io.File

class FontAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<DocItem, ItemFontBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemFontBinding {
        return ItemFontBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemFontBinding,
        item: DocItem,
        payloads: MutableList<Any>
    ) {
        with(binding) {
            kotlin.runCatching {
                val typeface: Typeface? = if (item.isContentPath) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.contentResolver
                            .openFileDescriptor(item.uri, "r")
                            ?.fileDescriptor?.let {
                                Typeface.Builder(it).build()
                            }
                    } else {
                        Typeface.createFromFile(RealPathUtil.getPath(context, item.uri))
                    }
                } else {
                    Typeface.createFromFile(item.uri.toString())
                }
                tvFont.typeface = typeface
            }.onFailure {
                it.printStackTrace()
                context.toast("Read ${item.name} Error: ${it.localizedMessage}")
            }
            tvFont.text = item.name
            root.onClick { callBack.onClick(item) }
            if (item.name == callBack.curFilePath.substringAfterLast(File.separator)) {
                ivChecked.visible()
            } else {
                ivChecked.invisible()
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemFontBinding) {
        holder.itemView.onClick {
            getItem(holder.layoutPosition)?.let {
                callBack.onClick(it)
            }
        }
    }

    interface CallBack {
        fun onClick(docItem: DocItem)
        val curFilePath: String
    }
}