package io.legado.app.ui.book.remote

import android.content.Context
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ItemRemoteBookBinding


class RemoteBookAdapter (context: Context, val callBack: CallBack) :
    RecyclerAdapter<String, ItemRemoteBookBinding>(context){

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
        item: String,
        payloads: MutableList<Any>
    ) {
        binding.run {
           tvName.text = item
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemRemoteBookBinding) {

    }

    interface CallBack {
        fun showRemoteBookInfo(book: Book)
    }
}