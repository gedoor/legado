package io.legado.app.ui.book.remote

import android.content.Context
import android.view.ViewGroup
import cn.hutool.core.date.LocalDateTimeUtil
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemRemoteBookBinding
import io.legado.app.utils.ConvertUtils
import io.legado.app.utils.FileUtils
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx


/**
 * 适配器
 * @author qianfanguojin
 */
class RemoteBookAdapter (context: Context, val callBack: CallBack) :
    RecyclerAdapter<RemoteBook, ItemRemoteBookBinding>(context){

    override fun getViewBinding(parent: ViewGroup): ItemRemoteBookBinding {
        return ItemRemoteBookBinding.inflate(inflater, parent, false)
    }

    override fun onCurrentListChanged() {

    }

    /**
     * 绑定RecycleView 中每一个项的视图和数据
     */
    override fun convert(
        holder: ItemViewHolder,
        binding: ItemRemoteBookBinding,
        item: RemoteBook,
        payloads: MutableList<Any>
    ) {
        binding.run {
            tvName.text = item.filename.substringBeforeLast(".")
            tvContentType.text = item.contentType
            tvSize.text = ConvertUtils.formatFileSize(item.size)
            tvDate.text = LocalDateTimeUtil.format(LocalDateTimeUtil.of(item.lastModify), "yyyy-MM-dd")
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemRemoteBookBinding) {
        binding.btnDownload.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    context.toastOnUi("开始加入")
                    callBack.addToBookshelf(it)
                    context.toastOnUi("加入成功")
                }
        }


    }

    interface CallBack {
        fun addToBookshelf(remoteBook: RemoteBook)
    }
}