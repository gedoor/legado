package io.legado.app.ui.document.adapter

import android.content.Context
import android.os.Environment
import android.view.ViewGroup
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.ItemPathFilepickerBinding
import io.legado.app.ui.document.utils.ConvertUtils
import io.legado.app.ui.document.utils.FilePickerIcon

import java.util.*


class PathAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<String, ItemPathFilepickerBinding>(context) {
    private val paths = LinkedList<String>()
    private val arrowIcon = ConvertUtils.toDrawable(FilePickerIcon.getArrow())

    fun getPath(position: Int): String {
        val tmp = StringBuilder("$sdCardDirectory/")
        //忽略根目录
        if (position == 0) {
            return tmp.toString()
        }
        for (i in 1..position) {
            tmp.append(paths[i]).append("/")
        }
        return tmp.toString()
    }

    fun updatePath(path: String) {
        var path1 = path
        path1 = path1.replace(sdCardDirectory, "")
        paths.clear()
        if (path1 != "/" && path1 != "") {
            val subDirs = path1.substring(path1.indexOf("/") + 1)
                .split("/")
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            Collections.addAll(paths, *subDirs)
        }
        paths.addFirst(ROOT_HINT)
        setItems(paths)
    }

    override fun getViewBinding(parent: ViewGroup): ItemPathFilepickerBinding {
        return ItemPathFilepickerBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemPathFilepickerBinding,
        item: String,
        payloads: MutableList<Any>
    ) {
        binding.apply {
            textView.text = item
            imageView.setImageDrawable(arrowIcon)
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemPathFilepickerBinding) {
        holder.itemView.setOnClickListener {
            callBack.onPathClick(holder.layoutPosition)
        }
    }

    interface CallBack {
        fun onPathClick(position: Int)
    }

    companion object {
        private const val ROOT_HINT = "SD"

        @Suppress("DEPRECATION")
        val sdCardDirectory: String = Environment.getExternalStorageDirectory().absolutePath
    }
}
