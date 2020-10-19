package io.legado.app.ui.book.read.config

import android.content.Context
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.constant.EventBus
import io.legado.app.help.ImageLoader
import io.legado.app.help.ReadBookConfig
import io.legado.app.utils.postEvent
import kotlinx.android.synthetic.main.item_bg_image.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.io.File

class BgAdapter(context: Context, val textColor: Int) :
    SimpleRecyclerAdapter<String>(context, R.layout.item_bg_image) {

    override fun convert(holder: ItemViewHolder, item: String, payloads: MutableList<Any>) {
        with(holder.itemView) {
            ImageLoader.load(
                context,
                context.assets.open("bg${File.separator}$item").readBytes()
            )
                .centerCrop()
                .into(iv_bg)
            tv_name.setTextColor(textColor)
            tv_name.text = item.substringBeforeLast(".")
        }
    }

    override fun registerListener(holder: ItemViewHolder) {
        holder.itemView.apply {
            this.onClick {
                getItemByLayoutPosition(holder.layoutPosition)?.let {
                    ReadBookConfig.durConfig.setCurBg(1, it)
                    ReadBookConfig.upBg()
                    postEvent(EventBus.UP_CONFIG, false)
                }
            }
        }
    }
}