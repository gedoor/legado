package io.legado.app.ui.book.remote

import android.content.Context
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.ItemRemoteBookBinding
import io.legado.app.utils.ConvertUtils
import java.text.SimpleDateFormat
import java.util.Date


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
            //Todo：需要判断书籍是否已经加入书架，来改变“下载”按钮的文本，暂时还没有比较好的方案
            tvName.text = item.filename.substringBeforeLast(".")
            tvContentType.text = item.contentType
            tvSize.text = ConvertUtils.formatFileSize(item.size)
            tvDate.text = SimpleDateFormat("yyyy-MM-dd").format(Date(item.lastModify))
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemRemoteBookBinding) {
        binding.btnDownload.setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.addToBookshelf(it)
                }
        }


    }

    interface CallBack {
        fun addToBookshelf(remoteBook: RemoteBook)
    }
}