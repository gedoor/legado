package io.legado.app.ui.widget.font

import android.content.Context
import android.graphics.Typeface
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_font.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.toast
import java.io.File

class FontAdapter(context: Context, val callBack: CallBack) :
    SimpleRecyclerAdapter<File>(context, R.layout.item_font) {

    override fun convert(holder: ItemViewHolder, item: File, payloads: MutableList<Any>) {
        with(holder.itemView) {
            try {
                val typeface = Typeface.createFromFile(item)
                tv_font.typeface = typeface
            } catch (e: Exception) {
                context.toast("读取${item.name}字体失败")
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
        fun onClick(file: File)
        val curFilePath: String
    }
}