package io.legado.app.ui.widget.font

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.utils.DocItem
import io.legado.app.utils.RealPathUtil
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_font.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.toast
import java.io.File

class FontAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<DocItem>(context, R.layout.item_font) {

    override fun convert(holder: ItemViewHolder, item: DocItem, payloads: MutableList<Any>) {
        with(holder.itemView) {
            try {
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
                tv_font.typeface = typeface
            } catch (e: Exception) {
                e.printStackTrace()
                context.toast("Read ${item.name} Error: ${e.localizedMessage}")
            }
            tv_font.text = item.name
            this.onClick { callBack.onClick(item) }
            if (item.name == callBack.curFilePath.substringAfterLast(File.separator)) {
                iv_checked.visible()
            } else {
                iv_checked.invisible()
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
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