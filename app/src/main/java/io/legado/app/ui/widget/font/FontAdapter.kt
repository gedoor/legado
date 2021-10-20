package io.legado.app.ui.widget.font

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.ItemFontBinding
import io.legado.app.utils.*
import timber.log.Timber
import java.io.File
import java.net.URLDecoder

class FontAdapter(context: Context, curFilePath: String, val callBack: CallBack) :
    RecyclerAdapter<DocItem, ItemFontBinding>(context) {

    private val curName = URLDecoder.decode(curFilePath, "utf-8")
        .substringAfterLast(File.separator)

    override fun getViewBinding(parent: ViewGroup): ItemFontBinding {
        return ItemFontBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemFontBinding,
        item: DocItem,
        payloads: MutableList<Any>
    ) {
        binding.run {
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
                Timber.e(it)
                context.toastOnUi("Read ${item.name} Error: ${it.localizedMessage}")
            }
            tvFont.text = item.name
            root.setOnClickListener { callBack.onClick(item) }
            if (item.name == curName) {
                ivChecked.visible()
            } else {
                ivChecked.invisible()
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemFontBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.onClick(it)
            }
        }
    }

    interface CallBack {
        fun onClick(docItem: DocItem)
    }
}